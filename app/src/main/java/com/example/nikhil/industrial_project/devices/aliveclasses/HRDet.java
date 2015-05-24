// HRDet.java
// Alive Technologies
//
// Calculates heart rate from the inter-beat intervals over a 5 sec window .
// Maintains a running mid mean RR interval to reject outliers.
package com.example.nikhil.industrial_project.devices.aliveclasses;

public class HRDet {
	@SuppressWarnings("unused")
	private static final String TAG = "HRDet";
	public static final int MAX_HR = 300;
	public static final int MIN_HR = 30;
	private static int MEDIAN_WINDOW_LENGTH = 10;
	private static int HR_AVG_WINDOW_SAMPLES = 5 * 300;
	private static int HISTORYBUFFER_LENGTH = 32; // Size must be power of 2 so simple math for calculating history index
	private static int HR_TOLERANCE = 10;  // Allow 10bpm step change in HR

	private int mADCUnit; // ADC units per mV
	private int mADCZero; // ADC zero level
	private int mSampleRate; // ECG sampling rate
	private int mSampleCount = 0, mCurrQRSSample, mPrevQRSSample, mLastHRUpdateSample;

	private double mHeartRate; // Heart Rate measurement calculated over 5sec  window 
	private int mCurrRR; // The last RR interval in samples
	private double mCurrHR; // Current Instantaneous HR
	private double mPrevHR; // Previous HR
	private int mTrimMeanSort[] = new int[HISTORYBUFFER_LENGTH];
	private QrsDet mQrsDet = new QrsDet();

	private int mRRInterval[] = new int[MEDIAN_WINDOW_LENGTH];
	private int mRRIntervalCount;
	private int mRRIndex;

	private int mRRIntervalHistory[] = new int[HISTORYBUFFER_LENGTH];
	private int mQrsSampleHistory[] = new int[HISTORYBUFFER_LENGTH];
	private int mHistoryCount = 0;

	public HRDet() {
		mSampleRate = QrsDet.SAMPLE_RATE; // 300Hz (OESA beat detector assumes 300Hz)
		mADCUnit = 50;  // 50 units per mV
		mADCZero = 128; // 128. (8bit unsigned data, where 128 = 0mV)
		reset();
	}

	public int getLastRR() {
		return (mCurrRR);
	}

	public double getHR() {
		return (mHeartRate);
	}

	public void reset() {
		reset(0);
	}

	public void reset(int sampleCount) {
		// Initialise variables used for heart rate calculation
		mSampleCount = sampleCount;
		if (mSampleCount == 0) {
			mPrevQRSSample = 0;
			mLastHRUpdateSample = 0;
		}

		mHeartRate = 0.;
		mCurrRR = 0;
		mRRIntervalCount = 0;
		mRRIndex = 0;
		mHistoryCount = 0;

		// Reset the beat detector
		mQrsDet.init();

	}

	// Description:
	//   Processes ECG samples to detect beats and to calculate the heart rate.
	// Returns: 
	//   When a QRS complex is detected it returns the delay (in samples),
	//   or zero if no QRS detected, 
	//   or -1 if HR reset due to timeouts etc.
	public int process(int ecgSample) {
		int tmp;
		int delay = 0;

		// Set baseline to 0 and resolution to 5 uV/lsb (200 units/mV)
		tmp = ecgSample - mADCZero;
		tmp *= 200;
		tmp /= mADCUnit;
		ecgSample = tmp;

		if (mSampleCount == 0) {
			mQrsDet.init();
		}
		
		// Pass ECG sample to beat detector
		delay = mQrsDet.process(ecgSample);

		// Beat was detected
		if (delay != 0) {
			mCurrQRSSample = mSampleCount - delay;

			if (mPrevQRSSample != 0) {
				mCurrRR = mCurrQRSSample - mPrevQRSSample;
				mCurrHR = mSampleRate * 60. / mCurrRR;
				
				mRRIntervalCount++;
				mRRInterval[mRRIndex++] = mCurrRR;
				if (mRRIndex >= MEDIAN_WINDOW_LENGTH)
					mRRIndex = 0;

				// Robust RR interval measurement using the trimmed mid mean
				double midMeanRR = trimMean(mRRInterval, Math.min(mRRIntervalCount, MEDIAN_WINDOW_LENGTH), 2);
				
				// Ignore intervals outside HR limits
				if (mCurrHR <= (MAX_HR+0.5) && mCurrHR >= (MIN_HR-0.5)) {
					double midMeanHR = mSampleRate * 60. / midMeanRR;

					
					// Update the current heart rate if change is within tolerance
					if (Math.abs(mCurrHR - mPrevHR) < HR_TOLERANCE && Math.abs(mCurrHR - midMeanHR) < HR_TOLERANCE) {
						mLastHRUpdateSample = mCurrQRSSample;
						mQrsSampleHistory[mHistoryCount % HISTORYBUFFER_LENGTH] = mCurrQRSSample;
						mRRIntervalHistory[mHistoryCount % HISTORYBUFFER_LENGTH] = mCurrRR;
						mHistoryCount++;

						double rrsum = mCurrRR;
						int intervalCount = 1;
						int index = mHistoryCount - 2;
						while (index >= 0 && mQrsSampleHistory[index % HISTORYBUFFER_LENGTH] >= (mCurrQRSSample - HR_AVG_WINDOW_SAMPLES)) {
							rrsum += mRRIntervalHistory[index % HISTORYBUFFER_LENGTH];
							intervalCount++;
							index--;
						}
						mHeartRate = mSampleRate * 60. / (rrsum / intervalCount);
					}
					mPrevHR = mCurrHR;
				}
			}
			mPrevQRSSample = mCurrQRSSample;
		}

		// Set heart rate to zero if no beat detection for 5 seconds
		if (mSampleCount - mPrevQRSSample > 5 * mSampleRate) {
			if (delay == 0 && mHeartRate > 0.05) {
				delay = -1;
			}
			mHeartRate = 0.;
		}
		// If heart rate has not been updated for 8 seconds
		if (mSampleCount - mLastHRUpdateSample > 8 * mSampleRate) {
			if (delay == 0 && mHeartRate > 0.05) {
				delay = -1;
			}
			mHeartRate = 0.;
		}
		mSampleCount++;
		return delay;
	}

	// trimMean: Returns a trim mean of an array of int's.
	// It uses a slow sort algorithm, but these arrays are small, so it hardly matters.
	// len: length of array
	// meanCount: Mid number of values in the array to average.
	double trimMean(int array[], int len, int meanCount) {
		int i, j, k;
		int temp;
		int index;

		// Copy to temp array so we don't change the order of original array
		for (i = 0; i < len; ++i) {
			mTrimMeanSort[i] = array[i];
		}
		
		// Sort temp array
		for (i = 0; i < len; ++i) {
			temp = mTrimMeanSort[i];
			for (j = 0; (temp < mTrimMeanSort[j]) && (j < i); ++j)
				;
			for (k = i - 1; k >= j; --k) {
				mTrimMeanSort[k + 1] = mTrimMeanSort[k];
			}
			mTrimMeanSort[j] = temp;
		}
		if (meanCount > len)
			meanCount = len;

		// Trim same number from top and bottom of sorted list
		index = (int) ((len - meanCount) >> 1);

		// Calculate trimmean.
		double sum = 0;
		for (j = 0; j < meanCount; j++)
			sum += mTrimMeanSort[index + j];

		return sum / meanCount;
	}
}

