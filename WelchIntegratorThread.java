package welch;

import welch.Queue;

public class WelchIntegratorThread extends Thread{
	private int segLength;
	
	private double[] PSD;
	
	private double result;
	
	public WelchIntegratorThread(int segLength) {
		this.segLength = segLength;
	}
	
	private boolean setPSDFromQueue() {
		if (Queue.done)
			return false;
		
		try {
			double[] PSD = Queue.getFromPSDQueue();
			
			if (PSD == null) return false;
			
			this.PSD = PSD;
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
		//TODO: Compute Noise Floor
	}
	
	public void run() {
		while (!Queue.done) {
			if (setPSDFromQueue())
			{
				computeNoiseFloor();
				
				addResultToQueue();
			}
		}
	}
}
