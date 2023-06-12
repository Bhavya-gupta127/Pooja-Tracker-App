package com.example.poojatracker;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

public class PoojaAddActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pooja_add);

        Button submitJson=findViewById(R.id.submitJSON);
        EditText jsonUrl=findViewById(R.id.addJsonUrl);


        submitJson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Toast.makeText(PoojaAddActivity.this, jsonUrl.getText(), Toast.LENGTH_SHORT).show();
                String Url= String.valueOf(jsonUrl.getText());

                //testing
                Url="https://github.com/Bhavya-gupta127/Pooja-Tracker-App/blob/master/PoojaList.json";


                JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, Url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Toast.makeText((Context) PoojaAddActivity.this, (CharSequence) response, Toast.LENGTH_SHORT).show();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(PoojaAddActivity.this, "API Error Please check your URL and Internet connection", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}