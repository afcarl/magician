/*
 * 	PseudoLogLikelihoodBasedLearning.java
 * 	Generative weight learning using Pseudo-log-likelihood
 *
 *  Created on: Jan 8, 2015
 *      Author: Tuan Anh Pham
 *      Email: tuanh118@gmail.com
 *      The University of Texas at Dallas
 *      All rights reserved.
 */

package starlib.mln.learn.weight;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import starlib.gm.core.LogDouble;
import starlib.mln.core.Atom;
import starlib.mln.core.MLN;
import starlib.mln.core.PredicateSymbol;
import starlib.mln.core.WClause;
import starlib.mln.store.GroundStore;
import starlib.mln.store.GroundStoreFactory;

public class PseudoLogLikelihoodBasedLearning {
	// Storing counts in an array
	static FormulaCounts[] true_count;
	static GroundStore gs;

	/** Return the unnormalized probability associated with the original world */
	private static LogDouble unnormalizedOriginalProb(Atom atom) {
		LogDouble exp = new LogDouble(0d, true);
		List<Integer> formula_id = gs.getMln().getClauseIdsBySymbol(atom.symbol);

		// Loop through the list
		for (int clause_id : formula_id) {
			exp = exp.multiply(gs.getMln().getClauses().get(clause_id).weight
					.power(true_count[clause_id].getOriginalCount()));
		}
		return exp;
	}

	/**
	 * Return the unnormalized probability associated with the world with the
	 * given ground atom flipped
	 */
	private static LogDouble unnormalizedFlippedProb(Atom atom, int ground_id) {
		LogDouble exp = new LogDouble(0d, true);
		List<Integer> formula_id = gs.getMln().getClauseIdsBySymbol(atom.symbol);

		// Loop through the list
		for (int clause_id : formula_id) {
			exp = exp.multiply(gs.getMln().getClauses().get(clause_id).weight
					.power(true_count[clause_id].getFlippedCount(atom, ground_id)));
		}
		return exp;
	}

	/**
	 * Compute and cache all the required true grounding counts with each ground
	 * atom flipped
	 * @throws FileNotFoundException 
	 */
	private static void computeCounts(String mln_file, String db_file) throws FileNotFoundException {
		// GroundStore creation
		gs = GroundStoreFactory.createGraphModBasedGroundStoreWithApproxCount(mln_file, db_file);
		gs.update();

		// Count # true groundings
		System.out.println("Numbers of true groundings");
		MLN mln = gs.getMln();
		
		// Initialize the true ground store
		true_count = new FormulaCounts[mln.getClauses().size()];
		for (int clause_id = 0; clause_id < mln.getClauses().size(); clause_id++) {
			true_count[clause_id] = new FormulaCounts(mln.getClause(clause_id));
		}

		for (int clause_id = 0; clause_id < mln.getClauses().size(); clause_id++) {
			WClause formula = mln.getClause(clause_id);
			
			formula.print();
			
			// Compute and cache the count of the original formula
			int original_count = gs.noOfTrueGroundings(clause_id).intValue();
			
			// Initialize the true ground store for the current formula
			true_count[clause_id].setOriginalCount(original_count);

			List<PredicateSymbol> visitedSymbols = new ArrayList<>(formula.atoms.size());
			// Iterate through all atoms
			for (int atom_id = 0; atom_id < formula.atoms.size(); atom_id++) {
				Atom atom = formula.atoms.get(atom_id);
				
				if(visitedSymbols.contains(atom.symbol))
					continue;
				
				System.out.println(atom.symbol);
				
				// Iterate through all ground values of current atom
				for (int ground_id = 0; ground_id < atom
						.getNumberOfGroundings(); ground_id++) {

					gs.flipAtom(atom.symbol, ground_id);

					// Compute and cache the count of the formula with the current
					// ground atom flipped
					int flipped_count = original_count - gs.noOfFalseGroundingsIncreased(clause_id).intValue();
					true_count[clause_id].setFlippedCount(atom_id, ground_id, flipped_count);

					gs.unflipAtom(atom.symbol, ground_id);
				}
				
				visitedSymbols.add(atom.symbol);
			}
		}
	}

	/** Learn weights for formulas based on the given world (database file) */
	public static void learnWeights() {
		MLN mln = gs.getMln();
		
		// Parameters
//		int MAX_ITER = 1000;
//		double threshold = 0.005;
		double percent_change_threshold = 0.001;
		double learning_rate = 0.01; // fixed learning rate
		
		// Variables to check for convergence
		boolean converged = false;
		double[] weight_change = new double[mln.getClauses().size()];
		
		int iter = 1;
		
		// Loop until the weights converge or after a preset number of iterations
//		while (!converged && iter < MAX_ITER) {
		while (!converged) {
			System.out.println("Iteration " + iter);
			
//			double learning_rate = 1.0 / iter;
			
			for (int clause_id = 0; clause_id < mln.getClauses().size(); clause_id++) {
				WClause formula = mln.getClause(clause_id);
				double delta = 0;
				
				// True grounding count of the original formula
				double original_count = true_count[clause_id].getOriginalCount();

				for (int atom_id = 0; atom_id < formula.atoms.size(); atom_id++) {
					Atom atom = formula.atoms.get(atom_id);
					LogDouble original_prob = unnormalizedOriginalProb(atom);
	
					for (int ground_id = 0; ground_id < atom
							.getNumberOfGroundings(); ground_id++) {
						
						// True grounding count of the formula with the current ground atom flipped
						double flipped_count = true_count[clause_id].getFlippedCount(atom_id, ground_id);
						LogDouble flipped_prob = unnormalizedFlippedProb(atom, ground_id);
	
						// Match the counts and probabilities to compute the weight change
						delta += original_count
								- ((original_prob.multiply(new LogDouble(
										original_count * 1.0))
										.add(flipped_prob.multiply(new LogDouble(
												flipped_count * 1.0))))
										.divide(original_prob.add(flipped_prob)))
										.getValue();
					}
				}
	
				// Update weight of current formula
				weight_change[clause_id] = learning_rate * delta;
				formula.weight = new LogDouble(formula.weight.getLogValue() + learning_rate * delta, true);
//				System.out.println("Updated weight: " + formula.weight);
			}
			
			iter++;
			
			// Print weights after each iteration
			System.out.println("\nWeights: ");
			for (WClause formula : mln.getClauses()) {
				formula.print();
				System.out.println(formula.weight);
				System.out.println();
			}
			System.out.println();

			// Check for weight convergence
			converged = true;
			for (int clause_id = 0; clause_id < mln.getClauses().size(); clause_id++) {
				if (mln.getClause(clause_id).weight.getLogValue() != 0 &&
					Math.abs(weight_change[clause_id]) / mln.getClause(clause_id).weight.getLogValue()
						> percent_change_threshold) {
					converged = false;
					break;
				}
			}
		}
		
		// Final weights
		System.out.println("\nFinal weights: ");
		for (WClause formula : mln.getClauses()) {
			formula.print();
			System.out.println(formula.weight);
			System.out.println();
		}
	}

	/** Empirical tests */
	public static void main(String[] args) throws FileNotFoundException {
		// Parse the MLN file and the DB file into an MLN object
//		String mln_file = "love_mln.txt";
//		String db_file = "love_mln_db.txt";
		String mln_file = "test.mln";
		String db_file = "test.db";
//		String mln_file = "webkb-magician.mln";
//		String db_file = "webkb-0.txt";
		
//		String mln_file = args[0];
//		String db_file = args[1];
		
		// Time
		long startTime = System.nanoTime();
		
		/** Learning */
		System.out.println("Computing Counts");
		
		// Compute all the counts required
		computeCounts(mln_file, db_file);
		
		long countTime = System.nanoTime();
		System.out.printf("Counting time: %f seconds\n\n", (countTime - startTime) / 1000000000.0);

		System.out.println("Counts computed\n");
		System.out.println("Learning Weights\n");
		
		// Update weights
		learnWeights();
		
		long learnTime = System.nanoTime();
		System.out.printf("Learning time: %fseconds\n", (learnTime - countTime) / 1000000000.0);
	}
}
