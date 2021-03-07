package welch;

import welch.WelchAccumulator;
import java.lang.Math;

class test {
	public static void main(String[] args){
		int segLength  = 128;
		int segOverlap = 64;
		int frameSize = 100;

		WelchAccumulator w = new WelchAccumulator(segLength, segOverlap, frameSize, null);

		double[] data = new double[frameSize];
		double result;

		for (int i = 0; i < 2048; i++) {
			for (int n = 0; n < frameSize; n++)
				data[n] = Math.random() * 30;
			
			w.addFrame(data);
			
			if (w.resultAvailable()) {
				result = w.getResult();
				System.out.println("Result: " + result);
			}
		}
		w.close();
	}
}
