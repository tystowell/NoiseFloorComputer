package welch;

import welch.Queue;
import java.lang.Math;
import org.jtransforms.fft.*;

class WelchComputerThread extends Thread {
	private int segLength;
	private int frameSize;
	private double[] window;

	private int resultLength;
	private double scale;

	private double[][] data;

	private DoubleFFT_1D fft;

	public double[] result;

	public WelchComputerThread(int segLength, int frameSize, double[] window) {
		this.segLength = segLength;
		this.frameSize = frameSize;
		
		if (window != null && window.length == segLength) {
			this.window = window;
	    } else {
	    	this.window = new double[segLength];
	    	for (int i = 0; i < segLength; i++)
	    		this.window[i] = 1; // If window is null or incorrect length, don't use a window
	    }

		this.resultLength = (int) (this.segLength / 2 + 1);
		setPeriodogramScale();

		this.data = new double[this.frameSize][this.segLength];

		fft = new DoubleFFT_1D(this.segLength);

		this.result = new double[this.resultLength];
	}

	// Sets the scale for computing the periodogram.
	private void setPeriodogramScale() {
		double sum = 0;

		for (int i = 0; i < segLength; i++)
			sum += Math.pow(this.window[i], 2);

		this.scale = 1.0 / sum;
	}

	private void createSignal(double[] signal, int shift) {
		double mean = 0;

		for (int i = 0; i < this.segLength; i++)
			mean += this.data[shift][i];

		mean /= this.segLength;

		for (int i = 0; i < this.segLength; i++) {
			signal[i] = this.data[shift][i];
			signal[i] -= mean;
			signal[i] *= this.window[i];
		}
	}

	// Converts a real spectrum from JTransforms to a periodogram by:
	// 1) Finding the squared magnitude of each value of the spectrum
	// 2) Scaling (most) values by 2 to conserve the energy
	// 3) Scaling by this.scale
	private void realSpectrumToPeriodogram(double[] spectrum, double[] periodogram) {
		for (int i = 0; i < this.resultLength; i ++) {
			periodogram[i] = 0;
			if (this.segLength % 2 == 1) {
				// If the signal length(n) was odd, spectrum is in the following format:
				// a[2i] = Re[i], 0<=k<(n+1)/2
				// a[2i+1] = Im[i], 0<k<(n-1)/2
				// a[1] = Im[(n-1)/2]

				periodogram[i] += Math.pow(spectrum[2*i], 2); // Re[i]

				if (i == this.resultLength - 1)
					periodogram[i] += Math.pow(spectrum[1], 2); // Im[(n-1)/2]
				else if (i != 0)
					periodogram[i] += Math.pow(spectrum[2*i+1], 2); // Im[i]

				if (i != 0)
					periodogram[i] *= 2; // Preserve energy in signal
			}else{
				// If the signal length(n) was even, spectrum is in the following format:
				// a[2i] = Re[i], 0<=k<n/2
				// a[2i+1] = Im[i], 0<k<n/2
				// a[1] = Re[n/2]

				if (i == this.resultLength - 1)
					periodogram[i] += Math.pow(spectrum[1], 2); // Re[n/2]
				else
					periodogram[i] += Math.pow(spectrum[2*i], 2); // Re[i]

				if (i > 0 && i < this.resultLength - 1) {
					periodogram[i] += Math.pow(spectrum[2*i+1], 2); // Im[i]
					periodogram[i] *= 2; // Preserve energy in signal
				}
			}

			periodogram[i] *= this.scale;
		}
	}

	// Computes the periodogram of the last segment and stores it in this.result
	private void computeSegment() {
		double[] signal = new double[this.segLength];
		double[] periodogram = new double[this.resultLength];

		for (int i = 0; i < this.resultLength; i++)
			this.result[i] = 0;

		for (int i = 0; i < this.frameSize; i++) {
			createSignal(signal, i);

			fft.realForward(signal);
			realSpectrumToPeriodogram(signal, periodogram);

			for (int j = 0; j < this.resultLength; j++)
				this.result[j] += periodogram[j] / this.frameSize;
		}
	}

	public void run() {
		while (!Queue.done) {
			try {
				this.data = Queue.getFromDataQueue();
			} catch (InterruptedException e) {
				break;
			}
			
			computeSegment();
			
			try {
				Queue.insertIntoResultQueue(this.result);
			} catch (InterruptedException e) {
				break;
			}
		}
	}
}
