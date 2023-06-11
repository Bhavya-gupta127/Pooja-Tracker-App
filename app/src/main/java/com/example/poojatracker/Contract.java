package com.example.poojatracker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Contract extends ActivityResultContract<String, String> {

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, String input) {
        // Create and return an intent to launch the activity
        Intent intent = new Intent(context, VideoPlayerActivity.class);
        intent.putExtra("contentURL", input);
        return intent;
    }

    @Override
    public String parseResult(int resultCode, @Nullable Intent intent) {
        // Parse the result from the activity and return it
        if (resultCode == Activity.RESULT_OK && intent != null) {
            return intent.getStringExtra("result");
        }
        return null;
    }
}
