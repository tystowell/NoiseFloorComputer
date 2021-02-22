package welch;

import welch.WelchComputerThread;
import welch.Queue;

class WelchAccumulator {
	private int segLength;
	private int segOverlap;
	private int frameSize;

	private int resultLength;

	private double[][] data;
	private int position;
	private int remaining;

	public double[] result;

	private WelchComputerThread ct;

	public WelchAccumulator(int segLength, int segOverlap, int frameSize, double[] window) throws IllegalArgumentException{
		if (segLength <= 1)
			throw new IllegalArgumentException("ERROR: segLength must be greater than 1");

		if (segOverlap >= segLength || segOverlap < 0)
			segOverlap = 0;
		
		if (frameSize <= 0)
			throw new IllegalArgumentException("ERROR: frameSize must be greater than 0");
		
		this.segLength = segLength;
		this.segOverlap = segOverlap;
		this.frameSize = frameSize;

		this.resultLength = (int) (this.segLength / 2 + 1);

		this.data = new double[this.frameSize][this.segLength];
		this.position = 0;
		this.remaining = this.segLength;

		this.result = new double[this.resultLength];

		ct = new WelchComputerThread(this.segLength, this.frameSize, window);
		ct.start();
	}

	public void close() {
		Queue.done = true;
		addDataToQueue(); // Push remaining data to ensure Computer Thread isn't stuck blocking
	}

	private void addDataToQueue() {
		double[][] flattenedData = new double[this.frameSize][this.segLength];
		for (int i = 0; i < this.frameSize; i++)
			for (int j = 0; j < this.segLength; j++)
				flattenedData[i][j] = this.data[i][(this.position + j) % this.segLength];
		
		try {
			Queue.insertIntoDataQueue(flattenedData);
		} catch(InterruptedException e) {
			System.out.println("uh oh");
		}
	}

	private void setResultFromQueue() {
		try {
			this.result = Queue.getFromResultQueue();
		} catch(InterruptedException e) {
			System.out.println("uh oh");
		}
	}

	// Takes a frame and adds it to this class's buffer.
	// Calls computeSegment() if needed.
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
	
	public double[] getResult() {
		if (!resultAvailable()) return null;
		
		setResultFromQueue();
		return this.result;
	}
}
