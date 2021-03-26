package welch;

import welch.SignalAccumulator;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

class test {
	// Reads a little endian binary file of floats into a frameSizexlength array
	// (Written from python with "array.tofile(<path>)")
	public static double[][] readData(String path, int frameSize, int length) {		
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
		
		double[][] data = new double[frameSize][length];
		int i = 0;
		
		while (bytesRead != -1) {
			buffer.flip();
			
			while (buffer.hasRemaining()) {
				double d = (double) buffer.getFloat();
				
				data[i / length][i % length] = d;
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
	
	// Create a frame of data (one new data point for each signal in the frame)
	// as a ByteBuffer.
	public static ByteBuffer createFrame(double[][] data, int frameSize, int frame)
	{
		ByteBuffer result = ByteBuffer.allocate(8 * frameSize);
		for (int i = 0; i < frameSize; i++)
			result.putDouble(data[i][frame]);
		
		return result.asReadOnlyBuffer();
	}
	
	public static void main(String[] args){
		String dataPath = "/path/to/binarydata.bin";
		int segLength  = 100;
		int segOverlap = 50;
		int frameSize = 2746;
		int dataLength = 2048;
		double noiseScale = 4.509888;

		SignalAccumulator w = new SignalAccumulator(segLength, segOverlap, frameSize, null, noiseScale);

		// Starts the accumulator by creating a thread and setting up the queue.
		w.start();

		// Read the data from a file
		double[][] data = readData(dataPath, frameSize, dataLength);
		
		// Check to make sure the read was successful
		if (data == null || data.length != frameSize || data[0].length != dataLength) {
			System.out.println("Data error!");
			return;
		}
		
		for (int i = 0; i < dataLength; i++) {
			// Create a frame
			ByteBuffer frame = createFrame(data, frameSize, i);
			
			// Add the frame
			w.addFrame(frame);
			
			// Check if any results have become available since the last iteration
			if (w.resultAvailable()) {
				double result = w.getResult();
				System.out.println("Result: " + result);
			}
		}
		// Close the accumulator (ensures that the NoiseComputer thread ends)
		w.stop();
	}
}
