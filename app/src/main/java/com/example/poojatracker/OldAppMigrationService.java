package com.example.poojatracker;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Built-in migration helper that reads Pooja schedules directly from the legacy
 * 2023 "Pooja Scheduler" app (com.example.poojatracker) when its Export button crashes.
 * Scoped strictly to com.example.poojatracker and disables itself once migration finishes.
 */
public class OldAppMigrationService extends AccessibilityService {

    private static final String TAG = "OldAppMigration";
    public static final String OLD_PACKAGE = "com.example.poojatracker";
    public static final String PREFS_NAME = "old_app_migration";
    public static final String KEY_AUTO_ACTIVE = "auto_active";
    public static final String KEY_LAST_COUNT = "last_recovered_count";
    public static final String KEY_LAST_TIME = "last_recovered_time";

    private static final String[][] DAY_BUTTON_IDS = new String[][]{
            {"com.example.poojatracker:id/monday"},
            {"com.example.poojatracker:id/tuesday"},
            {"com.example.poojatracker:id/wednesday"},
            {"com.example.poojatracker:id/thurday", "com.example.poojatracker:id/thursday"},
            {"com.example.poojatracker:id/friday"},
            {"com.example.poojatracker:id/saturday"},
            {"com.example.poojatracker:id/sunday"}
    };

    private static OldAppMigrationService activeInstance;

    private MyDBHelper dbHelper;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Set<Integer> completedDays = new HashSet<>();
    private final Set<Integer> sessionUniqueIds = new HashSet<>();
    private final Map<String, Set<Integer>> capturedPerDay = new HashMap<>();

    private String currentTargetDay = null;
    private int stagnantScrolls = 0;
    private int listOpenTicks = 0;
    private int nonTargetTicks = 0;
    private boolean tickerScheduled = false;

    private final Runnable crawlerTicker = new Runnable() {
        @Override
        public void run() {
            tickerScheduled = false;
            if (!isAutoActive(OldAppMigrationService.this)) {
                return;
            }
            try {
                stepAutoCrawler();
            } catch (Exception e) {
                Log.e(TAG, "Auto crawler step error", e);
            }
            if (isAutoActive(OldAppMigrationService.this)) {
                scheduleNextTick(700);
            }
        }
    };

    public static boolean isServiceEnabled(Context context) {
        if (activeInstance != null) {
            return true;
        }
        String enabledServices = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        if (TextUtils.isEmpty(enabledServices)) {
            return false;
        }
        android.content.ComponentName cn = new android.content.ComponentName(context, OldAppMigrationService.class);
        String flatFull = cn.flattenToString();
        String flatShort = cn.flattenToShortString();
        String simpleShort = context.getPackageName() + "/." + OldAppMigrationService.class.getSimpleName();
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabledServices);
        while (splitter.hasNext()) {
            String component = splitter.next();
            if (component.equalsIgnoreCase(flatFull)
                    || component.equalsIgnoreCase(flatShort)
                    || component.equalsIgnoreCase(simpleShort)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAutoActive(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_AUTO_ACTIVE, false);
    }

    public static void startAutoMigration(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_AUTO_ACTIVE, true).apply();
        if (activeInstance != null) {
            activeInstance.resetCrawlerSession();
            activeInstance.scheduleNextTick(500);
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        activeInstance = this;
        dbHelper = new MyDBHelper(this);
        if (isAutoActive(this)) {
            resetCrawlerSession();
            scheduleNextTick(500);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (dbHelper == null) {
            dbHelper = new MyDBHelper(this);
        }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null
                || !OLD_PACKAGE.contentEquals(root.getPackageName())) {
            return;
        }

        // Passive safety net: capture any visible Pooja list rows even if the user scrolls manually
        String detectedDay = detectDayFromListHeader(root);
        if (detectedDay != null) {
            int newlyAdded = scrapeFromListRoot(root, detectedDay);
            if (!isAutoActive(this) && newlyAdded > 0) {
                Toast.makeText(
                        this,
                        "Imported " + newlyAdded + " Pooja(s) from " + detectedDay,
                        Toast.LENGTH_SHORT
                ).show();
            }
        }

        if (isAutoActive(this) && !tickerScheduled) {
            scheduleNextTick(450);
        }
    }

    @Override
    public void onInterrupt() {
        handler.removeCallbacks(crawlerTicker);
        tickerScheduled = false;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(crawlerTicker);
        tickerScheduled = false;
        if (activeInstance == this) {
            activeInstance = null;
        }
        super.onDestroy();
    }

    private void resetCrawlerSession() {
        completedDays.clear();
        sessionUniqueIds.clear();
        capturedPerDay.clear();
        currentTargetDay = null;
        stagnantScrolls = 0;
        listOpenTicks = 0;
        nonTargetTicks = 0;
    }

    private void scheduleNextTick(long delayMs) {
        handler.removeCallbacks(crawlerTicker);
        tickerScheduled = true;
        handler.postDelayed(crawlerTicker, delayMs);
    }

    private void stepAutoCrawler() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null
                || !OLD_PACKAGE.contentEquals(root.getPackageName())) {
            nonTargetTicks++;
            if (nonTargetTicks > 25) {
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .edit()
                        .putBoolean(KEY_AUTO_ACTIVE, false)
                        .apply();
            }
            return;
        }
        nonTargetTicks = 0;

        // Dismiss accidental "Play Video?" dialog safely without touching Delete
        List<AccessibilityNodeInfo> cancelDialogNodes = root.findAccessibilityNodeInfosByText("No, Go Back");
        if (cancelDialogNodes != null && !cancelDialogNodes.isEmpty()) {
            performGlobalAction(GLOBAL_ACTION_BACK);
            return;
        }

        AccessibilityNodeInfo recyclerNode = findFirstById(root, "com.example.poojatracker:id/poojalist");
        String headerDay = detectDayFromListHeader(root);

        if (recyclerNode != null || headerDay != null) {
            String activeDay = headerDay != null ? headerDay
                    : (currentTargetDay != null ? currentTargetDay : "Monday");
            Set<Integer> daySet = getCapturedSet(activeDay);
            int beforeCount = daySet.size();
            scrapeFromListRoot(root, activeDay);
            int afterCount = daySet.size();
            int expectedTotal = parseExpectedTotal(root);

            listOpenTicks++;
            if (afterCount == 0 && expectedTotal != 0 && listOpenTicks < 2) {
                // Give RecyclerView one extra tick to bind initial rows
                return;
            }

            if (expectedTotal == 0 || (expectedTotal > 0 && afterCount >= expectedTotal)) {
                markDayCompleted(activeDay);
                performGlobalAction(GLOBAL_ACTION_BACK);
                return;
            }

            if (afterCount > beforeCount) {
                stagnantScrolls = 0;
            } else {
                stagnantScrolls++;
            }

            boolean scrolled = recyclerNode != null
                    && recyclerNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
            if (!scrolled || stagnantScrolls >= 3) {
                markDayCompleted(activeDay);
                performGlobalAction(GLOBAL_ACTION_BACK);
            }
            return;
        }

        // Check if we are on MainActivity with Monday..Sunday buttons
        AccessibilityNodeInfo mondayNode = findFirstByIds(root, DAY_BUTTON_IDS[0]);
        AccessibilityNodeInfo sundayNode = findFirstByIds(root, DAY_BUTTON_IDS[6]);
        if (mondayNode != null || sundayNode != null) {
            int nextDayIdx = -1;
            for (int i = 0; i < MyDBHelper.DAYS_ORDER.length; i++) {
                if (!completedDays.contains(i)) {
                    nextDayIdx = i;
                    break;
                }
            }

            if (nextDayIdx == -1) {
                finishAutoMigration();
                return;
            }

            AccessibilityNodeInfo dayBtn = findFirstByIds(root, DAY_BUTTON_IDS[nextDayIdx]);
            if (dayBtn != null) {
                currentTargetDay = MyDBHelper.DAYS_ORDER[nextDayIdx];
                stagnantScrolls = 0;
                listOpenTicks = 0;
                boolean clicked = dayBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                if (!clicked && dayBtn.getParent() != null) {
                    dayBtn.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            } else {
                completedDays.add(nextDayIdx);
            }
            return;
        }

        // If the old app was sitting on PoojaAddActivity or VideoPlayerActivity, go Back to MainActivity
        performGlobalAction(GLOBAL_ACTION_BACK);
    }

    private void finishAutoMigration() {
        int totalCount = dbHelper.getTotalUniquePoojaCount();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_AUTO_ACTIVE, false)
                .putInt(KEY_LAST_COUNT, totalCount)
                .putLong(KEY_LAST_TIME, System.currentTimeMillis())
                .apply();

        Toast.makeText(
                this,
                "Recovered " + totalCount + " Poojas across all 7 days!",
                Toast.LENGTH_LONG
        ).show();

        Intent returnIntent = new Intent(this, PoojaAddActivity.class);
        returnIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        returnIntent.putExtra("migrationCompletedCount", totalCount);
        startActivity(returnIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            handler.postDelayed(this::disableSelf, 500);
        }
    }

    private void markDayCompleted(String dayName) {
        for (int i = 0; i < MyDBHelper.DAYS_ORDER.length; i++) {
            if (MyDBHelper.DAYS_ORDER[i].equalsIgnoreCase(dayName)) {
                completedDays.add(i);
                break;
            }
        }
        stagnantScrolls = 0;
        listOpenTicks = 0;
    }

    private Set<Integer> getCapturedSet(String day) {
        Set<Integer> set = capturedPerDay.get(day);
        if (set == null) {
            set = new HashSet<>();
            capturedPerDay.put(day, set);
        }
        return set;
    }

    private String detectDayFromListHeader(AccessibilityNodeInfo root) {
        AccessibilityNodeInfo dayNode = findFirstById(root, "com.example.poojatracker:id/day");
        if (dayNode == null || dayNode.getText() == null) {
            return null;
        }
        return normalizeDay(dayNode.getText().toString());
    }

    private String normalizeDay(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim().toLowerCase(Locale.US);
        if (trimmed.startsWith("mon")) return "Monday";
        if (trimmed.startsWith("tue")) return "Tuesday";
        if (trimmed.startsWith("wed")) return "Wednesday";
        if (trimmed.startsWith("thu")) return "Thursday";
        if (trimmed.startsWith("fri")) return "Friday";
        if (trimmed.startsWith("sat")) return "Saturday";
        if (trimmed.startsWith("sun")) return "Sunday";
        return null;
    }

    private int parseExpectedTotal(AccessibilityNodeInfo root) {
        AccessibilityNodeInfo detailsNode = findFirstById(root, "com.example.poojatracker:id/details");
        if (detailsNode == null || detailsNode.getText() == null) {
            return -1;
        }
        String text = detailsNode.getText().toString().trim();
        int slash = text.indexOf('/');
        if (slash < 0 || slash + 1 >= text.length()) {
            return -1;
        }
        String right = text.substring(slash + 1).trim();
        try {
            return Integer.parseInt(right);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int scrapeFromListRoot(AccessibilityNodeInfo root, String dayName) {
        int newlyAddedForDay = 0;
        Set<Integer> daySet = getCapturedSet(dayName);

        List<AccessibilityNodeInfo> cards = safeFindById(root, "com.example.poojatracker:id/mainCard");
        if (!cards.isEmpty()) {
            for (AccessibilityNodeInfo card : cards) {
                if (extractAndSaveCard(card, dayName, daySet)) {
                    newlyAddedForDay++;
                }
            }
            return newlyAddedForDay;
        }

        // Fallback: anchor off each contentURL TextView and inspect its parent container
        List<AccessibilityNodeInfo> urlNodes = safeFindById(root, "com.example.poojatracker:id/contentURL");
        for (AccessibilityNodeInfo urlNode : urlNodes) {
            AccessibilityNodeInfo container = urlNode.getParent();
            if (container != null && container.getParent() != null) {
                container = container.getParent();
            }
            if (container != null && extractAndSaveCard(container, dayName, daySet)) {
                newlyAddedForDay++;
            }
        }
        return newlyAddedForDay;
    }

    private boolean extractAndSaveCard(AccessibilityNodeInfo container, String dayName, Set<Integer> daySet) {
        AccessibilityNodeInfo nameNode = findFirstById(container, "com.example.poojatracker:id/pooja_name");
        AccessibilityNodeInfo descNode = findFirstById(container, "com.example.poojatracker:id/pooja_desc");
        AccessibilityNodeInfo urlNode = findFirstById(container, "com.example.poojatracker:id/contentURL");
        AccessibilityNodeInfo statusNode = findFirstById(container, "com.example.poojatracker:id/status");

        String title = nameNode != null && nameNode.getText() != null ? nameNode.getText().toString().trim() : "";
        String desc = descNode != null && descNode.getText() != null ? descNode.getText().toString().trim() : "";
        String url = urlNode != null && urlNode.getText() != null ? urlNode.getText().toString().trim() : "";
        boolean status = statusNode != null && statusNode.isChecked();

        if (title.isEmpty() || url.isEmpty() || "Pooja url here".equalsIgnoreCase(url)) {
            return false;
        }
        if ("Pooja description asdfdaf here".equalsIgnoreCase(desc)) {
            desc = "";
        }

        int id = MyDBHelper.deterministicId(title, url);
        dbHelper.addPooja(id, title, desc, url, status);
        dbHelper.addDay(id, dayName);
        sessionUniqueIds.add(id);
        return daySet.add(id);
    }

    private AccessibilityNodeInfo findFirstByIds(AccessibilityNodeInfo root, String[] ids) {
        for (String id : ids) {
            AccessibilityNodeInfo found = findFirstById(root, id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private AccessibilityNodeInfo findFirstById(AccessibilityNodeInfo root, String viewId) {
        List<AccessibilityNodeInfo> list = safeFindById(root, viewId);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<AccessibilityNodeInfo> safeFindById(AccessibilityNodeInfo node, String viewId) {
        if (node == null) {
            return Collections.emptyList();
        }
        try {
            List<AccessibilityNodeInfo> list = node.findAccessibilityNodeInfosByViewId(viewId);
            return list != null ? list : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
