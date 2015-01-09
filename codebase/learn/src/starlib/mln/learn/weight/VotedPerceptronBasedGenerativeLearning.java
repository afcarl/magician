/*
 * 	VotedPerceptronBasedGenerativeLearning.java
 * 	Generative weight learning using Pseudo-log-likelihood
 *
 *  Created on: Jan 8, 2015
 *      Author: Tuan Anh Pham
 *      Email: tuanh118@gmail.com
 *      The University of Texas at Dallas
 *      All rights reserved.
 */

package starlib.mln.learn.weight;

import java.util.Hashtable;

public class VotedPerceptronBasedGenerativeLearning {
	// Store counts of true groundings in a Hash Table for synchronized access
	Hashtable<String, Integer> true_grouding_count = new Hashtable<String, Integer>();
	
	/** Return the unnormalized probability associated with the original world */
	private double unnormalizedOriginalProb(World world, GroundAtom ga) {
		double exp = 0;
		for f : ga.formula_list
			exp += true_grouding_count.get(f.toString()) * f.getWeight();
		return Math.exp(exp);
	}
	
	/** Return the unnormalized probability associated with the world with 
	 * 	the given ground atom flipped */
	private double unnormalizedFlippedProb(World world, GroundAtom ga) {
		double exp = 0;
		for f : ga.formula_list
			exp += true_grouding_count.get(f.toString() + ga.toString()) * f.getWeight();
		return Math.exp(exp);
	}
	
	/** Compute and cache all the required true grounding counts with each ground atom flipped */
	private void computeCounts(World world) {
		for f : world.formulas {
			true_grouding_count.put(f.toString(), noOfTrueGroundings(f));
			for ga : f
				true_grouding_count.put(f.toString() + ga.toString(), noOfTrueGroundings(f/ga, flipped(ga)));
		}
	}
	
	/** Learn weights for formulas based on given world (database file) */
	public void learnWeights(World world) {
		while (true) {
			for f : world.formulas {
				double delta = 0;
				int original_count = true_grouding_count.get(f.toString());
				for ga : f.groundAtoms {
					int flipped_count = true_grouding_count.get(f.toString() + ga.toString());
					double prob1 = unnormalizedOriginalProb(world, ga);
					double prob2 = unnormalizedFlippedProb(world, ga);
					delta += original_count - (original_count * prob1 + flipped_count * prob2) / (prob1 + prob2);
				}
				f.weight += delta;
			}
		}
	}
}
