package welch;

import welch.WelchAccumulator;
import java.lang.Math;

class test {
	public static void main(String[] args){
		int segLength  = 8;
		int segOverlap = 2;
		int frameSize = 3;
		double[] window = new double[segLength];

		for (int i = 0; i < segLength; i++)
			window[i] = 0.54 - 0.46 * (double) Math.cos(2 * Math.PI * i / (segLength - 1));

		WelchAccumulator w = new WelchAccumulator(segLength, segOverlap, frameSize, window);

		double[] data = new double[frameSize];
		double[] result = new double[segLength / 2 + 1];

		for (int i = 0; i < 8; i++) {
			for (int n = 0; n < frameSize; n++)
				data[n] = i + n;
			
			w.addFrame(data);

			if (w.resultAvailable())
				result = w.getResult();
		}

		w.close();		
	}
}
