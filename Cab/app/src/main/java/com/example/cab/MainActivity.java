package com.example.cab;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void travelerclick(View v1)
    {
        Intent i = new Intent(getApplicationContext(),TravelerLoginActivity.class);
        startActivity(i);

    }

    public void driverclick(View v2)
    {
        Intent j = new Intent(getApplicationContext(),DriverLoginActivity.class);
        startActivity(j);

    }
}
