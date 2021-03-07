package welch;

import welch.Queue;

public class WelchIntegratorThread extends Thread{
	private static final double NOISE_EST_PERCENT = 0.2;  // The percent of the periodogram (on the end) to use for the noise estimation.
	private static final int SAMPLE_RATE = 1970; // The sample rate in Hz
	private static final double SCALE_FACTOR = 225.438;
	
	private double[] periodogram;
	private int periodogramLength;
	
	private double result;
	
	public WelchIntegratorThread(int periodogramLength) {
		this.periodogramLength = periodogramLength;
	}
	
	private boolean setPeriodogramFromQueue() {
		if (Queue.done)
			return false;
		
		try {
			double[] PSD = Queue.getFromPeriodogramQueue();
			
			if (PSD == null) return false;
			
			this.periodogram = PSD;
		} catch (InterruptedException e) {
			Queue.done = true;
			return false;
		}
		
		return true;
	}
	
	private boolean addResultToQueue() {
		if (Queue.done)
			return false;
		
		try {
			return Queue.insertIntoResultQueue(Double.valueOf(this.result));						
		} catch (InterruptedException e) {
			Queue.done = true;
			return false;
		}
	}
	
	private void computeNoiseFloor() {
		int noiseEstimateLength = (int) (this.periodogramLength * WelchIntegratorThread.NOISE_EST_PERCENT);
		double noiseFloor = 0;
		if (noiseEstimateLength >= 1) {
			for (int i = this.periodogramLength - 1; i >= this.periodogramLength - noiseEstimateLength; i--) {
				noiseFloor += this.periodogram[i];
			}
			noiseFloor /= noiseEstimateLength;
		}else {
			noiseFloor = this.periodogram[this.periodogramLength];
		}
		this.result = WelchIntegratorThread.SCALE_FACTOR * noiseFloor * WelchIntegratorThread.SAMPLE_RATE / 2;
	}
	
	public void run() {
		while (!Queue.done) {
			if (setPeriodogramFromQueue())
			{
				computeNoiseFloor();
				
				addResultToQueue();
			}
		}
	}
}

