package com.example.poojatracker;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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