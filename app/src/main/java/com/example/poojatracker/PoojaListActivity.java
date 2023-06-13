package com.example.poojatracker;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.util.ArrayList;

public class PoojaListActivity extends AppCompatActivity implements SelectListener{
    ArrayList<PoojaModel> arrPooja=new ArrayList<>();
    ActivityResultLauncher<String> launcher;
    PoojaModel currentPooja;
    MyDBHelper dbHelper;
    RecyclerView recyclerView;
    RecyclerPoojaAdapter adapter;
    String day;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pooja_list);
        getSupportActionBar().hide();

        //connecting sqlite database
        dbHelper = new MyDBHelper(this);

        //resetting everyday
        SharedPreferences sharedPreferences=getSharedPreferences("lastOpen",MODE_PRIVATE);
        SharedPreferences.Editor editor=sharedPreferences.edit();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String prevDate=sharedPreferences.getString("date", String.valueOf(LocalDate.now()));
            if(!prevDate.equals(String.valueOf(LocalDate.now())))
            {
                editor.putString("date",String.valueOf(LocalDate.now()));
                dbHelper.updateDbForNewDay();
            }
        }

        //getting day name from prev activity
        Intent intent = getIntent();
        day = intent.getStringExtra("day");

        //setting day name
        TextView dayName=(TextView) findViewById(R.id.day);
        dayName.setText(day);


        //recycle view
        recyclerView=findViewById(R.id.poojalist);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        arrPooja=dbHelper.fetchPooja(day);
        recyclerView.setAdapter(new RecyclerPoojaAdapter(this,arrPooja,this));
        // Apply custom item decoration
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.spacing_between_items);
        SpaceItemDecoration itemDecoration = new SpaceItemDecoration(spacingInPixels);
        recyclerView.addItemDecoration(itemDecoration);


        //calling next activity for result
        launcher = registerForActivityResult(new Contract(), result -> {
            // Handle the result returned from the activity here
            if (result != null) {
                // Process the result
                if(result.equals("true"))
                {
                    //update arrPooja and db
                    dbHelper.updateDbForStatus(currentPooja.getId(),true);
                    currentPooja.setStatus(true);
                    arrPooja=dbHelper.fetchPooja(day);
                    recyclerView.setAdapter(new RecyclerPoojaAdapter(this,arrPooja,this));
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        arrPooja=dbHelper.fetchPooja(day);
    }

    @Override
    public void onItemClicked(PoojaModel poojaModel) {
        Intent i=new Intent(this,VideoPlayerActivity.class);
        new AlertDialog.Builder(this)
                .setTitle("Play Video?")
                .setMessage("Are you sure you want to start "+poojaModel.getTitle())
                .setPositiveButton("Yes,Go to Video", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        currentPooja=poojaModel;
                        launcher.launch(poojaModel.getVideoId());
                    }
                })
                .setNeutralButton("No, Go Back",null)
                .setIcon(R.drawable.omicon)
                .show();
    }

    @Override
    public void onCheckboxClicked(PoojaModel poojaModel) {
        Log.d("checkbox", String.valueOf(poojaModel.isStatus()));
        //update db here
        dbHelper.updateDbForStatus(poojaModel.getId(),!poojaModel.isStatus());
        //update arrList
        arrPooja=dbHelper.fetchPooja(day);

        poojaModel.setStatus(!poojaModel.isStatus());
    }


}