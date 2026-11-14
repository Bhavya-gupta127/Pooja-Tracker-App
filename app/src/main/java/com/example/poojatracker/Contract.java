package com.example.poojatracker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Contract extends ActivityResultContract<PoojaModel, String> {

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, PoojaModel input) {
        Intent intent = new Intent(context, VideoPlayerActivity.class);
        if (input != null) {
            intent.putExtra("contentURL", input.getVideoId());
            intent.putExtra("title", input.getTitle());
            intent.putExtra("desc", input.getDesc());
        }
        return intent;
    }

    @Override
    public String parseResult(int resultCode, @Nullable Intent intent) {
        if (resultCode == Activity.RESULT_OK && intent != null) {
            return intent.getStringExtra("result");
        }
        return null;
    }
}
