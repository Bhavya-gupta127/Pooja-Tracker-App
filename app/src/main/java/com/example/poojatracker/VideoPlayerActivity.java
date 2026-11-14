package com.example.poojatracker;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

public class VideoPlayerActivity extends AppCompatActivity {

    String videoId;
    float savedSeekTo = 0f;
    boolean hasLaunchedExternalFallback = false;
    boolean appliedInitialPlaybackRate = false;
    YouTubePlayer activePlayer = null;
    PlayerConstants.PlaybackRate selectedRate = PlayerConstants.PlaybackRate.RATE_1;
    YouTubePlayerTracker tracker = new YouTubePlayerTracker();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#12101C"));
        setContentView(R.layout.activity_video_player);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        Intent intent = getIntent();
        String rawInput = intent != null ? intent.getStringExtra("contentURL") : null;
        String title = intent != null ? intent.getStringExtra("title") : null;
        String desc = intent != null ? intent.getStringExtra("desc") : null;
        videoId = PoojaModel.extractVideoId(rawInput);

        TextView playerTitle = findViewById(R.id.playerPoojaTitle);
        TextView playerDesc = findViewById(R.id.playerPoojaDesc);
        if (title != null && !title.trim().isEmpty()) {
            playerTitle.setText(title.trim());
        }
        if (desc != null && !desc.trim().isEmpty()) {
            playerDesc.setText(desc.trim());
            playerDesc.setVisibility(View.VISIBLE);
        }

        SharedPreferences sharedPreferences = getSharedPreferences("videoId", MODE_PRIVATE);
        savedSeekTo = sharedPreferences.getFloat(videoId, 0f);
        String savedRateKey = sharedPreferences.getString("preferred_playback_rate", "1x");
        selectedRate = keyToRate(savedRateKey);

        Button btnBackPlayer = findViewById(R.id.btnBackPlayer);
        TextView errorBanner = findViewById(R.id.player_error_banner);
        Button btnOpenYoutube = findViewById(R.id.btn_open_youtube);
        Button btnMarkCompleted = findViewById(R.id.btn_mark_completed);
        Button btnNotCompleted = findViewById(R.id.btn_not_completed);

        TextView speed05x = findViewById(R.id.speed05x);
        TextView speed1x = findViewById(R.id.speed1x);
        TextView speed15x = findViewById(R.id.speed15x);
        TextView speed2x = findViewById(R.id.speed2x);
        TextView[] speedChips = new TextView[]{speed05x, speed1x, speed15x, speed2x};
        String[] speedKeys = new String[]{"0.5x", "1x", "1.5x", "2x"};

        Runnable updateSpeedChipStyles = () -> {
            String activeKey = rateToKey(selectedRate);
            for (int i = 0; i < speedChips.length; i++) {
                if (speedKeys[i].equals(activeKey)) {
                    speedChips[i].setBackgroundResource(R.drawable.gradientbtn);
                    speedChips[i].setTextColor(Color.parseColor("#1F1B2E"));
                } else {
                    speedChips[i].setBackgroundColor(Color.parseColor("#2D2844"));
                    speedChips[i].setTextColor(Color.parseColor("#E2D9FF"));
                }
            }
        };
        updateSpeedChipStyles.run();

        for (int i = 0; i < speedChips.length; i++) {
            final String key = speedKeys[i];
            speedChips[i].setOnClickListener(v -> {
                selectedRate = keyToRate(key);
                sharedPreferences.edit().putString("preferred_playback_rate", key).apply();
                updateSpeedChipStyles.run();
                if (activePlayer != null) {
                    activePlayer.setPlaybackRate(selectedRate);
                }
            });
        }

        btnBackPlayer.setOnClickListener(v -> showCompletionDialog());
        btnOpenYoutube.setOnClickListener(v -> {
            int seekSec = tracker.getCurrentSecond() > 0f ? (int) tracker.getCurrentSecond() : (int) savedSeekTo;
            openInYouTubeApp(videoId, seekSec);
        });
        btnMarkCompleted.setOnClickListener(v -> finishWithStatus("true"));
        btnNotCompleted.setOnClickListener(v -> finishWithStatus("false"));

        YouTubePlayerView youTubePlayerView = findViewById(R.id.youtube_player_view);
        youTubePlayerView.setEnableAutomaticInitialization(false);
        getLifecycle().addObserver(youTubePlayerView);

        IFramePlayerOptions iFramePlayerOptions = new IFramePlayerOptions.Builder(this)
                .controls(1)
                .rel(0)
                .origin("https://" + getPackageName())
                .build();

        final String finalId = videoId;
        youTubePlayerView.initialize(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                activePlayer = youTubePlayer;
                savedSeekTo = sharedPreferences.getFloat(finalId, 0f);
                Log.d("youtube2", "Loading videoId=" + finalId + " at " + savedSeekTo);
                youTubePlayer.addListener(tracker);
                youTubePlayer.loadVideo(finalId, savedSeekTo);
                youTubePlayer.setPlaybackRate(selectedRate);
            }

            @Override
            public void onStateChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerState state) {
                if (state == PlayerConstants.PlayerState.PLAYING) {
                    errorBanner.setVisibility(View.GONE);
                    if (!appliedInitialPlaybackRate) {
                        appliedInitialPlaybackRate = true;
                        youTubePlayer.setPlaybackRate(selectedRate);
                    }
                }
            }

            @Override
            public void onError(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerError error) {
                Log.e("youtube_error", "Error playing " + finalId + ": " + error.name());
                if (error == PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER
                        || error == PlayerConstants.PlayerError.REQUEST_MISSING_HTTP_REFERER
                        || error == PlayerConstants.PlayerError.VIDEO_NOT_FOUND) {
                    errorBanner.setVisibility(View.VISIBLE);
                    if (!hasLaunchedExternalFallback) {
                        hasLaunchedExternalFallback = true;
                        Toast.makeText(
                                VideoPlayerActivity.this,
                                "Video owner disabled embedded playback. Opening in YouTube...",
                                Toast.LENGTH_LONG
                        ).show();
                        openInYouTubeApp(finalId, (int) savedSeekTo);
                    }
                }
            }
        }, true, iFramePlayerOptions);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showCompletionDialog();
            }
        });
    }

    private PlayerConstants.PlaybackRate keyToRate(String key) {
        if ("0.5x".equals(key)) return PlayerConstants.PlaybackRate.RATE_0_5;
        if ("1.5x".equals(key)) return PlayerConstants.PlaybackRate.RATE_1_5;
        if ("2x".equals(key)) return PlayerConstants.PlaybackRate.RATE_2;
        return PlayerConstants.PlaybackRate.RATE_1;
    }

    private String rateToKey(PlayerConstants.PlaybackRate rate) {
        if (rate == PlayerConstants.PlaybackRate.RATE_0_5) return "0.5x";
        if (rate == PlayerConstants.PlaybackRate.RATE_1_5) return "1.5x";
        if (rate == PlayerConstants.PlaybackRate.RATE_2) return "2x";
        return "1x";
    }

    private void openInYouTubeApp(String id, int startSeconds) {
        String webUrl = "https://www.youtube.com/watch?v=" + id
                + (startSeconds > 0 ? "&t=" + startSeconds + "s" : "");
        Intent appIntent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse("vnd.youtube:" + id + (startSeconds > 0 ? "?t=" + startSeconds : ""))
        );
        Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
        try {
            startActivity(appIntent);
        } catch (ActivityNotFoundException ex) {
            try {
                startActivity(webIntent);
            } catch (ActivityNotFoundException inner) {
                Toast.makeText(this, "Could not find a browser or YouTube app to open video", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void finishWithStatus(String status) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("result", status);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    private void showCompletionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Pooja Status")
                .setMessage("Mark this Pooja as completed for today?")
                .setPositiveButton("Completed", (dialog, which) -> finishWithStatus("true"))
                .setNegativeButton("Not Yet", (dialog, which) -> finishWithStatus("false"))
                .setNeutralButton("Keep Watching", null)
                .setIcon(R.drawable.omicon)
                .show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoId != null && tracker.getCurrentSecond() > 0f) {
            SharedPreferences sharedPreferences = getSharedPreferences("videoId", MODE_PRIVATE);
            sharedPreferences.edit().putFloat(videoId, tracker.getCurrentSecond()).apply();
        }
    }
}