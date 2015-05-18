package com.example.nikhil.industrial_project;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.test.ActivityInstrumentationTestCase2;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;


public class MainMenuActivity extends ActionBarActivity implements View.OnClickListener
{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

       buttons();
    }

    private void buttons()
    {
        Button button_ECG;
        button_ECG = (Button) findViewById(R.id.button_ECG);
        button_ECG.setOnClickListener(this);

        Button button_PulseOxy;
        button_PulseOxy = (Button) findViewById(R.id.button_PulseOxy);
        button_PulseOxy.setOnClickListener(this);

        Button button_BP;
        button_BP = (Button) findViewById(R.id.button_BP);
        button_BP.setOnClickListener(this);

        Button button_Collective;
        button_Collective = (Button) findViewById(R.id.button_Collective);
        button_Collective.setOnClickListener(this);

    }

    private void ButtonClick_ECG()
    {
        Intent intent = new Intent(MainMenuActivity.this, AliveECG_Activity.class);
        startActivity(intent);
    }

    private void ButtonClick_PulseOxy()
    {
        Intent intent = new Intent(MainMenuActivity.this, PulseOxy_Activity.class);
        startActivity(intent);
    }

    private void ButtonClick_BP()
    {
        Intent intent = new Intent(MainMenuActivity.this, BP_Activity.class);
        startActivity(intent);
    }

    private void ButtonClick_Collective()
    {
        Intent intent = new Intent(MainMenuActivity.this, Collective_Activity.class);
        startActivity(intent);
    }

    private void ActionBar_Settings()
    {
        Intent intent = new Intent(MainMenuActivity.this, Setting_Activity.class);
        startActivity(intent);
    }

    public void onClick (View v)
    {
        switch (v.getId())
        {
            case R.id.button_ECG: ButtonClick_ECG(); break;
            case R.id.button_PulseOxy: ButtonClick_PulseOxy(); break;
            case R.id.button_BP: ButtonClick_BP(); break;
            case R.id.button_Collective: ButtonClick_Collective(); break;
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            ActionBar_Settings();
        }

        return super.onOptionsItemSelected(item);
    }
}
