// Util.java
// Alive Technologies
package com.example.nikhil.industrial_project.devices.aliveclasses;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewConfiguration;

import com.example.nikhil.industrial_project.preferences.HeartMonitorPreferences;

import java.lang.reflect.Field;
import java.util.Locale;

public class Util {
	private static final String TAG = "Util";
	
    public static int fromDpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, AliveX.getContext().getResources().getDisplayMetrics());
    }
    
	public static void forceOverflowMenuButton(Context context) {
		try {
			ViewConfiguration config = ViewConfiguration.get(context);
			Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
	
			if (menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		} catch (Exception e) {
			// presumably, not relevant
		}	
	}
	public static String getAppVersionName() {
		String versionName="unknown";
		try {
			Context context = AliveX.getContext();
			versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0 ).versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return versionName;
	}

	
	// Lookup mains frequency based on the country of connected network.
	// If no network connection use last saved country, else use country from SIM card, else use country from system locale, else default to 50Hz
	// Returns 50 or 60
    public static int lookupMainsFrequency() {
    	int mainsFreq=50; // Default set to 50Hz
    	/*
		String country=null;

    	SharedPreferences prefs = HeartMonitorPreferences.getSharedPreferences(AliveX.getContext());
    	try {
			TelephonyManager manager = (TelephonyManager) AliveX.getContext().getSystemService(Context.TELEPHONY_SERVICE);
			if (manager != null) {
				if (manager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
					// Get country from network if GSM. Not reliable if on CDMA
					country = manager.getNetworkCountryIso();

				}
				if(!TextUtils.isEmpty(country)) {
					// We have country from the network, so save and use this country if later disconnected from network
					prefs.edit().putString(HeartMonitorPreferences.PREF_NETWORK_COUNTRY_CODE, country).apply();
				}else {
					// Get country from last saved value from when we were connected to the network
					country = prefs.getString(HeartMonitorPreferences.PREF_NETWORK_COUNTRY_CODE, null);
				}
				if(TextUtils.isEmpty(country)) {
					// Get country from SIM
					country = manager.getSimCountryIso();
					if(TextUtils.isEmpty(country)) {
						// Get country from systems locale
				        Locale defaultLocale = Locale.getDefault();
				        if (defaultLocale != null) {
				            country = defaultLocale.getCountry();
				        }
					}
				}
				if (!TextUtils.isEmpty(country)) {
					country = country.trim();
					String[] countries60Hz = AliveX.getContext().getResources().getStringArray(alive.R.array.Mains60HzList);
					int len = countries60Hz.length;
					for (int i = 0; i < len; i++) {
						// Search if country code is in array of countries with 60hz mains freq
						if (countries60Hz[i].equalsIgnoreCase(country)) {
							mainsFreq = 60;
							break;
						}
					}
				}
			}

    	}catch(Exception e) {
    		Log.e(TAG, "Country not found. Mains frequency set 50Hz", e);
    	}
    	*/
    	return(mainsFreq);
    }
}
