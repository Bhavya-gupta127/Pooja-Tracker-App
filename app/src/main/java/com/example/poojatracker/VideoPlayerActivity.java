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
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

public class VideoPlayerActivity extends AppCompatActivity {

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
        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                String videoId = finalId;
                youTubePlayer.loadVideo(videoId, 0);
                youTubePlayer.pause(); // laggy but works to stop auto play
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
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}