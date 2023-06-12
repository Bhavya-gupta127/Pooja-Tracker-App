package com.example.poojatracker;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.time.LocalDate;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        SharedPreferences sharedPreferences=getSharedPreferences("lastOpen",MODE_PRIVATE);
        SharedPreferences.Editor editor=sharedPreferences.edit();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String prevDate=sharedPreferences.getString("date", String.valueOf(LocalDate.now()));
            if(prevDate.equals(String.valueOf(LocalDate.now())))
                Toast.makeText(this, "Dont Update database", Toast.LENGTH_SHORT).show();
            else
            {
                Toast.makeText(this, "Update database", Toast.LENGTH_SHORT).show();
                editor.putString("date",String.valueOf(LocalDate.now()));
            }

//            Log.d("dateset",prevDate);
//            Log.d("dateset",String.valueOf(LocalDate.now()));

        }


        Button monday=findViewById(R.id.monday);
        Button tuesday=findViewById(R.id.tuesday);
        Button wednesday=findViewById(R.id.wednesday);
        Button add=findViewById(R.id.add);
//      !add ..4 more
        Intent intent=new Intent(MainActivity.this,PoojaListActivity.class);

        monday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent.putExtra("day","Monday");
                startActivity(intent);
            }
        });
        tuesday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent.putExtra("day","Tuesday");
                startActivity(intent);
            }
        });
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i=new Intent(MainActivity.this,PoojaAddActivity.class);
                startActivity(i);
            }
        });
    }
}