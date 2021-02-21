import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Queue {
	public static boolean done = false;

	public static BlockingQueue<double[][]> dataQueue = new ArrayBlockingQueue<>(1);
	public static BlockingQueue<double[]> resultQueue = new ArrayBlockingQueue<>(1);

	public static void insertIntoDataQueue(double[][] x) {
		dataQueue.put(x);
	}

	public static double[][] getFromDataQueue() throws InterruptedException {
		return dataQueue.take();
	}

	public static void insertIntoResultQueue(double[] x) throws InterruptedException {
		resultQueue.put(x);
	}

	public static double[] getFromResultQueue() {
		return resultQueue.take();
	}
}
