package com.example.nikhil.industrial_project;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

//TODO Andries
public class Setting_Activity extends ActionBarActivity implements View.OnClickListener {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_);


    }


    @Override
    public void onClick(View v) {

        switch (v.getId())
        {
            case (R.id.buttonBloodPressureCuffPrefs) : startActivity(new Intent(Setting_Activity.this, BloodPressureCuffPreferences.class)); break;
            case (R.id.buttonHeartMonitorPrefs) : startActivity(new Intent(Setting_Activity.this, HeartMonitorPreferences.class));   break;
            case (R.id.buttonPulseOximeterPrefs) : startActivity(new Intent(Setting_Activity.this, PulseOximeterPreferences.class)); break;
        }

    }
}
