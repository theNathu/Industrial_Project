// QRSDet.java
// Alive Technologies
//
// Adapted from QRSdet2 by Patrick Hamilton, http://www.eplimited.com
/*****************************************************************************
FILE:  qrsdet2.cpp
AUTHOR:	Patrick S. Hamilton
REVISED:	7/08/2002
  ___________________________________________________________________________

qrsdet2.cpp: A QRS detector.
Copywrite (C) 2002 Patrick S. Hamilton

This file is free software; you can redistribute it and/or modify it under
the terms of the GNU Library General Public License as published by the Free
Software Foundation; either version 2 of the License, or (at your option) any
later version.

This software is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE.  See the GNU Library General Public License for more
details.

You should have received a copy of the GNU Library General Public License along
with this library; if not, write to the Free Software Foundation, Inc., 59
Temple Place - Suite 330, Boston, MA 02111-1307, USA.

You may contact the author by e-mail (pat@eplimited.edu) or postal mail
(Patrick Hamilton, E.P. Limited, 35 Medford St., Suite 204 Somerville,
MA 02143 USA).  For updates to this software, please visit our website
(http://www.eplimited.com).
  __________________________________________________________________________

This file contains functions for detecting QRS complexes in an ECG.  The
QRS detector requires filter functions in qrsfilt.cpp and parameter
definitions in qrsdet.h.  QRSDet is the only function that needs to be
visable outside of these files.

Syntax:
	int QRSDet(int ecgSample, int init) ;

Description:
	QRSDet() implements a modified version of the QRS detection
	algorithm described in:

	Hamilton, Tompkins, W. J., "Quantitative investigation of QRS
	detection rules using the MIT/BIH arrhythmia database",
	IEEE Trans. Biomed. Eng., BME-33, pp. 1158-1165, 1987.

	Consecutive ECG samples are passed to QRSDet.  QRSDet was
	designed for a 200 Hz sample rate.  QRSDet contains a number
	of static variables that it uses to adapt to different ECG
	signals.  These variables can be reset by passing any value
	not equal to 0 in init.

	Note: QRSDet() requires filters in QRSFilt.cpp

Returns:
	When a QRS complex is detected QRSDet returns the detection delay.

****************************************************************/

package com.example.nikhil.industrial_project.devices.aliveclasses;

import com.example.nikhil.industrial_project.devices.aliveclasses.MainsFilter;

public class QrsDet {
	public static final int SAMPLE_RATE = 300; // Sample rate in Hz
	
	private static final String TAG = "QrsDet";
    private static final int MS10 = ((int) (10 / ((double) 1000 / (double) SAMPLE_RATE) + 0.5));
    private static final int MS25 = ((int) (25 / ((double) 1000 / (double) SAMPLE_RATE) + 0.5));
    private static final int MS80 = ((int) (80 / ((double) 1000 / (double) SAMPLE_RATE) + 0.5));
    private static final int MS95 = ((int) (95 / ((double) 1000 / (double) SAMPLE_RATE) + 0.5));
    private static final int MS100 = ((int) (100 / ((double) 1000 / (double) SAMPLE_RATE) + 0.5));
    private static final int MS125 = ((int) (125 / ((double) 1000 / (double) SAMPLE_RATE) + 0.5));
    private static final int MS150 = ((int) (150 / ((double) 1000 / (double) SAMPLE_RATE) + 0.5));
    private static final int MS195 = ((int) (195 / ((double) 1000 / (double) SAMPLE_RATE) + 0.5));
    private static final int MS220 = ((int) (220 / ((double) 1000 / (double) SAMPLE_RATE) + 0.5));
    private static final int MS360 = ((int) (360 / ((double) 1000 / (double) SAMPLE_RATE) + 0.5));
    private static final int MS1000 = SAMPLE_RATE;
    private static final int MS1500 = ((int) (1500 / ((double) 1000 / (double) SAMPLE_RATE)));
    private static final int DERIV_LENGTH = MS10;
    private static final int LPBUFFER_LGTH = ((int) (2 * MS25));
    private static final int HPBUFFER_LGTH = MS125;
    private static final int PRE_BLANK = MS195;
    private static final int MIN_PEAK_AMP = 7; // Prevents detections of peaks smaller than about 300 uV.
    
    /// Detector threshold  = 0.3125 = TH_NUMERATOR/TH_DENOMINATOR
    private static final int TH_NUMERATOR = 3125;
    private static final int TH_DENOMINATOR = 10000;
    private static final int WINDOW_WIDTH = MS80;           // Moving window integration width.
    private static final int FILTER_DELAY = (int) (((double) DERIV_LENGTH / 2) + ((double) LPBUFFER_LGTH / 2 - 1) + (((double) HPBUFFER_LGTH - 1) / 2) + PRE_BLANK);  // filter delays plus 200 ms blanking delay
    private static final int DER_DELAY = WINDOW_WIDTH + FILTER_DELAY + MS100;    // Variables for peakHeight function
    private int mPeakMax = 0;
    private int mPeakTimeSinceMax = 0;
    private int mPeakLastDatum = 0;
    private int mDDBuffer[] = new int[DER_DELAY]; // Buffer holding derivative data.
    private int mDly = 0;
    private int mDDPtr;
    private int mDelay = 0;
    private int mDetThresh,  mQpkcnt = 0;
    private int mQrsBuf[] = new int[8];
    private int mNoiseBuf[] = new int[8];
    private int mRRBuf[] = new int[8];
    private int mRsetBuf[] = new int[8];
    private int mRsetCount = 0;
    private int mNMean,  mQMean,  mRRMean;
    private int mCount;
    private int mSBPeak = 0;
    private int mSBLoc;
    private int mSBCount = MS1500;
    private int mMaxDer,  mLastMax;
    private int mInitBlank,  mInitMax;
    private int mPreBlankCnt,  mTempPeak;
    private QRSFilter mQRSFilter = new QRSFilter();
    private Deriv mDeriv1 = new Deriv();
    private MainsFilter mMainsFilter = new MainsFilter();

    public QrsDet() {
        init();
    }

    public void init() {
        // Initialize all buffers
        for (int i = 0; i < 8; ++i) {
            mNoiseBuf[i] = 0; // Initialize noise buffer
            mRRBuf[i] = MS1000; // and R-to-R interval buffer.
        }

        mQpkcnt = mMaxDer = mLastMax = mCount = mSBPeak = 0;
        mInitBlank = mInitMax = mPreBlankCnt = mDDPtr = 0;
        mSBCount = MS1500;
        mQRSFilter.init(); // initialise filters
        mDeriv1.init();
        mMainsFilter.init();
        peakHeight(0, 1);
        
        // initialize derivative buffer
        for(int i=0;i<DER_DELAY;i++) {
        	mDDBuffer[i] = 0;
        }
    }
    public void setMainsFrequency(int freq) {
    	mMainsFilter.setMainsFrequency(freq);
    }
    
    public int process(int datum) {
        int mfdatum, fdatum;
        int qrsDelay = 0;
        int i, newPeak, aPeak;

        // Filter data
        mfdatum = (int)mMainsFilter.filter(datum);
        fdatum = mQRSFilter.addSample(mfdatum);   
   
        // Wait until normal detector is ready before calling early detections.
        aPeak = peakHeight(fdatum, 0);
        if (aPeak < MIN_PEAK_AMP) {
            aPeak = 0;        
            // Hold any peak that is detected for 200 ms
            // in case a bigger one comes along.  There
            // can only be one QRS complex in any 200 ms window.
        }
        newPeak = 0;
        
        // If there has been no peak for 200 ms save this one and start counting.
        if (aPeak != 0 && mPreBlankCnt == 0) 
        {                                       
            mTempPeak = aPeak;
            mPreBlankCnt = PRE_BLANK;           // MS200
        } 
        // If we have held onto a peak for 200 ms pass it on for evaluation.
        else if (aPeak == 0 && mPreBlankCnt != 0) 
        {                                       
            if (--mPreBlankCnt == 0) {
                newPeak = mTempPeak;
            }
        } 
        // If we were holding a peak, but this ones bigger, save it and start
        // counting to 200 ms again.
        else if (aPeak != 0) 
        { 
            if (aPeak > mTempPeak) 
            {
                mTempPeak = aPeak;
                mPreBlankCnt = PRE_BLANK; // MS200
            } else if (--mPreBlankCnt == 0) {
                newPeak = mTempPeak;
            }
        }

        // Save derivative of raw signal for T-wave and baseline shift discrimination.
        mDDBuffer[mDDPtr] = mDeriv1.addSample(mfdatum);
        if (++mDDPtr == DER_DELAY) {
            mDDPtr = 0;
        }
        // Initialize the qrs peak buffer with the first eight
        // local maximum peaks detected. 
        if (mQpkcnt < 8) {
            ++mCount;
            if (newPeak > 0) {
                mCount = WINDOW_WIDTH;
            }
            if (++mInitBlank == MS1000) {
                mInitBlank = 0;
                mQrsBuf[mQpkcnt] = mInitMax;
                mInitMax = 0;
                ++mQpkcnt;
               
    			// Mod so that detection is faster at starting
    			if(mQpkcnt == 2) 
    			{
    				mQpkcnt = 8;
    				mQMean = (mQrsBuf[0] + mQrsBuf[1]) / 2;
    				mQrsBuf[2] = mQrsBuf[4] = mQrsBuf[6] = mQrsBuf[0];
    				mQrsBuf[3] = mQrsBuf[5] = mQrsBuf[7] = mQrsBuf[1];
    				mNMean = 0 ;
    				mRRMean = MS1000 ;
    				mSBCount = MS1500+MS150 ;
    				mDetThresh = thresh(mQMean, mNMean) ;
    			}	
            }
            if (newPeak > mInitMax) {
                mInitMax = newPeak;
            }
        }else { // Else test for a qrs.
            ++mCount;
            if (newPeak > 0) {

            	// Check for maximum derivative and matching minima and maxima
                // for T-wave and baseline shift rejection.  Only consider this
                // peak if it doesn't seem to be a base line shift.

                if (baselineShiftCheck(mDDBuffer, mDDPtr) == 0) {
    				mDelay = WINDOW_WIDTH + mDly ;


    				// If a peak occurs within 360 ms of the last beat it might be a T-wave.
    				// Classify it as noise if its maximum derivative
    				// is less than 1/2 the maximum derivative in the last detected beat.

    				if((mMaxDer < (mLastMax/2)) // less than one third
    					&& ((mCount - mDelay) < MS360))
    				{ // store the new peak as noise and go on
    					shiftArrayValues(mNoiseBuf);
    					mNoiseBuf[0] = newPeak ;
    					mNMean  = mean(mNoiseBuf,8) ;
    					mDetThresh = thresh(mQMean,mNMean) ;
    				}
    				// Classify the beat as a QRS complex
    				// if it has been at least 360 ms since the last detection
    				// or the maximum derivative was large enough, and the
    				// peak is larger than the detection threshold.
    				
                    // Classify the beat as a QRS complex
                    // if the peak is larger than the detection threshold.

    				else if (newPeak > mDetThresh) {
                        shiftArrayValues(mQrsBuf);
                        mQrsBuf[0] = newPeak;
                        mQMean = mean(mQrsBuf, 8);
                        mDetThresh = thresh(mQMean, mNMean);
                        shiftArrayValues(mRRBuf);
                        mRRBuf[0] = mCount - mDelay;
                        mRRMean = mean(mRRBuf, 8);
                        mSBCount = mRRMean + (mRRMean >> 1) + WINDOW_WIDTH;
                        mCount = mDelay;

                        mSBPeak = 0;

                        mLastMax = mMaxDer;
                        mMaxDer = 0;
                        qrsDelay = mDelay + FILTER_DELAY;
                        mInitBlank = mInitMax = mRsetCount = 0;
                    } 
                    // If a peak isn't a QRS update noise buffer and estimate.
                    // Store the peak for possible search back.
                    else {
                        shiftArrayValues(mNoiseBuf);
                        mNoiseBuf[0] = newPeak;
                        mNMean = mean(mNoiseBuf, 8);
                        mDetThresh = thresh(mQMean, mNMean);

                        // Don't include early peaks (which might be T-waves)
                        // in the search back process.  A T-wave can mask
                        // a small following QRS.

                        if ((newPeak > mSBPeak) && ((mCount - WINDOW_WIDTH) >= MS360)) {
                            mSBPeak = newPeak;
                            mSBLoc = mCount - mDelay;
                        }
                    }
                }
            }

            // Test for search back condition.  If a QRS is found in
            // search back update the QRS buffer and mDetThresh.
            if ((mCount > mSBCount) && (mSBPeak > (mDetThresh >> 1))) {
                shiftArrayValues(mQrsBuf);
                mQrsBuf[0] = mSBPeak;
                mQMean = mean(mQrsBuf, 8);
                mDetThresh = thresh(mQMean, mNMean);
                shiftArrayValues(mRRBuf);
                mRRBuf[0] = mSBLoc;
                mRRMean = mean(mRRBuf, 8);
                mSBCount = mRRMean + (mRRMean >> 1) + WINDOW_WIDTH;
                qrsDelay = mDelay = mCount = mCount - mSBLoc;
                qrsDelay += FILTER_DELAY;
                mSBPeak = 0;
                mLastMax = mMaxDer;
                mMaxDer = 0;

                mInitBlank = mInitMax = mRsetCount = 0;
            }
        }

        // In the background estimate threshold to replace adaptive threshold
        // if eight seconds elapses without a QRS detection.
        if (mQpkcnt == 8) {
            if (++mInitBlank == MS1000) {
                mInitBlank = 0;
                mRsetBuf[mRsetCount] = mInitMax;
                mInitMax = 0;
                ++mRsetCount;

                // Reset threshold if it has been 8 seconds without a detection.
                if (mRsetCount == 8) {
                    for (i = 0; i < 8; ++i) {
                        mQrsBuf[i] = mRsetBuf[i];
                        mNoiseBuf[i] = 0;
                    }
                    mQMean = mean(mRsetBuf, 8);
                    mNMean = 0;
                    mRRMean = MS1000;
                    mSBCount = MS1500 + MS150;
                    mDetThresh = thresh(mQMean, mNMean);
                    mInitBlank = mInitMax = mRsetCount = 0;
                }
            }
            if (newPeak > mInitMax) {
                mInitMax = newPeak;
            }
        }
        
        if(qrsDelay>0) {
        	qrsDelay += mMainsFilter.getDelay();
        }
        return (qrsDelay);
    }

    // Shift array values to the right
    private void shiftArrayValues(int data[]) {
        int nLength = data.length;
        for (int i = nLength - 1; i > 0; i--) {
            data[i] = data[i - 1];
        }
    }

    /**************************************************************
     * peakHeight() takes a datum as input and returns a peak height
     * when the signal returns to half its peak height, or
     **************************************************************/
    private int peakHeight(int datum, int init) {
        int pk = 0;

        if (init != 0) {
            mPeakMax = mPeakTimeSinceMax = 0;
        }
        if (mPeakTimeSinceMax > 0) {
            ++mPeakTimeSinceMax;
        }
        if ((datum > mPeakLastDatum) && (datum > mPeakMax)) {
            mPeakMax = datum;
            if (mPeakMax > 2) {
                mPeakTimeSinceMax = 1;
            }
        } else if (datum < (mPeakMax >> 1)) {
            pk = mPeakMax;
            mPeakMax = 0;
            mPeakTimeSinceMax = 0;
            mDly = 0;
        } else if (mPeakTimeSinceMax > MS95) {
            pk = mPeakMax;
            mPeakMax = 0;
            mPeakTimeSinceMax = 0;
            mDly = 3;
        }
        mPeakLastDatum = datum;
        return (pk);
    }

    /********************************************************************
    mean returns the mean of an array of integers.  It uses a slow
    sort algorithm, but these arrays are small, so it hardly matters.
     ********************************************************************/
    private int mean(int[] array, int datnum) {
        long sum;
        int i;

        for (i = 0    , sum = 0; i < datnum; ++i) {
            sum += array[i];
        }
        sum /= datnum;
        return ((int) sum);
    }

    /****************************************************************************
    thresh() calculates the detection threshold from the qrs mean and noise
    mean estimates.
     ****************************************************************************/
    private int thresh(int qmean, int nmean) {
        int thrsh;
        int dmed;

        dmed = qmean - nmean;
        dmed = (int) (dmed * TH_NUMERATOR / TH_DENOMINATOR);
        thrsh = nmean + dmed;
        return (thrsh);
    }

    /***********************************************************************
    baselineShiftCheck() reviews data to see if a baseline shift has occurred.
    This is done by looking for both positive and negative slopes of
    roughly the same magnitude in a 220 ms window.
     ***********************************************************************/
    private int baselineShiftCheck(int[] dBuf, int dbPtr) {
        int max, min, maxt, mint, t, x;
        max = min = maxt = mint = 0;

        for (t = 0; t < MS220; ++t) {
            x = dBuf[dbPtr];
            if (x > max) {
                maxt = t;
                max = x;
            } else if (x < min) {
                mint = t;
                min = x;
            }
            if (++dbPtr == DER_DELAY) {
                dbPtr = 0;
            }
        }

        mMaxDer = max;
        min = -min;

        /* Possible beat if a maximum and minimum pair are found
        where the interval between them is less than 150 ms. */

        if ((max > (min >> 3)) && (min > (max >> 3)) && (Math.abs(maxt - mint) < MS150)) {
            return (0);
        } else {
            return (1);
        }
    }

    /*****************************************************************************
     *  Deriv implement derivative approximation represented by
     *  the difference equation:
     *
     *   y[n] = x[n] - x[n - 10ms]
     *
     *  Filter delay is DERIV_LENGTH/2
     *****************************************************************************/
    private class Deriv {

        int mDerBuff[] = new int[DERIV_LENGTH];
        int mDerI = 0;

        public Deriv() {
        }

        public void init() {
            for (mDerI = 0; mDerI < DERIV_LENGTH; ++mDerI) {
                mDerBuff[mDerI] = 0;
            }
            mDerI = 0;
        }

        public int addSample(int x) {
            int y;

            y = x - mDerBuff[mDerI];
            mDerBuff[mDerI] = x;
            if (++mDerI == DERIV_LENGTH) {
                mDerI = 0;
            }
            return (y);
        }
    }

    /******************************************************************************
     * Syntax:
     *   int QRSFilter(int datum, int init) ;
     * Description:
     *   QRSFilter() takes samples of an ECG signal as input and returns a sample of
     *   a signal that is an estimate of the local energy in the QRS bandwidth.  In
     *   other words, the signal has a lump in it whenever a QRS complex, or QRS
     *   complex like artifact occurs.  The filters were originally designed for data
     *  sampled at 200 samples per second, but they work nearly as well at sample
     *   frequencies from 150 to 250 samples per second.
     *
     *   The filter buffers and static variables are reset if a value other than
     *   0 is passed to QRSFilter through init.
     *******************************************************************************/
    private class QRSFilter {

        LPFilter mLPFilter = new LPFilter();
        HPFilter mHPFilter = new HPFilter();
        MWIntegrator mMWIntegrator = new MWIntegrator();
        Deriv mDeriv = new Deriv();

        public QRSFilter() {
        }

        public void init() {
            mLPFilter.init();      // Initialize filters.
            mHPFilter.init();
            mMWIntegrator.init();
            mDeriv.init();
            addSample(0);

        }

        public int addSample(int datum) {
            int fdatum;
            fdatum = mLPFilter.addSample(datum);       // Low pass filter data.
            fdatum = mHPFilter.addSample(fdatum);      // High pass filter data.
            fdatum = mDeriv.addSample(fdatum);         // Take the derivative.
            fdatum = Math.abs(fdatum);                 // Take the absolute value.
            fdatum = mMWIntegrator.addSample(fdatum);  // Average over an 80 ms window .
            return (fdatum);
        }

        /*************************************************************************
         *  lpfilt() implements the digital filter represented by the difference
         *  equation:
         *
         *   y[n] = 2*y[n-1] - y[n-2] + x[n] - 2*x[t-24 ms] + x[t-48 ms]
         *
         *   Note that the filter delay is (LPBUFFER_LGTH/2)-1
         *
         **************************************************************************/
        private class LPFilter {

            long mY1 = 0;
            long mY2 = 0;
            int mBuffer[] = new int[LPBUFFER_LGTH];
            int mIndex = 0;

            public LPFilter() {
            }

            public void init() {
                for (mIndex = 0; mIndex < LPBUFFER_LGTH; ++mIndex) {
                    mBuffer[mIndex] = 0;
                }
                mY1 = mY2 = 0;
                mIndex = 0;
                addSample(0);
            }

            public int addSample(int datum) {
                long y0;
                int output;
                int halfPtr;

                halfPtr = mIndex - (LPBUFFER_LGTH / 2); // Use halfPtr to index
                if (halfPtr < 0) // to x[n-6].
                {
                    halfPtr += LPBUFFER_LGTH;
                }
                y0 = (mY1 << 1) - mY2 + datum - (mBuffer[halfPtr] << 1) + mBuffer[mIndex];
                mY2 = mY1;
                mY1 = y0;
                output = (int) (y0 / ((LPBUFFER_LGTH * LPBUFFER_LGTH) / 4));
                mBuffer[mIndex] = datum;           // Stick most recent sample into
                if (++mIndex == LPBUFFER_LGTH) // the circular buffer and update
                {
                    mIndex = 0;                 // the buffer pointer.
                }
                return (output);
            }
        }

        /******************************************************************************
         *  hpfilt() implements the high pass filter represented by the following
         *  difference equation:
         *
         *   y[n] = y[n-1] + x[n] - x[n-128 ms]
         *   z[n] = x[n-64 ms] - y[n] ;
         *
         *  Filter delay is (HPBUFFER_LGTH-1)/2
         ******************************************************************************/
        private class HPFilter {

            long mY = 0;
            int mBuffer[] = new int[HPBUFFER_LGTH];
            int mIndex = 0;

            public HPFilter() {
            }

            public void init() {
                for (mIndex = 0; mIndex < HPBUFFER_LGTH; ++mIndex) {
                    mBuffer[mIndex] = 0;
                }
                mIndex = 0;
                mY = 0;
                addSample(0);
            }

            public int addSample(int datum) {
                int z;
                int halfPtr;
                mY += datum - mBuffer[mIndex];
                halfPtr = mIndex - (HPBUFFER_LGTH / 2);
                if (halfPtr < 0) {
                    halfPtr += HPBUFFER_LGTH;
                }
                z = mBuffer[halfPtr] - (int) (mY / HPBUFFER_LGTH);

                mBuffer[mIndex] = datum;
                if (++mIndex == HPBUFFER_LGTH) {
                    mIndex = 0;
                }

                return (z);
            }
        }


        // MWIntegrator implements a moving window integrator.  It averages
        // the signal values over the last WINDOW_WIDTH samples.
        private class MWIntegrator {

            long mSum = 0;
            int mBuffer[] = new int[WINDOW_WIDTH];
            int mIndex = 0;

            public MWIntegrator() {
            }

            public void init() {
                for (mIndex = 0; mIndex < WINDOW_WIDTH; ++mIndex) {
                    mBuffer[mIndex] = 0;
                }
                mSum = 0;
                mIndex = 0;
                addSample(0);
            }

            public int addSample(int datum) {
                int output;

                mSum += datum;
                mSum -= mBuffer[mIndex];
                mBuffer[mIndex] = datum;
                if (++mIndex == WINDOW_WIDTH) {
                    mIndex = 0;
                }
                if ((mSum / WINDOW_WIDTH) > 32000) {
                    output = 32000;
                } else {
                    output = (int) (mSum / WINDOW_WIDTH);
                }
                return (output);
            }
        }
    }
}


