package com.example.poojatracker;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

public class PoojaAddActivity extends AppCompatActivity {

    private static final String PREFS_MIGRATION = "screen_media_migration";
    private static final String KEY_LAST_MEDIA_COUNT = "last_media_count";

    private MyDBHelper dbHelper;
    private final String[] dayMapping = MyDBHelper.DAYS_ORDER;
    private final ScreenMediaPlaylistImporter mediaImporter = new ScreenMediaPlaylistImporter();

    private TextView legacyRecoveryStatus;
    private Button btnMigrateOldApp;

    private final ActivityResultLauncher<String> legacyStoragePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    exportScheduleToDownloads();
                } else {
                    Toast.makeText(
                            PoojaAddActivity.this,
                            "Storage permission is required on Android 9 and below to save to Downloads",
                            Toast.LENGTH_LONG
                    ).show();
                }
            });

    private final ActivityResultLauncher<String[]> importLocalJsonLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    importJsonFromUri(uri);
                }
            });

    private final ActivityResultLauncher<String[]> importScreenMediaLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    startScreenMediaScan(uris);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_color));
        setContentView(R.layout.activity_pooja_add);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        dbHelper = new MyDBHelper(this);

        Button btnBackAdd = findViewById(R.id.btnBackAdd);
        btnBackAdd.setOnClickListener(view -> finish());

        legacyRecoveryStatus = findViewById(R.id.legacyRecoveryStatus);
        btnMigrateOldApp = findViewById(R.id.btnMigrateOldApp);
        Button btnLoadStarterPack = findViewById(R.id.btnLoadStarterPack);

        btnMigrateOldApp.setOnClickListener(view -> showScreenMediaPickerGuide());
        btnLoadStarterPack.setOnClickListener(view -> {
            try {
                JSONArray bundled = ScreenMediaPlaylistImporter.loadBundledJsonArray(this);
                int imported = importPoojaJsonArray(bundled);
                Toast.makeText(
                        this,
                        "Loaded " + imported + " Poojas into your weekly schedule!",
                        Toast.LENGTH_LONG
                ).show();
                refreshRecoveryBanner();
            } catch (Exception e) {
                Log.e("PoojaAdd", "Failed to load bundled Pooja pack", e);
                Toast.makeText(this, "Unable to load bundled pack", Toast.LENGTH_SHORT).show();
            }
        });

        // JSON Import / Export controls
        Button submitJson = findViewById(R.id.submitJSON);
        Button importFileJson = findViewById(R.id.importFileJSON);
        Button exportJson = findViewById(R.id.exportJSON);
        EditText jsonUrl = findViewById(R.id.addJsonUrl);

        submitJson.setOnClickListener(view -> {
            String url = String.valueOf(jsonUrl.getText()).trim();
            if (url.isEmpty() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
                Toast.makeText(PoojaAddActivity.this, "Please enter a valid http/https JSON URL", Toast.LENGTH_SHORT).show();
                jsonUrl.requestFocus();
                return;
            }
            submitJson.setEnabled(false);
            JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                    Request.Method.GET,
                    url,
                    null,
                    response -> {
                        submitJson.setEnabled(true);
                        int imported = importPoojaJsonArray(response);
                        if (imported > 0) {
                            Toast.makeText(
                                    PoojaAddActivity.this,
                                    "Imported " + imported + " Poojas successfully!",
                                    Toast.LENGTH_SHORT
                            ).show();
                            refreshRecoveryBanner();
                        } else {
                            Toast.makeText(
                                    PoojaAddActivity.this,
                                    "No valid Pooja entries found in JSON",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    },
                    error -> {
                        submitJson.setEnabled(true);
                        Toast.makeText(
                                PoojaAddActivity.this,
                                "Unable to fetch JSON. Please check the URL and internet connection.",
                                Toast.LENGTH_SHORT
                        ).show();
                        Log.e("TestJson", "Volley JSON import error", error);
                    }
            );
            requestQueue.add(jsonArrayRequest);
        });

        importFileJson.setOnClickListener(view ->
                importLocalJsonLauncher.launch(new String[]{"application/json", "text/*", "*/*"})
        );

        exportJson.setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                legacyStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                return;
            }
            exportScheduleToDownloads();
        });

        // Custom Pooja Add controls
        EditText addTitle = findViewById(R.id.addTitle);
        EditText addDescription = findViewById(R.id.addDescription);
        EditText addContentUrl = findViewById(R.id.addContentUrl);
        TextView selectAllDays = findViewById(R.id.selectAllDays);

        CheckBox checkMonday = findViewById(R.id.checkmonday);
        CheckBox checkTuesday = findViewById(R.id.checktuesday);
        CheckBox checkWednesday = findViewById(R.id.checkWednesday);
        CheckBox checkThursday = findViewById(R.id.checkThursday);
        CheckBox checkFriday = findViewById(R.id.checkFriday);
        CheckBox checkSaturday = findViewById(R.id.checkSaturday);
        CheckBox checkSunday = findViewById(R.id.checkSunday);

        CheckBox[] dayCheckBoxes = new CheckBox[]{
                checkMonday, checkTuesday, checkWednesday, checkThursday,
                checkFriday, checkSaturday, checkSunday
        };

        Runnable syncSelectAllLabel = () -> {
            boolean allChecked = true;
            for (CheckBox cb : dayCheckBoxes) {
                if (!cb.isChecked()) {
                    allChecked = false;
                    break;
                }
            }
            selectAllDays.setText(allChecked ? "Clear All" : "Select All");
        };

        CompoundButton.OnCheckedChangeListener dayChangeListener =
                (buttonView, isChecked) -> syncSelectAllLabel.run();
        for (CheckBox cb : dayCheckBoxes) {
            cb.setOnCheckedChangeListener(dayChangeListener);
        }

        String preselectDay = getIntent() != null ? getIntent().getStringExtra("preselectDay") : null;
        if (preselectDay != null) {
            for (int i = 0; i < dayMapping.length; i++) {
                if (dayMapping[i].equalsIgnoreCase(preselectDay.trim())) {
                    dayCheckBoxes[i].setChecked(true);
                    break;
                }
            }
            syncSelectAllLabel.run();
        }

        selectAllDays.setOnClickListener(view -> {
            boolean shouldSelectAll = !"Clear All".contentEquals(selectAllDays.getText());
            for (CheckBox cb : dayCheckBoxes) {
                cb.setChecked(shouldSelectAll);
            }
            syncSelectAllLabel.run();
        });

        Button submitData = findViewById(R.id.submitData);
        submitData.setOnClickListener(view -> {
            String title = String.valueOf(addTitle.getText()).trim();
            String description = String.valueOf(addDescription.getText()).trim();
            String contentUrl = String.valueOf(addContentUrl.getText()).trim();

            if (title.isEmpty()) {
                Toast.makeText(PoojaAddActivity.this, "Please enter a Pooja title", Toast.LENGTH_SHORT).show();
                addTitle.requestFocus();
                return;
            }
            if (contentUrl.isEmpty() || !PoojaModel.hasValidVideoId(contentUrl)) {
                Toast.makeText(
                        PoojaAddActivity.this,
                        "Please enter a valid YouTube link or 11-char Video ID",
                        Toast.LENGTH_SHORT
                ).show();
                addContentUrl.requestFocus();
                return;
            }

            boolean[] selectedDays = new boolean[dayCheckBoxes.length];
            boolean atLeastOneDay = false;
            for (int i = 0; i < dayCheckBoxes.length; i++) {
                selectedDays[i] = dayCheckBoxes[i].isChecked();
                if (selectedDays[i]) {
                    atLeastOneDay = true;
                }
            }

            if (!atLeastOneDay) {
                Toast.makeText(PoojaAddActivity.this, "Please select at least one day", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                Random r = new Random();
                int id = r.nextInt(999999 - 100000) + 100000;
                dbHelper.addPooja(id, title, description, contentUrl, false);
                for (int j = 0; j < selectedDays.length; j++) {
                    if (selectedDays[j]) {
                        dbHelper.addDay(id, dayMapping[j]);
                    }
                }

                Toast.makeText(PoojaAddActivity.this, "Pooja added successfully!", Toast.LENGTH_SHORT).show();
                addTitle.setText("");
                addDescription.setText("");
                addContentUrl.setText("");
                for (CheckBox cb : dayCheckBoxes) {
                    cb.setChecked(false);
                }
                syncSelectAllLabel.run();
            } catch (Exception e) {
                Log.e("PoojaAdd", "Failed to add Pooja", e);
                Toast.makeText(PoojaAddActivity.this, "Unexpected error while saving Pooja", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshRecoveryBanner();
    }

    private void refreshRecoveryBanner() {
        SharedPreferences prefs = getSharedPreferences(PREFS_MIGRATION, MODE_PRIVATE);
        int lastMediaCount = prefs.getInt(KEY_LAST_MEDIA_COUNT, 0);
        if (lastMediaCount > 0) {
            legacyRecoveryStatus.setText("✓ Recovered " + lastMediaCount + " Poojas from your screen recording / screenshots! You can add more screenshots anytime.");
        }
    }

    private void showScreenMediaPickerGuide() {
        String howItWorks = "Since the old 2023 app's Export button crashes, you can recover your exact playlist with zero permissions:\n\n"
                + "1. Start your phone's built-in Screen Recorder (from Quick Settings), open your old 'Pooja Scheduler' app, and scroll through Monday → Sunday (~25s video) — OR take screenshots.\n\n"
                + "2. Tap 'Select Video / Screenshots' below and pick that .mp4 video (or multiple screenshots). Every Pooja, Hindi description, YouTube link, and weekday will be extracted offline!";
        new AlertDialog.Builder(this)
                .setTitle("Import from Screen Video / Screenshots")
                .setMessage(howItWorks)
                .setPositiveButton("Select Video / Screenshots", (d, w) ->
                        importScreenMediaLauncher.launch(new String[]{"video/*", "image/*"})
                )
                .setNegativeButton("Cancel", null)
                .setIcon(R.drawable.omicon)
                .show();
    }

    private void startScreenMediaScan(List<Uri> uris) {
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("Scanning Playlist...")
                .setMessage("Reading frames on-device (0 found so far)...")
                .setCancelable(false)
                .setIcon(R.drawable.omicon)
                .create();
        progressDialog.show();

        btnMigrateOldApp.setEnabled(false);
        mediaImporter.importFromMediaUris(this, uris, dbHelper, new ScreenMediaPlaylistImporter.Callback() {
            @Override
            public void onProgress(int currentStep, int totalSteps, int recoveredPoojas) {
                if (isFinishing() || isDestroyed()) return;
                progressDialog.setMessage(
                        "Scanning frame " + currentStep + " / " + totalSteps
                                + " • Found " + recoveredPoojas + " Poojas..."
                );
            }

            @Override
            public void onComplete(int recoveredPoojas, int dayLinksAdded) {
                if (isFinishing() || isDestroyed()) return;
                progressDialog.dismiss();
                btnMigrateOldApp.setEnabled(true);

                if (recoveredPoojas > 0) {
                    getSharedPreferences(PREFS_MIGRATION, MODE_PRIVATE)
                            .edit()
                            .putInt(KEY_LAST_MEDIA_COUNT, recoveredPoojas)
                            .apply();
                    refreshRecoveryBanner();

                    new AlertDialog.Builder(PoojaAddActivity.this)
                            .setTitle("Playlist Recovered!")
                            .setMessage("Imported " + recoveredPoojas + " unique Poojas (" + dayLinksAdded + " weekday entries) from your recording/screenshots!\n\nTip: Tap 'Export Schedule to Downloads' below to save a clean JSON backup.")
                            .setPositiveButton("View My Schedule", (d, w) -> finish())
                            .setNeutralButton("Stay Here", null)
                            .setIcon(R.drawable.omicon)
                            .show();
                } else {
                    Toast.makeText(
                            PoojaAddActivity.this,
                            "No Pooja cards with YouTube links detected. Make sure the recording/screenshot shows a weekday list (Monday–Sunday) in the old app.",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (isFinishing() || isDestroyed()) return;
                progressDialog.dismiss();
                btnMigrateOldApp.setEnabled(true);
                Toast.makeText(
                        PoojaAddActivity.this,
                        "Scan failed: " + errorMessage,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private int importPoojaJsonArray(JSONArray response) {
        if (response == null) {
            return 0;
        }
        int importedCount = 0;
        Random random = new Random();
        java.util.Set<Integer> seenIdsInBatch = new java.util.HashSet<>();
        for (int i = 0; i < response.length(); i++) {
            try {
                JSONObject object = response.getJSONObject(i);
                int id = object.optInt("id", 100000 + random.nextInt(900000));
                String title = object.optString("title", "").trim();
                String desc = object.optString("desc", object.optString("description", "")).trim();
                String contentUrl = object.optString("contentURL", object.optString("contentUrl", "")).trim();
                boolean status = object.optBoolean("status", false);
                JSONArray days = object.optJSONArray("days");

                if (title.isEmpty() || contentUrl.isEmpty()) {
                    continue;
                }

                if (seenIdsInBatch.add(id)) {
                    dbHelper.addPooja(id, title, desc, contentUrl, status);
                    dbHelper.clearDaysForPooja(id);
                    importedCount++;
                }
                if (days != null) {
                    int limit = Math.min(days.length(), dayMapping.length);
                    for (int j = 0; j < limit; j++) {
                        if (days.optInt(j, 0) == 1) {
                            dbHelper.addDay(id, dayMapping[j]);
                        }
                    }
                }
            } catch (JSONException e) {
                Log.w("PoojaImport", "Skipping malformed JSON entry at index " + i, e);
            }
        }
        return importedCount;
    }

    private void importJsonFromUri(Uri uri) {
        File tempFile = new File(getCacheDir(), "pooja_import_tmp_" + System.currentTimeMillis());
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                Toast.makeText(this, "Unable to read selected file", Toast.LENGTH_SHORT).show();
                return;
            }
            try (OutputStream out = new FileOutputStream(tempFile)) {
                byte[] buf = new byte[8192];
                int read;
                while ((read = inputStream.read(buf)) != -1) {
                    out.write(buf, 0, read);
                }
            }

            byte[] header = new byte[15];
            int headerLen;
            try (java.io.FileInputStream fis = new java.io.FileInputStream(tempFile)) {
                headerLen = fis.read(header);
            }
            String magic = headerLen > 0 ? new String(header, 0, headerLen, StandardCharsets.US_ASCII) : "";
            int imported;
            if (magic.startsWith("SQLite format 3")) {
                imported = dbHelper.importFromSqliteFile(tempFile);
            } else {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new java.io.FileInputStream(tempFile), StandardCharsets.UTF_8)
                )) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append('\n');
                    }
                }
                JSONArray jsonArray = new JSONArray(sb.toString());
                imported = importPoojaJsonArray(jsonArray);
            }

            if (imported > 0) {
                Toast.makeText(this, "Imported " + imported + " Poojas successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No valid Pooja entries found in file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("PoojaImport", "Failed to import file", e);
            Toast.makeText(this, "Invalid file. Select an exported Pooja JSON or PoojaDB file.", Toast.LENGTH_LONG).show();
        } finally {
            if (tempFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
            }
        }
    }

    private void exportScheduleToDownloads() {
        String jsonString = dbHelper.exportToJson();
        if (jsonString == null || jsonString.trim().equals("[]")) {
            Toast.makeText(this, "No Poojas to export yet. Add a Pooja first!", Toast.LENGTH_SHORT).show();
            return;
        }

        String filename = "pooja_schedule_" + System.currentTimeMillis() + ".json";
        byte[] payload = jsonString.getBytes(StandardCharsets.UTF_8);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, filename);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/json");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                values.put(MediaStore.Downloads.IS_PENDING, 1);

                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) {
                    throw new IOException("MediaStore returned null URI");
                }
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    if (out == null) {
                        throw new IOException("Unable to open Downloads output stream");
                    }
                    out.write(payload);
                    out.flush();
                }
                ContentValues completeValues = new ContentValues();
                completeValues.put(MediaStore.Downloads.IS_PENDING, 0);
                getContentResolver().update(uri, completeValues, null, null);
            } else {
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (dir != null && !dir.exists() && !dir.mkdirs()) {
                    Log.w("PoojaExport", "Downloads directory mkdirs returned false");
                }
                File file = new File(dir, filename);
                try (OutputStream out = new FileOutputStream(file)) {
                    out.write(payload);
                    out.flush();
                }
            }
            Toast.makeText(this, "Exported to Downloads/" + filename, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e("PoojaExport", "Export failed", e);
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}