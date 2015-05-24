// AliveListener.java
// Alive Technologies
package com.example.nikhil.industrial_project.listeners;

import com.example.nikhil.industrial_project.packets.BloodPressureCuffPacket;

public interface BloodPressureCuffListener {
	class BloodPressureCuffStatus {
		public static final int BPC_STATUS_NONE =0;
	    public static final int BPC_STATUS_CONNECTING =1;
	    public static final int BPC_STATUS_CONNECTED =2;
	    public static final int BPC_STATUS_RECONNECTING =3; // Connection lost/disconnected, attempting to reconnect.
	}
    void onAliveHeartBeat(int timeSampleCount, double hr, int rrSamples);
    void onAlivePacket(int timeSampleCount, final BloodPressureCuffPacket packet);
    void onAliveStatus(int statusID);
}
