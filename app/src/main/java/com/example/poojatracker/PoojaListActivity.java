package com.example.poojatracker;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

        //getting day name from prev activity
        Intent intent = getIntent();
        day = intent.getStringExtra("day");

        //setting day name
        TextView dayName=(TextView) findViewById(R.id.day);
        dayName.setText(day);

        //connecting sqlite database
         dbHelper = new MyDBHelper(this);

        //recycle view
        recyclerView=findViewById(R.id.poojalist);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        arrPooja=dbHelper.fetchPooja(day);
        recyclerView.setAdapter(new RecyclerPoojaAdapter(this,arrPooja,this));

        //calling next activity for result
        launcher = registerForActivityResult(new Contract(), result -> {
            // Handle the result returned from the activity here
            if (result != null) {
                // Process the result
                if(result.equals("true"))
                {
                    Toast.makeText(this, currentPooja.getTitle(), Toast.LENGTH_SHORT).show();
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
        Toast.makeText(this, "resumed", Toast.LENGTH_SHORT).show();
        arrPooja=dbHelper.fetchPooja(day);
    }

    @Override
    public void onItemClicked(PoojaModel poojaModel) {
        Intent i=new Intent(this,VideoPlayerActivity.class);
        new AlertDialog.Builder(this)
                .setTitle("Go Back?")
                .setMessage("Mumma glti so back press ho gya toh Cancel pe click krdo")
                .setPositiveButton("Go to Video", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        currentPooja=poojaModel;
                        launcher.launch(poojaModel.getVideoId());
                    }
                })
                .setNeutralButton("Cancel",null)
                .setIcon(android.R.drawable.ic_dialog_alert)
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