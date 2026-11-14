package com.example.poojatracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private MyDBHelper dbHelper;
    private View[] dayCards;
    private TextView[] dayBadges;
    private TextView[] todayBadges;
    private TextView homeSubtitle;

    private final String[] dayNames = MyDBHelper.DAYS_ORDER;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_color));
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_main);

        dbHelper = new MyDBHelper(this);

        homeSubtitle = findViewById(R.id.homeSubtitle);
        dayCards = new View[]{
                findViewById(R.id.monday),
                findViewById(R.id.tuesday),
                findViewById(R.id.wednesday),
                findViewById(R.id.thurday),
                findViewById(R.id.friday),
                findViewById(R.id.saturday),
                findViewById(R.id.sunday)
        };
        dayBadges = new TextView[]{
                findViewById(R.id.badgeMon),
                findViewById(R.id.badgeTue),
                findViewById(R.id.badgeWed),
                findViewById(R.id.badgeThu),
                findViewById(R.id.badgeFri),
                findViewById(R.id.badgeSat),
                findViewById(R.id.badgeSun)
        };
        todayBadges = new TextView[]{
                findViewById(R.id.todayBadgeMon),
                findViewById(R.id.todayBadgeTue),
                findViewById(R.id.todayBadgeWed),
                findViewById(R.id.todayBadgeThu),
                findViewById(R.id.todayBadgeFri),
                findViewById(R.id.todayBadgeSat),
                findViewById(R.id.todayBadgeSun)
        };

        for (int i = 0; i < dayCards.length; i++) {
            final String day = dayNames[i];
            dayCards[i].setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, PoojaListActivity.class);
                intent.putExtra("day", day);
                startActivity(intent);
            });
        }

        View.OnClickListener openAddListener = v -> {
            Intent i = new Intent(MainActivity.this, PoojaAddActivity.class);
            startActivity(i);
        };
        Button add = findViewById(R.id.add);
        Button btnTopAdd = findViewById(R.id.btnTopAdd);
        add.setOnClickListener(openAddListener);
        btnTopAdd.setOnClickListener(openAddListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        dbHelper.ensureDailyReset(this);
        refreshDashboardSummary();
    }

    private void refreshDashboardSummary() {
        Map<String, int[]> summary = dbHelper.getAllDayCounts();

        Calendar calendar = Calendar.getInstance();
        int calDay = calendar.get(Calendar.DAY_OF_WEEK);
        // Convert Calendar.SUNDAY(1)..SATURDAY(7) to Monday(0)..Sunday(6)
        int todayIndex = (calDay == Calendar.SUNDAY) ? 6 : (calDay - Calendar.MONDAY);

        for (int i = 0; i < dayNames.length; i++) {
            int[] counts = summary.get(dayNames[i]);
            int checked = counts != null ? counts[0] : 0;
            int total = counts != null ? counts[1] : 0;

            boolean isToday = (i == todayIndex);
            todayBadges[i].setVisibility(isToday ? View.VISIBLE : View.GONE);
            dayCards[i].setBackgroundResource(
                    isToday ? R.drawable.card_today_surface : R.drawable.card_surface
            );

            if (total == 0) {
                dayBadges[i].setText("0 Poojas");
                dayBadges[i].setBackgroundResource(R.drawable.badge_pill_bg);
                dayBadges[i].setTextColor(ContextCompat.getColor(this, R.color.card_text_secondary));
            } else if (checked >= total) {
                dayBadges[i].setText("✓ " + checked + " / " + total);
                dayBadges[i].setBackgroundResource(R.drawable.badge_done_bg);
                dayBadges[i].setTextColor(ContextCompat.getColor(this, R.color.badge_done_text));
            } else {
                dayBadges[i].setText(checked + " / " + total);
                dayBadges[i].setBackgroundResource(R.drawable.badge_pill_bg);
                dayBadges[i].setTextColor(ContextCompat.getColor(this, R.color.badge_pill_text));
            }
        }

        int[] todayCounts = summary.get(dayNames[todayIndex]);
        int todayChecked = todayCounts != null ? todayCounts[0] : 0;
        int todayTotal = todayCounts != null ? todayCounts[1] : 0;
        String formattedDate = new SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(new Date());
        if (todayTotal > 0) {
            homeSubtitle.setText(formattedDate + " • " + todayChecked + " of " + todayTotal + " completed today");
        } else if (dbHelper.getTotalUniquePoojaCount() == 0
                && getPackageManager().getLaunchIntentForPackage(OldAppMigrationService.OLD_PACKAGE) != null) {
            homeSubtitle.setText(formattedDate + " • Old app detected! Tap + Manage to recover");
        } else {
            homeSubtitle.setText(formattedDate + " • Tap + Manage to build your schedule");
        }
    }
}