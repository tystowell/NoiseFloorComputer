package welch;

import welch.WelchAccumulator;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

class test {
	// Reads a little endian binary file of floats into a 2746x2048 array
	// (Written from python with "array.tofile(<path>)")
	public static double[][] readData(String path) {		
		FileInputStream fis;
		try {
			fis = new FileInputStream(path);
		} catch (FileNotFoundException e) {
			return null;
		}
		FileChannel channel = fis.getChannel();
		
		ByteBuffer buffer = ByteBuffer.allocate(22495232);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		int bytesRead;
		try {
			bytesRead = channel.read(buffer);
		} catch (IOException e) {
			return null;
		}
		
		double[][] data = new double[2746][2048];
		int i = 0;
		
		while (bytesRead != -1) {
			buffer.flip();
			
			while (buffer.hasRemaining()) {
				double d = (double) buffer.getFloat();
				
				data[i / 2048][i % 2048] = d;
				i++;
			}
			
			buffer.clear();
			try {
				bytesRead = channel.read(buffer);
			} catch (IOException e) {
				return null;
			}
		}
		
		return data;
	}
	
	public static void main(String[] args){
		String dataPath = "/path/to/binarydata.bin";
		int segLength  = 200;
		int segOverlap = 100;
		int frameSize = 2746;

		WelchAccumulator w = new WelchAccumulator(segLength, segOverlap, frameSize, null);

		double[][] data = readData(dataPath);
		
		if (data == null || data.length != frameSize) {
			System.out.println("Data error!");
			return;
		}
		
		double result;
		double[] frame = new double[frameSize];
		
		for (int i = 0; i < 2048; i++) {
			for (int j = 0; j < frameSize; j++)
				frame[j] = data[j][i];
						
			w.addFrame(frame);
			
			if (w.resultAvailable()) {
				result = w.getResult();
				System.out.println("Result: " + result);
			}
		}
		w.close();
	}
}
