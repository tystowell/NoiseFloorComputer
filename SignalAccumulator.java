package welch;

import welch.NoiseComputer;
import welch.Queue;
import java.nio.ByteBuffer;

/**
 * The SignalAccumulator class gathers frames of data describing a
 * series of signals coming in real time. Every segLength - segOverlap
 * frames, it uses the last segLength data values and pushes them to a
 * queue. 
 * 
 * It also creates a thread of the NoiseComputer class, which pulls
 * data from that queue and computes the average noise floor of all the
 * signals. The NoiseComputer class is implemented as a seperate thread
 * to make sure that SignalAccumulator doesn't spend a significant amount
 * of time doing calculations and miss the next data value, since they
 * have to come in real time.
 * 
 * Finally, SignalAccumulator provides a couple of functions to receive
 * results from a queue that NoiseComputer is pushing into.
 * 
 * SignalAccumulator has the following parameters:
 * 
 * 1) segLength:
 * The amount of frames to use for a noise floor calculation. A larger
 * value leads to more accurate results, but returns less of them.
 * 
 * 2) segOverlap:
 * The amount by which consecutive segments should overlap. Increasing this
 * value can lead to more results without sacrificing accuracy. Must be
 * smaller than segLength.
 * 
 * 3) frameSize:
 * The number of signals it is working on. Also the number of data points it
 * expects to receive in a single frame.
 * 
 * 4) window:
 * A windowing function to use while computing the fourier transform on a segment.
 * Must be a double array of length segLength. If window is NULL, it will default
 * to a hamming window, a window that is efficient at minimizing spectral leakage.
 * 
 * 5) noiseScale:
 * Just scales the noiseFloor result before returning it.
 * 
 * @author Tyler Stowell
 */

class SignalAccumulator {
	private int segLength;
	private int segOverlap;
	private int frameSize;

	private double[][] data;
	private int position;
	private int remaining;

	private double result;
	
	private double[] window;
	private double noiseScale;

	private NoiseComputer ct;

	public SignalAccumulator(int segLength, int segOverlap, int frameSize, double[] window, double noiseScale) throws IllegalArgumentException{
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
		
		this.result = 0;
		
		if (window == null){
	    	this.window = new double[segLength];
	    	for (int i = 0; i < segLength; i++)
	    		this.window[i] = 0.54 - 0.46 * (double) Math.cos(2 * Math.PI * i / (segLength - 1)); // If null, use the hamming window
	    } else if (window.length != segLength){
	    	this.window = new double[segLength];
	    	for (int i = 0; i < segLength; i++)
	    		this.window[i] = 1; // If window is the incorrect length, don't use a window
	    } else {
	    	this.window = window;
	    }
		
		this.noiseScale = noiseScale;
		ct = null;
	}

	/**
	 * Adds the data in this.data to the queue.
	 * 
	 * @return true on success, false otherwise
	 */
	private boolean addDataToQueue() {
		if (Queue.done || ct == null)
			return false;
		
		double[][] orderedData = new double[this.frameSize][this.segLength];
		for (int i = 0; i < this.frameSize; i++)
			for (int j = 0; j < this.segLength; j++)
				orderedData[i][j] = this.data[i][(this.position + j) % this.segLength];
		
		try {
			return Queue.insertIntoDataQueue(orderedData);
		} catch(InterruptedException e) {
			Queue.done = true;
			return false;
		}
	}

	/**
	 * Gets the data from the result queue and puts it in this.result.
	 * 
	 * @return true on success, false otherwise
	 */
	private boolean setResultFromQueue() {
		if (Queue.done || ct == null)
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

	/**
	 * Adds a frame to this.data. If needed, adds this.data to the queue.
	 * 
	 * @param frame the frame to add as a ByteBuffer
	 * @return -1 on failure, 0 on success, 1 on success if data was also added to queue
	 */
	public int addFrame(ByteBuffer frame) {
		frame.flip();
		
		if (frame.remaining() != 8 * this.frameSize)
			return -1;
		
		for (int i = 0; i < this.frameSize; i++)
			this.data[i][this.position] = frame.getDouble();

		this.position = (this.position + 1) % this.segLength;
		this.remaining -= 1;
		
		if (this.remaining <= 0) {
			if (!addDataToQueue())
				return -1;
			this.remaining += this.segLength - this.segOverlap;
			return 1;
		}

		return 0;
	}
	
	/**
	 * Determines if a new result is available
	 * 
	 * @return true if a result is available, false otherwise
	 */
	public boolean resultAvailable() {
		return !Queue.resultQueueEmpty();
	}
	
	/**
	 * If the queue has new data, gets that data. Then, return the last
	 * received result.
	 * 
	 * @return the most recent noise floor result as a double.
	 */
	public double getResult() {
		if (!Queue.resultQueueEmpty())
			setResultFromQueue();
		
		return this.result;
	}
	
	/**
	 * Any configuration needed to make sure queues are set up and the NoiseComputer thread
	 * is running.
	 */
	public void start() {
		Queue.done = false;
		Queue.emptyAllQueues();
		
		if (ct == null)
		{
			ct = new NoiseComputer(this.segLength, this.frameSize, this.window, this.noiseScale);
			ct.start();
		}
	}
	
	/**
	 * Making sure the queue is closed and the NoiseComputer thread stops.
	 */
	public void stop() {
		Queue.done = true;
		Queue.emptyAllQueues();
		
		if (ct != null)
		{
			ct.interrupt();
			ct = null;
		}
	}
}
