package starlib.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ParallelUtil {

	private ExecutorService pool;
	private static int cores;
	private static ParallelUtil _instance;
	private static Random random = new Random();

	// Parameters
	private static boolean IS_PARALLEL_ALLOWED = false;
	private static int MINIMUM_SIZE_FOR_PARALLEL = 200000;

	private ParallelUtil() {
		cores = Runtime.getRuntime().availableProcessors();
		pool = Executors.newFixedThreadPool(cores);
		// TODO: Read from some parameter configuration
	}

	public static void init() {
		if(_instance == null) {
			_instance = new ParallelUtil();
		}
	}

	static class ArrayAdder implements Callable<Double> {

		int start;
		double[] array;
		int length;

		public ArrayAdder(double[] array, int start, int length) {
			this.start = start;
			this.array = array;
			this.length = length;
		}

		@Override
		public Double call() throws Exception {
			return sum(array, start, length);
		}

	};

	private static double sum(double[] array, int start, int length) {
		double sum = 0;
		int end = start + length;
		for (int i = start; i < end ; i++) {
			sum += array[i];
		}

		return sum;
	}

	public static double sum(double[] array) {
		if(!IS_PARALLEL_ALLOWED || array.length < MINIMUM_SIZE_FOR_PARALLEL) {
			return sum(array, 0, array.length);
		} else {
			init();

			int length = (int) Math.ceil(array.length/(double) cores);
			Set<Future<Double>> set = new HashSet<Future<Double>>(cores);

			for (int i = 0; i < cores; i++) {
				int start = i*length;
				if(start + length >= array.length) {
					length = array.length - start;
				}
				Callable<Double> callable = new ArrayAdder(array, start, length);
				Future<Double> future = _instance.pool.submit(callable);
				set.add(future);
			}

			try {
				int sum = 0;
				for (Future<Double> future : set) {
					sum += future.get();
				}			
				return sum;
			} catch (InterruptedException | ExecutionException e) {
				return sum(array, 0, array.length);
			}
		}
	}

	private static int sample(double[] probTable, double norm_const, double rand_num, int start, int length) {
		double cdf = 0;
		int end = start + length;
		for (int i = start; i < end && !Thread.currentThread().isInterrupted() ; i++) {
			cdf += probTable[i]/norm_const;
			if (rand_num <= cdf){
				return i;
			}
		}

		return -1;
	}

	static class ArraySampler implements Callable<Integer> {

		int start;
		double[] probTable;
		int length;
		double norm_const;
		double rand_num;

		public ArraySampler(double[] probTable, double norm_const, double rand_num, int start, int length) {
			this.start = start;
			this.probTable = probTable;
			this.length = length;
			this.norm_const = norm_const;
			this.rand_num = rand_num;
		}

		@Override
		public Integer call() throws Exception {
			return sample(probTable, norm_const, rand_num, 0, probTable.length);
		}

	};

	public static int sample(double[] array, double norm_const) {
		double rand_num = random.nextDouble();

		if(!IS_PARALLEL_ALLOWED || array.length <= Integer.MAX_VALUE) {
			return sample(array, norm_const, rand_num, 0, array.length);
		} else {
			init();

			int length = (int) Math.ceil(array.length/(double) cores);
			double expectedNormConst = norm_const/cores;
			List<Future<Integer>> futures = new ArrayList<>(cores);

			for (int i = 0; i < cores; i++) {
				int start = i*length;
				if(start + length >= array.length) {
					length = array.length - start;
				}
				Callable<Integer> callable = new ArraySampler(array, expectedNormConst, rand_num, start, length);
				Future<Integer> future = _instance.pool.submit(callable);
				futures.add(future);
			}

			try {
				int address = -1;
				int futureId = 0;
				for (Future<Integer> future : futures) {
					address = future.get();
					futureId++;
					if(address > -1)
						break; // One sample found
				}
				
				// Cancel all others
				for (; futureId < futures.size(); futureId++) {
					futures.get(futureId).cancel(true);
				}
				
				return address;
			} catch (InterruptedException | ExecutionException e) {
				return sample(array, norm_const, rand_num, 0, array.length);
			}
		}
	}

	private static int max(double[] array, int start, int length) {
		double maxValue = Double.MIN_VALUE;
		int maxIndex = 0;
		int end = start + length;
		for (int i = start; i < end ; i++) {
			if(array[i] > maxValue) {
				maxValue = array[i];
				maxIndex = i;
			}
		}

		return maxIndex;
	}

	static class ArrayMaxFinder implements Callable<Integer> {

		int start;
		double[] array;
		int length;

		public ArrayMaxFinder(double[] array, int start, int length) {
			this.start = start;
			this.array = array;
			this.length = length;
		}

		@Override
		public Integer call() throws Exception {
			return max(array, start, length);
		}

	};

	public static int max(double[] array) {
		if(!IS_PARALLEL_ALLOWED || array.length < MINIMUM_SIZE_FOR_PARALLEL) {
			return max(array, 0, array.length);
		} else {
			init();

			int length = (int) Math.ceil(array.length/(double) cores);
			Set<Future<Integer>> set = new HashSet<Future<Integer>>(cores);

			for (int i = 0; i < cores; i++) {
				int start = i*length;
				if(start + length >= array.length) {
					length = array.length - start;
				}
				Callable<Integer> callable = new ArrayMaxFinder(array, start, length);
				Future<Integer> future = _instance.pool.submit(callable);
				set.add(future);
			}

			try {
				double maxValue = Double.MIN_VALUE;
				int maxIndex = 0;
				for (Future<Integer> future : set) {
					int index = future.get();
					if(array[index] > maxValue) {
						maxValue = array[index];
						maxIndex = index;
					}
				}			
				return maxIndex;
			} catch (InterruptedException | ExecutionException e) {
				return max(array, 0, array.length);
			}
		}
	}

}
