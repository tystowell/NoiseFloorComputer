package welch;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class Queue {
	public final static long TIMEOUT = 500;
	
	public static boolean done = false;

	public static BlockingQueue<double[][]> dataQueue = new ArrayBlockingQueue<>(1);
	public static BlockingQueue<Double> resultQueue = new ArrayBlockingQueue<>(1);

	public static boolean insertIntoDataQueue(double[][] x) throws InterruptedException {
		return dataQueue.offer(x, TIMEOUT, TimeUnit.MILLISECONDS);
	}

	public static double[][] getFromDataQueue() throws InterruptedException {
		return dataQueue.poll(TIMEOUT, TimeUnit.MILLISECONDS);
	}
	
	public static boolean dataQueueEmpty() {
		return dataQueue.isEmpty();
	}

	public static boolean insertIntoResultQueue(Double x) throws InterruptedException {
		return resultQueue.offer(x, TIMEOUT, TimeUnit.MILLISECONDS);
	}

	public static Double getFromResultQueue() throws InterruptedException {
		return resultQueue.poll(TIMEOUT, TimeUnit.MILLISECONDS);
	}
	
	public static boolean resultQueueEmpty() {
		return resultQueue.isEmpty();
	}
	
	public static void emptyAllQueues() {
		dataQueue.clear();
		resultQueue.clear();
	}
}
