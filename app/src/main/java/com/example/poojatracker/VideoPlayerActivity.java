package com.example.poojatracker;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.time.LocalDate;

public class VideoPlayerActivity extends AppCompatActivity {


    String videoId;
    float savedSeekTo=0f;
    YouTubePlayerTracker tracker = new YouTubePlayerTracker();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        getSupportActionBar().hide();

        String id = "gXWXKjR-qII"; //default video
        Intent intent = getIntent();
        id=intent.getStringExtra("contentURL");

        YouTubePlayerView youTubePlayerView = findViewById(R.id.youtube_player_view);
        getLifecycle().addObserver(youTubePlayerView);
        String finalId = id;

        SharedPreferences sharedPreferences=getSharedPreferences("videoId",MODE_PRIVATE);
        SharedPreferences.Editor editor=sharedPreferences.edit();
        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                 videoId = finalId;
//                youTubePlayer.setPlaybackRate(2);
                youTubePlayer.setPlaybackRate(PlayerConstants.PlaybackRate.RATE_2);
//                youTubePlayer.seekTo(savedSeekTo);
//                youTubePlayer.pause(); //     laggy but works to stop auto play
                savedSeekTo=sharedPreferences.getFloat(videoId,0f);
                Log.d("youtube2", String.valueOf(savedSeekTo));
                youTubePlayer.cueVideo(videoId, savedSeekTo);

                youTubePlayer.addListener(tracker);
                tracker.getState();
                tracker.getCurrentSecond();
                tracker.getVideoDuration();
                tracker.getVideoId();
            }
        });

        ///////////////////////// exoplayer start /////////////////////

//        ExoPlayer player = new ExoPlayer.Builder(this).build();
//        PlayerView playerView=findViewById(R.id.exoPlayerView);
//        playerView.setPlayer(player);
//        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(url));
//        player.setMediaItem(mediaItem);
//        player.prepare();
//        player.play();
        ////////////////////////exoplayer end////////////////////////////
    }

    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences sharedPreferences=getSharedPreferences("videoId",MODE_PRIVATE);
        SharedPreferences.Editor editor=sharedPreferences.edit();
        Log.d("youtube", String.valueOf(tracker.getCurrentSecond()));
        editor.putFloat(videoId,tracker.getCurrentSecond());
        editor.apply();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Go Back?")
                .setMessage("Please Share the Status of Completion")
                .setPositiveButton("Completed( Go Back )", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //check the checkbox
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("result", "true");
                        setResult(Activity.RESULT_OK, resultIntent);
                        finish();

                        VideoPlayerActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton("Not Completed (Go Back)", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //check the checkbox
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("result", "false");
                        setResult(Activity.RESULT_OK, resultIntent);
                        finish();
                        VideoPlayerActivity.super.onBackPressed();
                    }
                })
                .setNeutralButton("Continue Watching (Stay Here)", null)
                .setIcon(R.drawable.omicon)
                .show();
    }
}