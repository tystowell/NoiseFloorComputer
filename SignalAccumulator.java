package welch;

import welch.NoiseComputer;
import welch.Queue;

class SignalAccumulator {
	private int segLength;
	private int segOverlap;
	private int frameSize;

	private double[][] data;
	private int position;
	private int remaining;

	private double result;

	private NoiseComputer ct;

	public SignalAccumulator(int segLength, int segOverlap, int frameSize, double[] window) throws IllegalArgumentException{
		if (segLength <= 1)
			throw new IllegalArgumentException("ERROR: segLength must be greater than 1");

		if (segOverlap >= segLength || segOverlap < 0)
			segOverlap = 0;
		
		if (frameSize <= 0)
			throw new IllegalArgumentException("ERROR: frameSize must be greater than 0");
		
		this.segLength = segLength;
		this.segOverlap = segOverlap;
		this.frameSize = frameSize;

		this.data = new double[this.frameSize][this.segLength];
		this.position = 0;
		this.remaining = this.segLength;
		
		Queue.emptyAllQueues();

		ct = new NoiseComputer(this.segLength, this.frameSize, window);
		ct.start();
	}

	private boolean addDataToQueue() {
		double[][] flattenedData = new double[this.frameSize][this.segLength];
		for (int i = 0; i < this.frameSize; i++)
			for (int j = 0; j < this.segLength; j++)
				flattenedData[i][j] = this.data[i][(this.position + j) % this.segLength];
		
		if (Queue.done)
			return false;
		
		try {
			return Queue.insertIntoDataQueue(flattenedData);
		} catch(InterruptedException e) {
			Queue.done = true;
			return false;
		}
	}

	private boolean setResultFromQueue() {
		if (Queue.done)
			return false;
		
		try {
			Double result = Queue.getFromResultQueue();
			
			if (result == null) return false;
			
			this.result = result.doubleValue();
		} catch(InterruptedException e) {
			Queue.done = true;
			return false;
		}
		
		return true;
	}

	/* 
	 * Takes a frame and adds it to this class's buffer.
	 * If needed, adds a segment to queue so the ComputerThread can compute the noise floor.
	 */
	public int addFrame(double[] frame) {		
		if (frame.length != this.frameSize)
			return -1;

		for (int i = 0; i < this.frameSize; i++)
			this.data[i][this.position] = frame[i];

		this.position = (this.position + 1) % this.segLength;
		this.remaining -= 1;
		
		if (this.remaining <= 0) {
			addDataToQueue();
			this.remaining += this.segLength - this.segOverlap;
			return 1;
		}

		return 0;
	}
	
	public boolean resultAvailable() {
		return !Queue.resultQueueEmpty();
	}
	
	public double getResult() {
		setResultFromQueue();
		
		return this.result;
	}
	
	public void close() {
		Queue.done = true;
	}
}


