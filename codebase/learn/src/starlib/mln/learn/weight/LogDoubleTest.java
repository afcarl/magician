package starlib.mln.learn.weight;

import starlib.gm.core.LogDouble;

public class LogDoubleTest {
	public static void main(String[] args) {
		// Simple tests
		LogDouble a = new LogDouble(5d), b = new LogDouble(7d, true);
		System.out.printf("%.2f\n", a.getValue());
		System.out.printf("%.2f\n", b.getValue());
		System.out.println();
		
		
		/** Test with formula weights and Markov blanket probabilities */
		
		// Weights
		LogDouble[] w = new LogDouble[4];
		w[0] = new LogDouble(3d, true);
		w[1] = new LogDouble(1d, true);
		w[2] = new LogDouble(5d, true);
		w[3] = new LogDouble(2d, true);
		
		// Counts
		int [] counts = new int[] {1, 2, 3, 4};

		// Compute the probability
		LogDouble original_prob = new LogDouble(0d, true);
		for (int i = 0; i < 4; i++) {
			original_prob = original_prob.multiply(w[i].power(counts[i]));
		}

		System.out.println("Original prob");
		System.out.printf("%.2f\n", original_prob.getValue());
		System.out.printf("%.2f\n", original_prob.getLogValue());
		System.out.println();
		
		/** Compute the Gradient */
		LogDouble flipped_prob = new LogDouble(39d, true); // counts = {2, 3, 4, 5}
		int original_count = 2;
		int filpped_count = 3;
		
		double result = original_count - ((original_prob.multiply(new LogDouble(original_count * 1.0))
						.add(flipped_prob.multiply(new LogDouble(filpped_count * 1.0))))
						.divide(original_prob.add(flipped_prob))).getValue();
		System.out.println("Gradient");
		System.out.printf("%.10f\n", result);
		
		// Division
		System.out.println("\nAddition Test:");
		
		LogDouble s = new LogDouble(898536d, true); // 898536.8394746337
		LogDouble t = new LogDouble(899457d, true); // 899457.9940869209
		System.out.println(s);
		System.out.println(t);

		System.out.println(t.divide(s).getValue() == Double.POSITIVE_INFINITY);

		double zero_threshold = Math.exp(-100);
		System.out.println(zero_threshold);
		System.out.println(1/zero_threshold);
	}
}
