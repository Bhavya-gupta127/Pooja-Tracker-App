package com.example.poojatracker;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class PoojaListActivity extends AppCompatActivity implements SelectListener {
    ArrayList<PoojaModel> arrPooja = new ArrayList<>();
    ActivityResultLauncher<PoojaModel> launcher;
    PoojaModel currentPooja;
    MyDBHelper dbHelper;
    RecyclerView recyclerView;
    String day;
    int scrollTo = 0;

    private TextView detailsBadge;
    private TextView progressLabel;
    private ProgressBar dayProgressBar;
    private View emptyStateCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_color));
        setContentView(R.layout.activity_pooja_list);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        dbHelper = new MyDBHelper(this);
        dbHelper.ensureDailyReset(this);

        Intent intent = getIntent();
        day = intent != null && intent.getStringExtra("day") != null
                ? intent.getStringExtra("day")
                : "Monday";

        Button btnBackList = findViewById(R.id.btnBackList);
        Button btnAddFromList = findViewById(R.id.btnAddFromList);
        Button btnEmptyAdd = findViewById(R.id.btnEmptyAdd);
        TextView dayName = findViewById(R.id.day);
        detailsBadge = findViewById(R.id.details);
        progressLabel = findViewById(R.id.progressLabel);
        dayProgressBar = findViewById(R.id.dayProgressBar);
        emptyStateCard = findViewById(R.id.emptyStateCard);
        TextView emptyStateTitle = findViewById(R.id.emptyStateTitle);

        dayName.setText(day);
        emptyStateTitle.setText("No Poojas scheduled for " + day);

        btnBackList.setOnClickListener(v -> finish());
        View.OnClickListener openAddForDay = v -> {
            Intent addIntent = new Intent(PoojaListActivity.this, PoojaAddActivity.class);
            addIntent.putExtra("preselectDay", day);
            startActivity(addIntent);
        };
        btnAddFromList.setOnClickListener(openAddForDay);
        btnEmptyAdd.setOnClickListener(openAddForDay);

        recyclerView = findViewById(R.id.poojalist);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.spacing_between_items);
        recyclerView.addItemDecoration(new SpaceItemDecoration(spacingInPixels));

        updatelist();

        launcher = registerForActivityResult(new Contract(), result -> {
            if ("true".equals(result) && currentPooja != null) {
                dbHelper.updateDbForStatus(currentPooja.getId(), true);
                currentPooja.setStatus(true);
                updatelist();
                scrollTo = dbHelper.getFirstUnchecked(day);
                if (scrollTo >= 0 && scrollTo < arrPooja.size()) {
                    recyclerView.smoothScrollToPosition(scrollTo);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatelist();
    }

    private void updatelist() {
        arrPooja = dbHelper.fetchPooja(day);
        recyclerView.setAdapter(new RecyclerPoojaAdapter(this, arrPooja, this));
        refreshProgressHeader();
    }

    private void refreshProgressHeader() {
        int[] counts = dbHelper.getDayCounts(day);
        int checked = counts[0];
        int total = counts[1];

        if (total == 0) {
            emptyStateCard.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            detailsBadge.setText("0 / 0");
            detailsBadge.setBackgroundResource(R.drawable.badge_pill_bg);
            detailsBadge.setTextColor(ContextCompat.getColor(this, R.color.badge_pill_text));
            progressLabel.setText("No Poojas scheduled yet");
            dayProgressBar.setProgress(0);
        } else {
            emptyStateCard.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            int pct = Math.round((checked * 100f) / total);
            dayProgressBar.setProgress(pct);
            if (checked >= total) {
                detailsBadge.setText("✓ " + checked + " / " + total);
                detailsBadge.setBackgroundResource(R.drawable.badge_done_bg);
                detailsBadge.setTextColor(ContextCompat.getColor(this, R.color.badge_done_text));
                progressLabel.setText("All Poojas completed for " + day + "!");
            } else {
                detailsBadge.setText(checked + " / " + total);
                detailsBadge.setBackgroundResource(R.drawable.badge_pill_bg);
                detailsBadge.setTextColor(ContextCompat.getColor(this, R.color.badge_pill_text));
                progressLabel.setText((total - checked) + " remaining for " + day);
            }
        }
    }

    @Override
    public void onItemClicked(PoojaModel poojaModel) {
        currentPooja = poojaModel;
        launcher.launch(poojaModel);
    }

    @Override
    public void onDeleteClicked(PoojaModel poojaModel) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Pooja?")
                .setMessage("Remove \"" + poojaModel.getTitle().trim() + "\" from " + day + "'s schedule?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    try {
                        dbHelper.deleteFromDb(poojaModel.getId(), day);
                        updatelist();
                        Toast.makeText(PoojaListActivity.this, "Removed from " + day, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(PoojaListActivity.this, "Error while removing Pooja", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .setIcon(R.drawable.omicon)
                .show();
    }

    @Override
    public void onCheckboxClicked(PoojaModel poojaModel) {
        boolean nextStatus = !poojaModel.isStatus();
        dbHelper.updateDbForStatus(poojaModel.getId(), nextStatus);
        poojaModel.setStatus(nextStatus);
        updatelist();
    }
}