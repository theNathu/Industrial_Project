// AppPreferences.java
// Alive Technologies
package com.example.nikhil.industrial_project;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.MenuItem;

/**
 * AppPreferences, using a single fragment in an activity.
 */
public class HeartMonitorPreferences extends ActionBarActivity {
	@SuppressWarnings("unused")
	private static final String TAG = "AppPreferences";
	
	public static final String PREF_HM_BTNAME = "heart_monitor_btname";
	public static final String PREF_HM_BTADDRESS = "heart_monitor_btaddress";
	public static final String PREF_KEEP_SCREEN_ON = "keep_screen_on";
	public static final String PREF_NETWORK_COUNTRY_CODE = "network_country_code"; // Country code from last time connected on GSM network
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
		// Set up the action bar.
		ActionBar actionBar = getSupportActionBar();
		actionBar.setTitle(R.string.action_settings);
		actionBar.setDisplayOptions( ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP);
		
		// http://stackoverflow.com/questions/14184182/why-wont-fragment-retain-state-when-screen-is-rotated
		if (savedInstanceState == null)
	    {
	    	// Display the fragment as the main content.
	    	getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
	    }
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case android.R.id.home:
	        finish();
	        break;
	
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	    return true;
	}
	
	public static SharedPreferences getSharedPreferences(Context context) {
	    return PreferenceManager.getDefaultSharedPreferences(context);
	}
	
	
	public static class PrefsFragment extends PreferenceFragment {
		private static final String KEY_HEART_MONITOR = "heart_monitor";
		//private static final String KEY_KEEP_SCREEN_ON = "keep_screen_on";
		private static final String KEY_ABOUT_APP = "about_app";
				
	    // Intent request codes
	    private static final int REQUEST_CONNECT_DEVICE = 1;
	    
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
	        final Activity activity = getActivity();
			addPreferencesFromResource(R.xml.heart_monitor_preferences);
			
	        // Get the custom preference
	        Preference customPref = (Preference) findPreference(KEY_HEART_MONITOR);
	        customPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				public boolean onPreferenceClick(Preference preference) {
		            // Launch the DeviceListActivity to see devices and do scan
		            Intent serverIntent = new Intent(activity, BTDeviceListActivity.class);
		            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
					return true;
				}
	        });
	        String hmName = getSharedPreferences(activity).getString(PREF_HM_BTNAME, null);
	        if(!TextUtils.isEmpty(hmName)) {
	        	customPref.setSummary(hmName);
	        }else {
	        	customPref.setSummary(R.string.scan_for_device);
	        }
	 		
	 		/*
			Preference pref = findPreference(KEY_ABOUT_APP);
			if(pref!=null) {
				pref.setSummary(getString(R.string.app_name) + " version " + Util.getAppVersionName() + "\n2014 Alive Technologies Pty Ltd.");					
			}*/
	 	}
		
	    
		@Override
	    public void onActivityResult(int requestCode, int resultCode, Intent data) {
	
	        switch (requestCode) {
	        case REQUEST_CONNECT_DEVICE:
	            // When DeviceListActivity returns with a device to connect
	            if (resultCode == Activity.RESULT_OK) {
	                // Get the device MAC address
	            	String prefHeartMonitorBTName = data.getExtras().getString(BTDeviceListActivity.EXTRA_DEVICE_NAME);
	            	String prefHeartMonitorBTAddress = data.getExtras().getString(BTDeviceListActivity.EXTRA_DEVICE_ADDRESS);
	                if(prefHeartMonitorBTName==null || prefHeartMonitorBTName.length()==0) {
	                	prefHeartMonitorBTName = prefHeartMonitorBTAddress;
	                }
	                SharedPreferences.Editor editor = getPreferenceScreen().getEditor();
	                editor.putString(PREF_HM_BTNAME,prefHeartMonitorBTName);
	                editor.putString(PREF_HM_BTADDRESS,prefHeartMonitorBTAddress);
	                editor.commit();
	                
	    			// Update summary in case heart monitor changed
	                Preference customPref = (Preference) findPreference(KEY_HEART_MONITOR);
	                customPref.setSummary(prefHeartMonitorBTName);
	            }
	            break;
	        }
	    }
	}   
}