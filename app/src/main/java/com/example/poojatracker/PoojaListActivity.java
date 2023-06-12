package com.example.poojatracker;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
        dbHelper.addPooja(1,"Inserting Data in Database","WsCube their online presence, grow their business, and reach new heights. ","https://www.youtube.com/watch?v=rK4walNCMzI&list=PLjVLYmrlmjGdDps6HAwOOVoAtBPAgIOXL&index=90",false);
        dbHelper.addDay(1,"MONDAY");
        dbHelper.addDay(1,"TUESDAY");
        dbHelper.addDay(1,"SATURDAY");

        dbHelper.addPooja(2,"Fetch the Data in Database","WsCube their online presence, grow their business, and reach new heights. ","https://www.youtube.com/watch?v=ZDWg9qFwIIY&list=PLjVLYmrlmjGdDps6HAwOOVoAtBPAgIOXL&index=91",false);
        dbHelper.addDay(2,"TUESDAY");
        dbHelper.addDay(2,"SATURDAY");

        dbHelper.addPooja(3,"asdfasdf Data in Database","WsCube their online presence, grow their business, and reach new heights. ","https://www.youtube.com/watch?v=rK4walNCMzI&list=PLjVLYmrlmjGdDps6HAwOOVoAtBPAgIOXL&index=90",false);


        //recycle view
        RecyclerView recyclerView=findViewById(R.id.poojalist);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //making object of poojamodel to pass in array arrpooja
//        PoojaModel model = new PoojaModel("img url here","title","desc","https://www.youtube.com/watch?v=FEqF1_jDV-A&t=2982s",false);

        if(day.equals("Monday")) {

            arrPooja.add(new PoojaModel(4, "monday 1", "desc", "https://www.youtube.com/watch?v=gXWXKjR-qII", false));
//            arrPooja.add(new PoojaModel("img url here", "monday 2", "desc", "https://www.youtube.com/watch?v=O1UqQhhA3Ns", false));
        }
        if(day.equals("Tuesday")) {

//            arrPooja.add(new PoojaModel("img url here", "tuesday 1", "desc", "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", false));
//            arrPooja.add(new PoojaModel("img url here", "2nd title", "desc lorem10", "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", true));
        }
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

                    currentPooja.setStatus(true);
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

//        i.putExtra("contentURL",poojaModel.getContentURL());

//        startActivityForResult(i,1);
        //        startActivity(i);

//        call next activity here
        //!add promt to prevent acidental clicks
//        Toast.makeText(this, poojaModel.getTitle(), Toast.LENGTH_SHORT).show();
    }
}