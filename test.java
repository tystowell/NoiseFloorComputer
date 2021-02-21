package welch;

import welch.WelchAccumulator;
import java.lang.Math;

class test {
	public static void main(String[] args){
		int segLength  = 128;
		int segOverlap = 64;
		int frameSize = 2746;
		double[] window = new double[segLength];

		for (int i = 0; i < segLength; i++)
			window[i] = 0.54 + 0.46 * (double) Math.cos(2 * Math.PI * i / (segLength - 1));

		WelchAccumulator w = new WelchAccumulator(segLength, segOverlap, frameSize, window);

		double[] data = new double[frameSize];
		double[] result = new double[segLength / 2 + 1];

		for (int i = 0; i < 512; i++) {
			for (int n = 0; n < frameSize; n++)
				data[n] = Math.random();
			
			w.addFrame(data);
			if (w.resultAvailable())
				result = w.getResult();
		}

		w.close();

		
		System.out.println("Last Result:");
		for (int i = 0; i < segLength / 2 + 1; i++) {
			System.out.println(result[i]);
		}
	}
}
