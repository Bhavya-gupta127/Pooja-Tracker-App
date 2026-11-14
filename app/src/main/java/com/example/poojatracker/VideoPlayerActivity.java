package com.example.poojatracker;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
    YouTubePlayerTracker tracker = new YouTubePlayerTracker();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        Intent intent = getIntent();
        String rawInput = intent != null ? intent.getStringExtra("contentURL") : null;
        videoId = PoojaModel.extractVideoId(rawInput);

        SharedPreferences sharedPreferences = getSharedPreferences("videoId", MODE_PRIVATE);
        savedSeekTo = sharedPreferences.getFloat(videoId, 0f);

        TextView errorBanner = findViewById(R.id.player_error_banner);
        Button btnOpenYoutube = findViewById(R.id.btn_open_youtube);
        Button btnMarkCompleted = findViewById(R.id.btn_mark_completed);
        Button btnNotCompleted = findViewById(R.id.btn_not_completed);

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
                savedSeekTo = sharedPreferences.getFloat(finalId, 0f);
                Log.d("youtube2", "Loading videoId=" + finalId + " at " + savedSeekTo);
                youTubePlayer.addListener(tracker);
                youTubePlayer.loadVideo(finalId, savedSeekTo);
                youTubePlayer.setPlaybackRate(PlayerConstants.PlaybackRate.RATE_2);
            }

            @Override
            public void onStateChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerState state) {
                if (state == PlayerConstants.PlayerState.PLAYING) {
                    errorBanner.setVisibility(View.GONE);
                    if (!appliedInitialPlaybackRate) {
                        appliedInitialPlaybackRate = true;
                        youTubePlayer.setPlaybackRate(PlayerConstants.PlaybackRate.RATE_2);
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
                .setTitle("Go Back?")
                .setMessage("Please Share the Status of Completion")
                .setPositiveButton("Completed( Go Back )", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finishWithStatus("true");
                    }
                })
                .setNegativeButton("Not Completed (Go Back)", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finishWithStatus("false");
                    }
                })
                .setNeutralButton("Continue Watching (Stay Here)", null)
                .setIcon(R.drawable.omicon)
                .show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoId != null && tracker.getCurrentSecond() > 0f) {
            SharedPreferences sharedPreferences = getSharedPreferences("videoId", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            Log.d("youtube", String.valueOf(tracker.getCurrentSecond()));
            editor.putFloat(videoId, tracker.getCurrentSecond());
            editor.apply();
        }
    }
}