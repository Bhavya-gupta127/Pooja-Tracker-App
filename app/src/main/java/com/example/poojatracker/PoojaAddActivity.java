package com.example.poojatracker;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

import kotlin.random.URandomKt;

public class PoojaAddActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pooja_add);


        RequestQueue requestQueue;
        requestQueue = Volley.newRequestQueue(this);

        //database
        MyDBHelper dbHelper = new MyDBHelper(this);
        String[] dayMapping=new String[] {"Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"};



        //for json add
        Button submitJson = findViewById(R.id.submitJSON);
        EditText jsonUrl = findViewById(R.id.addJsonUrl);
        submitJson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String Url = String.valueOf(jsonUrl.getText());
                //testing
//                Url = "https://raw.githubusercontent.com/Bhavya-gupta127/Pooja-Tracker-App/master/PoojaList.json";
                JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, Url, null, new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            for(int i=0;i<response.length();i++)
                            {
                                JSONObject object= response.getJSONObject(i);
                                int id=object.getInt("id");
                                String title=object.getString("title");
                                String desc=object.getString("desc");
                                String contentUrl=object.getString("contentURL");
                                JSONArray days = object.getJSONArray("days");
                                for(int j=0;j<days.length();j++)
                                    if(days.getInt(j)==1)
                                        dbHelper.addDay(id,dayMapping[j]);
                                boolean status =false;

                                dbHelper.addPooja(id,title,desc,contentUrl,status);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(PoojaAddActivity.this, "API Error Please check your URL and Internet connection", Toast.LENGTH_SHORT).show();
                        Log.d("TestJson", String.valueOf(error));
                    }
                });
                requestQueue.add(jsonArrayRequest);

            }
        });

        //Custom add
        EditText addTitle = findViewById(R.id.addTitle);
        EditText addDescription = findViewById(R.id.addDescription);
        EditText addContentUrl = findViewById(R.id.addContentUrl);
        CheckBox checkmonday=findViewById(R.id.checkmonday);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) CheckBox checktuesday=findViewById(R.id.checktuesday);
        CheckBox checkwednesday=findViewById(R.id.checkWednesday);
        CheckBox checkthursday=findViewById(R.id.checkThursday);
        CheckBox checkfriday=findViewById(R.id.checkFriday);
        CheckBox checksaturday=findViewById(R.id.checkSaturday);
        CheckBox checksunday=findViewById(R.id.checkSunday);
        Button submitData = findViewById(R.id.submitData);
        submitData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Random r = new Random();
                int id = r.nextInt(999999-100000) + 100000;
                boolean[] mydays= new boolean[]{checkmonday.isChecked(),checktuesday.isChecked(),checkwednesday.isChecked(),checkthursday.isChecked(),checkfriday.isChecked(),checksaturday.isChecked(),checksunday.isChecked()};
                dbHelper.addPooja(id,String.valueOf(addTitle.getText()),String.valueOf(addDescription.getText()),String.valueOf(addContentUrl.getText()),false);
                for(int j=0;j<mydays.length;j++) {
                    Log.d("Finalbacked", String.valueOf(mydays[j]));
                    if(mydays[j])
                        dbHelper.addDay(id,dayMapping[j]);
                }
            }
        });
    }
}