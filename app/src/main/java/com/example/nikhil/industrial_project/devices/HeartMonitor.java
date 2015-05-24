// AliveHeartMonitor.java
// Alive Technologies
package com.example.nikhil.industrial_project.devices;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;


import com.example.nikhil.industrial_project.devices.aliveclasses.HRDet;
import com.example.nikhil.industrial_project.listeners.HeartMonitorListener;
import com.example.nikhil.industrial_project.listeners.HeartMonitorListener.HeartMonitorStatus;
import com.example.nikhil.industrial_project.packets.AliveHeartMonitorPacket;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class HeartMonitor {
	private static final String TAG = "AliveHeartMonitor";

    // UUID for the Serial Port Profile (SPP)
    private static final UUID SERIALPORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private ConnectionThread mConnectionThread;
    private final BluetoothAdapter mBluetoothAdapter;
    private String mBtAddress;
    private HeartMonitorListener mListener;

    public HeartMonitor(HeartMonitorListener listener, String btAddress) {
       mListener =  listener;
       mBtAddress = btAddress;
       mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }
    
    private class ConnectionThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private boolean mmCancel=false;
        private boolean mmConnecting=false;
        
        public ConnectionThread() {
            // Use a temporary object that is later assigned to mmSocket, because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = mBluetoothAdapter.getRemoteDevice(mBtAddress);
     
            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
            	//tmp = mmDevice.createRfcommSocketToServiceRecord(SERIALPORT_UUID);
                tmp = mmDevice.createInsecureRfcommSocketToServiceRecord(SERIALPORT_UUID);
            } catch (IOException e) {
            	Log.e(TAG, "createInsecureRfcommSocketToServiceRecord() failed", e);
            }
            mmSocket = tmp;
        }
     
        public void run() {
            AliveHeartMonitorPacket hmPacket = new AliveHeartMonitorPacket();
            byte[] streamBuffer = new byte[256];
            InputStream inStream = null;
            int sampleCount=0;
            HRDet mHRDet = new HRDet();
            
            
            Log.v(TAG, "ConnectionThread started");
            
            while(mmCancel==false) {
	                
	            // Cancel discovery because it will slow down the connection
	        	mBluetoothAdapter.cancelDiscovery();
	        	mmConnecting=true;
	        	
	            try {
	                // Connect the device through the socket. This will block
	                // until it succeeds or throws an exception
	            	Log.i(TAG, "CONNECT START");
	                mmSocket.connect();
	                
	                Log.i(TAG, "CONNECT DONE");
	            } catch (IOException connectException) {
	            	Log.e(TAG, "CONNECT EXCEPTION", connectException);
	            }
	            
	            if(mmCancel==false && mmSocket.isConnected()) {
			        mHRDet.reset(sampleCount);

			        InputStream tmpInStream = null;
			        inStream = null;
			        
		   	        // Get the BluetoothSocket input and output streams
		   	        try {
		   	        	tmpInStream = mmSocket.getInputStream();
		   	        } catch (IOException e) {
		   	            Log.e(TAG, "Bluetooth socket not created", e);
		   	        }
		   	        mmConnecting=false;

		    		if(mmCancel==false && tmpInStream!=null) {

		    			mListener.onAliveStatus(HeartMonitorStatus.HM_STATUS_CONNECTED);
		    			
		    			inStream = tmpInStream;
		    			try {
				            hmPacket.init();
				            while (!mmCancel) {
				                int nBytesRead;
				                nBytesRead = inStream.read(streamBuffer); // Blocks until data arrives
				                for (int n = 0; n < nBytesRead; n++) {
				                    if (hmPacket.add(streamBuffer[n])) {
				                        // We have a packet of data from the heart monitor
				                        mListener.onAlivePacket(sampleCount, hmPacket);
				                        
				            			// Process the ECG data
				                        int len = hmPacket.getECGLength();
				                    	int startIndex = hmPacket.getECGDataIndex();
				                        byte [] buffer = hmPacket.getPacketData();

				        				for (int i = 0; i < len; i++) {
				        					int nDatum = (buffer[startIndex+i] & 0xFF);
				        					int nDelay = mHRDet.process(nDatum);
				        					if(nDelay!=0) {
				        						mListener.onAliveHeartBeat(sampleCount+i+1-nDelay, mHRDet.getHR(), mHRDet.getLastRR());
				        					}
				        				}
				        				sampleCount += len;
				                    }
				                }
				            }
				        }catch (Exception e) {
				            // We get IOException when the socket is closed by the app, or if BT out of range or heart monitor turned off, 
				            // or java.lang.SecurityException if we don't have permissions to make BT connections
				        	// System.gc();
				            Log.e(TAG, e.getMessage());
				        }
		    		} 
	            }
	            
        		if(!mmCancel) {
        			mListener.onAliveStatus(HeartMonitorStatus.HM_STATUS_RECONNECTING);
        			try {
        				// Wait for a few seconds and then try connecting again
        				int count=0;
        				while(!mmCancel && count++ < 50) { Thread.sleep(100); }
        			}catch(InterruptedException e) {}
        		}
        	}
            try {
            	mmSocket.close();
            } catch (Exception closeException) { }
            
            
       		Log.v(TAG, "ConnectionThread exited");
        }
     
        // Will cancel an in-progress connection, and close the socket
        public void cancel() {
            try {
            	mmCancel = true;
            	Log.i(TAG, "mmSocket.close() mmConneccting:" + mmConnecting);
                if(mmConnecting==false) mmSocket.close();
            } catch (IOException e) { }
        }
    }
    
    // Stop and exit the thread
    public synchronized void stopConnection() {
    	if (mConnectionThread != null) {mConnectionThread.cancel(); mConnectionThread = null;}
    	
    	mListener.onAliveStatus(HeartMonitorStatus.HM_STATUS_NONE);
    }
    public synchronized void startConnection()
    {
    	if(mConnectionThread!=null) {mConnectionThread.cancel(); mConnectionThread = null;}
    	
        // Start the thread to manage the connection and perform transmissions
        mConnectionThread = new ConnectionThread();
        mConnectionThread.start();
        mListener.onAliveStatus(HeartMonitorStatus.HM_STATUS_CONNECTING);
    }

}
