# NoiseFloorComputer

Computes the noise floor of "frameSize" signals coming from a system in real time.

SignalAccumulator gathers arrays of data with .addFrame(double[]). This array contains the next value for each of "frameSize" signals. When it has enough data to compute the noise floor, it pushes it onto a Queue. Then, NoiseComputer (a helper thread created by SignalAccumulator) computes the periodogram of each signal, averages them, and estimates the asymptote to compute the noise floor. It then pushes this value onto a seperate queue. The point of this seperate thread is to ensure that SignalAccumulator doesn't get stuck waiting for a long calculation to complete and miss values coming in real time. However, when using it, you don't need to know that this thread exists.

To receive this data, use the SignalAccumulator.resultAvailable() method, which tells you if a result is on the receiving queue. If it is, use w.getResult() to receive it. (Calling w.getResult() without a new value on the queue will continute to return the previous value. Failing to call it will leave the value on the queue, meaning that new noise floor calculations will be discarded until the queue is empty again).

SignalAccumulator takes 5 parameters: segLength, segOverlap, frameSize, window, and noiseScale.

1) segLength: The length of the signal that is used to compute the noise floor.
2) segOverlap: The amount that two consecutive segments should overlap by.
3) frameSize: The number of signals in the system
4) window: The window to use for computing the periodogram of a segment. If null, uses the hamming window.
5) noiseScale: A value that the final noise floor computation is scaled by.

So, if you had a system that produced 3 related signals, and you wanted to compute the noise floor using signal lengths of 300 and an overlap of 150 (50%), the following would be the recommended parameters:

SignalAccumulator(300, 150, 3, null, 1);

(null is recommended because the hamming window reduces spectral leakage).

See the main function of test.java for an implementation.
