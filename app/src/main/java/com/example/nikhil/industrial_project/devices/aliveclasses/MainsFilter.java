// MainsFilter.java
// Alive Technologies
//
// Simple averaging LP Filter that also removes 50Hz or 60Hz AC Mains noise from ECG.
package com.example.nikhil.industrial_project.devices.aliveclasses;

// Simple averaging filter
public class MainsFilter {
	private boolean mInit = true;
	private	double mD[] = new double[6];
	private int mIndex;
	private int mCount;
	
	public MainsFilter() {
        setMainsFrequency(Util.lookupMainsFrequency());
        mInit=true;
        mIndex = 0;
    }	
	
	public void init() {
		mInit=true;
	}

	public double filter(double val) {
       if(mInit==true) {
            mIndex=0;
            for(int i=0;i<mCount;i++) {
                mD[i] = val; 
            }
            mInit=false;
            return(val);
        }
        
        mD[mIndex] = val;
        double sum = 0.0;
        for(int i=0;i<mCount;i++) sum += mD[i];
       
        double avg = sum/mCount;
        mIndex++;
        if(mIndex==mCount) mIndex=0;
        return(avg);
	}

	public void setMainsFrequency(int mainsFreq) {
        // If mains is 50 Hz, average 6 samples at 300Hz
        // If mains is 60 Hz, average 5 samples at 300Hz
        mCount = mainsFreq==50 ? 6 : 5; 
    }

	public void reset() {
        mIndex=0;
        for(int i=0;i<mCount;i++) {
            mD[i] = 0; 
        }
    }
    public int getDelay() {
        return mCount/2;
    }
    

};