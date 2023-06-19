package com.example.poojatracker;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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
    int scrollTo=0;
    @SuppressLint({"MissingInflatedId", "LocalSuppress"}) TextView Details;
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
                editor.apply();
                dbHelper.updateDbForNewDay();
            }
        }

        //getting day name from prev activity
        Intent intent = getIntent();
        day = intent.getStringExtra("day");

        //setting day name
        TextView dayName=(TextView) findViewById(R.id.day);
        Details=(TextView) findViewById(R.id.details);
        Details.setText(dbHelper.fetchPoojaDetails(this,day));
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
                    scrollTo=dbHelper.getFirstUnchecked(day);
                    recyclerView.smoothScrollToPosition(scrollTo);
                    updatelist();
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        updatelist();
        Details.setText(dbHelper.fetchPoojaDetails(this,day));
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
                }) .setNegativeButton("Delete Video", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            dbHelper.deleteFromDb(poojaModel.getId(),day);
                            updatelist();
                            Toast.makeText(PoojaListActivity.this, "Deleted days ", Toast.LENGTH_SHORT).show();
                        }
                        catch (Exception e)
                        {
                            Toast.makeText(PoojaListActivity.this, "Error While deleting", Toast.LENGTH_SHORT).show();
                        }


                    }
                })
                .setNeutralButton("No, Go Back",null)
                .setIcon(R.drawable.omicon)
                .show();
    }

    private void updatelist() {
        arrPooja=dbHelper.fetchPooja(day);
        recyclerView.setAdapter(new RecyclerPoojaAdapter(this,arrPooja,this));

    }

    @Override
    public void onCheckboxClicked(PoojaModel poojaModel) {
        Log.d("checkbox", String.valueOf(poojaModel.isStatus()));
        //update db here
        dbHelper.updateDbForStatus(poojaModel.getId(),!poojaModel.isStatus());
        //update arrList
        arrPooja=dbHelper.fetchPooja(day);

        poojaModel.setStatus(!poojaModel.isStatus());

        Details.setText(dbHelper.fetchPoojaDetails(this,day));

    }


}