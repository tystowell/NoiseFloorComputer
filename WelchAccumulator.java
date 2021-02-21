import java.lang.Math;

class WelchAccumulator {
	private int segLength;
	private int segOverlap;
	private int frameSize;

	private int resultLength;

	private double[][] data;
	private int position;
	private int remaining;

	public double[] result;

	private WelchComputerThread wct;

	public WelchAccumulator(int segLength, int segOverlap, int frameSize, double[] window) {
		this.segLength = segLength;
		this.segOverlap = segOverlap;
		this.frameSize = frameSize;

		this.resultLength = (int) (this.segLength / 2 + 1);

		this.data = new double[this.frameSize][this.segLength];
		this.position = 0;
		this.remaining = this.segLength;

		this.result = new double[this.resultLength];

		wct = new WelchComputerThread(this.segLength, this.frameSize, window);
	}

	public void close() {
		Queue.done = true;
	}

	private void addDataToQueue() {
		double[][] flattenedData = new double[this.frameSize][this.segLength];
		for (int i = 0; i < this.frameSize; i++)
			for (int j = 0; j < this.segLength; j++)
				flattenedData[i][j] = this.data[i][(this.position + j) % this.segLength];

		Queue.insertIntoDataQueue(flattenedData);
	}

	private void getDataFromQueue() {
		this.result = Queue.getFromResultQueue();
	}

	// Takes a frame and adds it to this class's buffer.
	// Calls computeSegment() if needed.
	public int addFrame(double[] frame) {
		if (frame.length != this.frameSize)
			return -1;

		for (int i = 0; i < this.frameSize; i++)
			this.data[this.position][i] = frame[i];

		this.position = (this.position + 1) % this.segLength;
		this.remaining -= 1;

		if (this.remaining <= 0) {
			addDataToQueue();
			this.remaining += this.segLength - this.segOverlap;
		}
	}
}
