// AliveService.java
// Alive Technologies
package com.example.nikhil.industrial_project.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.nikhil.industrial_project.MainMenuActivity;
import com.example.nikhil.industrial_project.R;
import com.example.nikhil.industrial_project.devices.BloodPressureCuff;
import com.example.nikhil.industrial_project.listeners.BloodPressureCuffListener;
import com.example.nikhil.industrial_project.listeners.BloodPressureCuffListener.BloodPressureCuffStatus;
import com.example.nikhil.industrial_project.packets.BloodPressureCuffPacket;
import com.example.nikhil.industrial_project.preferences.BloodPressureCuffPreferences;

public class BloodPressureCuffService extends Service implements BloodPressureCuffListener {
	private static final String TAG = "AliveService";

	private boolean mRunning = false;
	private boolean mPause = false;
	private long mPauseTime;
    private String mBTAddress;
    private BloodPressureCuffListener mAliveListener = null;
    private int mStatus;
    private BloodPressureCuff bloodPressureCuff = null;
    private long mStartTime=0; // Used to calculate duration

    @Override
    public void onCreate() {
    	Log.d(TAG, "onCreate() called");
    	mStatus = BloodPressureCuffStatus.BPC_STATUS_NONE;
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	Log.d(TAG, "onDestroy() called");
    	if(bloodPressureCuff !=null) {
    		bloodPressureCuff.stopConnection();
    		bloodPressureCuff = null;
    	}
        stopForeground(true);
    }
    public synchronized void registerAliveListener(BloodPressureCuffListener listener)
    {
   		mAliveListener = listener;
    }
    public synchronized void unregisterAliveListener()
    {
   		mAliveListener = null;
    }
    public int getStatus() {
    	return(mStatus);
    }

    public long getElapsedTime() {
    	long elapsedTime; 
    	if(mPause) {
    		elapsedTime = mPauseTime - mStartTime;
    	}else {
    		elapsedTime = SystemClock.elapsedRealtime() - mStartTime;
    	}
    	return(elapsedTime);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	Log.d(TAG, "onStartCommand() id " + startId + ": " + intent);
    	if ((flags & START_FLAG_RETRY) == 0) {
    		// TODO If it's a restart, do something.
    		Log.i(TAG, "Restarting");
    	}else {
    		Log.i(TAG, "Starting");
    		// TODO Alternative background process.
    	}
    	mStartTime = SystemClock.elapsedRealtime();
    	
    	// Look up preferences to see what connections to start
    	SharedPreferences prefs = BloodPressureCuffPreferences.getSharedPreferences(this);

        mBTAddress = prefs.getString(BloodPressureCuffPreferences.PREF_BP_BTADDRESS, "");
    	
        // We want this service to continue running until it is explicitly stopped, so return sticky.
    	startForeground();
    	
    	if(mBTAddress.length()>0 && bloodPressureCuff ==null) {

    		mStatus = BloodPressureCuffStatus.BPC_STATUS_CONNECTING;
    		bloodPressureCuff = new BloodPressureCuff(this,mBTAddress);
    		bloodPressureCuff.startConnection();
    		mRunning = true;
    	}else {
    		Log.i(TAG, "No started");
    	}
    	
    	// START_NOT_STICKY:  If this service's process is killed, the system will not try to re-created the service.
    	// START_STICKY: If this service's process is killed, later the system will try to re-created the service.
        return START_STICKY;
    }

    public boolean isRunning() {
    	return mRunning;
    }
  
    public void pause() {
    	mPause = true;
    	mPauseTime = SystemClock.elapsedRealtime();
    }
    public void resume() {
    	if(mPause) {
    		mStartTime += SystemClock.elapsedRealtime() - mPauseTime;
    	}
    	mPause = false;
    	
    }
    public boolean isPaused() {
    	return(mPause);
    }
    
    public void stop() {
    	Log.d(TAG, "stop()");
    	if(mRunning) {
	    	mRunning = false;
	    	mPause = false;
	
	    	if(bloodPressureCuff !=null) {
	    		bloodPressureCuff.stopConnection();
	    		bloodPressureCuff = null;
	    	} 	
	    	stopForeground(true);
    	}
    	stopSelf();
    }
    
    private void startForeground() {
		/*
    	Log.d(TAG, "startForeground");
        
        Intent intent = new Intent(this, MainMenuActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
        		.setOngoing(true)
                .setSmallIcon(R.drawable.ic_stat_app)
                .setContentIntent(contentIntent)
                .setContentTitle(getText(R.string.app_name))
                .setContentText(getText(R.string.hmservice_running));

        // Using string id as a unique number.
        startForeground(R.string.hmservice_running, mBuilder.build());
        */
  	
    }

    
    // Class for clients to access.  Because we know this service always
    // runs in the same process as its clients, we don't need to deal with IPC.
    public class HMBinder extends Binder {
    	BloodPressureCuffService getService() {
            return BloodPressureCuffService.this;
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // This is the object that receives interactions from clients.
    private final IBinder mBinder = new HMBinder();
    
    
    // HeartMonitorListener callbacks
    public void onAlivePacket(int timeSampleCount, BloodPressureCuffPacket packet) {
    	if(mPause) return;
    	synchronized(this) {
    		if(mAliveListener!=null) {
    			mAliveListener.onAlivePacket(timeSampleCount, packet);
    		}
    	}
    }
    public void onAliveHeartBeat(int timeSampleCount, double heartRate, int rrSamples) {
    	if(mPause) return;
    	synchronized(this) {
    		if(mAliveListener!=null) {
    			mAliveListener.onAliveHeartBeat(timeSampleCount, heartRate, rrSamples);
    		}
    	}
    }
    public void onAliveStatus(int statusID) {
    	mStatus = statusID;
    	if(mPause) return;
    	synchronized(this) {
    		if(mAliveListener!=null) {
    			mAliveListener.onAliveStatus(statusID);
    		}
    	}
    }

}
