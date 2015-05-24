// AliveListener.java
// Alive Technologies
package com.example.nikhil.industrial_project.listeners;

import com.example.nikhil.industrial_project.packets.NoninPulseOxiPacket;

public interface PulseOximeterListener {
	class PulseOximeterStatus {
		public static final int PO_STATUS_NONE =0;
	    public static final int PO_STATUS_CONNECTING =1;
	    public static final int PO_STATUS_CONNECTED =2;
	    public static final int PO_STATUS_RECONNECTING =3; // Connection lost/disconnected, attempting to reconnect.
	}
    void onAliveHeartBeat(int timeSampleCount, double hr, int rrSamples);
    void onAlivePacket(int timeSampleCount, final NoninPulseOxiPacket packet);
    void onAliveStatus(int statusID);
}
