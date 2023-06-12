package com.example.poojatracker;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pooja_list);

        //getting day name from prev activity
        Intent intent = getIntent();
        String day = intent.getStringExtra("day");

        //setting day name
        TextView dayName=(TextView) findViewById(R.id.day);
        dayName.setText(day);

        //connecting sqlite database
        MyDBHelper dbHelper = new MyDBHelper(this);

        //recycle view
        RecyclerView recyclerView=findViewById(R.id.poojalist);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

//use map to do this as well
        if(day.equals("Monday")) {
            Log.d("dbtest",day);
            arrPooja=dbHelper.fetchPooja(day);
        }
        if(day.equals("Tuesday")) {
            arrPooja=dbHelper.fetchPooja("Tuesday");
        }
//        ...


        RecyclerPoojaAdapter adapter=new RecyclerPoojaAdapter(this,arrPooja,this);
        recyclerView.setAdapter(adapter);

        //calling next activity for result
        launcher = registerForActivityResult(new Contract(), result -> {
            // Handle the result returned from the activity here
            if (result != null) {
                // Process the result
                if(result.equals("true"))
                {
                    Toast.makeText(this, currentPooja.getTitle(), Toast.LENGTH_SHORT).show();
                    currentPooja.setStatus(1);
                    //update dp here
//                    recyclerView.setAdapter(new RecyclerPoojaAdapter(this,arrPooja,this));
                }
//                Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public void onItemClicked(PoojaModel poojaModel) {
        Intent i=new Intent(this,VideoPlayerActivity.class);
        currentPooja=poojaModel;
        launcher.launch(poojaModel.getVideoId());
        //!add promt to prevent acidental clicks
//        Toast.makeText(this, poojaModel.getTitle(), Toast.LENGTH_SHORT).show();
    }
}