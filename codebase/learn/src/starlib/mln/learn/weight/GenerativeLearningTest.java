/*
 * 	GenerativeLearningTest.java
 *
 *  Created on: Jan 8, 2015
 *      Author: Tuan Anh Pham
 *      Email: tuanh118@gmail.com
 *      The University of Texas at Dallas
 *      All rights reserved.
 */

package starlib.mln.learn.weight;

import java.io.FileNotFoundException;
import java.util.Hashtable;
import java.util.List;

import starlib.gm.core.LogDouble;
import starlib.mln.core.Atom;
import starlib.mln.core.MLN;
import starlib.mln.core.WClause;
import starlib.mln.store.GroundStore;
import starlib.mln.store.GroundStoreFactory;

public class GenerativeLearningTest {
	// Store counts of true groundings in a Hash Table for synchronized access
	static Hashtable<String, Double> true_grouding_count = new Hashtable<String, Double>();
	static GroundStore gs;

	/** Return the unnormalized probability associated with the original world */
	private static LogDouble unnormalizedOriginalProb(Atom atom) {
		LogDouble exp = new LogDouble(0d, true);
		
		List<WClause> formula_list = gs.getMln().getClausesBySymbol(atom.symbol);

		// Loop through the list
		for (WClause formula : formula_list)
			System.out.printf("%.1f\n", true_grouding_count.get(formula.toString()));
//			exp = exp.multiply(formula.weight.power(true_grouding_count.get(formula.toString())));
		return exp;
	}

	/**
	 * Return the unnormalized probability associated with the world with the
	 * given ground atom flipped
	 */
	private static LogDouble unnormalizedFlippedProb(Atom atom, int ground_atom_id) {
		LogDouble exp = new LogDouble(0d, true);
		List<WClause> formula_list = gs.getMln().getClausesBySymbol(atom.symbol);

		// Loop through the list
		for (WClause formula : formula_list) {
			System.out.printf("%.1f\n", true_grouding_count.get(formula
					.toString() + atom.symbol.toString() + ground_atom_id));
//			exp = exp.multiply(formula.weight.power(true_grouding_count
//					.get(formula.toString() + atom.toString() + ground_atom_id)));
		}
		System.out.println();
		return exp;
	}

	/**
	 * Compute and cache all the required true grounding counts with each ground
	 * atom flipped
	 * @throws FileNotFoundException 
	 */
	private static void computeCounts(String mln_file, String db_file) throws FileNotFoundException {
		// GroundStore creation
		gs = GroundStoreFactory.createGraphModBasedGroundStore(mln_file, db_file);
		gs.update();

		// Count # true groundings
		System.out.println("Numbers of true groundings");
		MLN mln = gs.getMln();

		for (int clause_id = 0; clause_id < mln.getClauses().size(); clause_id++) {
			WClause formula = mln.getClause(clause_id);
			formula.print();
			
			// Compute and cache the count of the original formula
			double original_count = gs.noOfTrueGroundings(clause_id);
			System.out.printf("Clause %d's original count: %.1f\n", clause_id,
					original_count);
			true_grouding_count.put(formula.toString(), original_count);

			System.out.println("Ground atoms");

			// Iterate through all atoms
			for (Atom atom : mln.getClause(clause_id).atoms) {
				System.out.println(atom.symbol.toString());

				// Iterate through all ground values of current atom
				for (int ground_atom_id = 0; ground_atom_id < atom
						.getNumberOfGroundings(); ground_atom_id++) {
					
					// Flip the ground atom, get a list of formulas containing 
					// current atom and update ground store accordingly
					gs.flipAtom(atom.symbol, ground_atom_id);
					List<Integer> formula_list = gs.getMln().getClauseIdsBySymbol(atom.symbol);
					gs.update(formula_list);

					// Compute and cache the count of the formula with the current
					// ground atom flipped
					double flipped_count = gs.noOfTrueGroundings(clause_id);
					System.out.printf("Flipped %d: %.1f\n", ground_atom_id,
							flipped_count);
					true_grouding_count.put(formula.toString() 
							+ atom.symbol.toString() + ground_atom_id, flipped_count);
					
					// Unflip the ground atom
					gs.flipAtom(atom.symbol, ground_atom_id);
					gs.update(formula_list);
				}
				System.out.println();
			}
		}
	}

	/** Learn weights for formulas based on given world (database file) */
	public static void learnWeights() {
		MLN mln = gs.getMln();
		
		// while (true) {
		for (WClause formula : mln.getClauses()) {
			double delta = 0;
			double original_count = true_grouding_count.get(formula.toString());

			for (Atom atom : formula.atoms) {
				System.out.println();
				System.out.println(atom.symbol.toString());
				System.out.println("Original series");
				LogDouble original_prob = unnormalizedOriginalProb(atom);

				for (int ground_atom_id = 0; ground_atom_id < atom
						.getNumberOfGroundings(); ground_atom_id++) {
					double flipped_count = true_grouding_count.get(formula
							.toString() + atom.symbol.toString() + ground_atom_id);

					System.out.printf("Flipped %d series\n", ground_atom_id);
					LogDouble flipped_prob = unnormalizedFlippedProb(atom, ground_atom_id);

//					delta = original_count
//							- ((original_prob.multiply(new LogDouble(
//									original_count * 1.0))
//									.add(flipped_prob.multiply(new LogDouble(
//											flipped_count * 1.0))))
//									.divide(original_prob.add(flipped_prob)))
//									.getValue();
				}
			}

			// formula.weight += delta;
//			formula.weight = new LogDouble(formula.weight.getLogValue() + delta, true);			
		}
		// }
	}

	public static void main(String[] args) throws FileNotFoundException {
		// Parse the MLN file and the DB file into an MLN object
//		String mln_file = "love_mln.txt";
//		String db_file = "love_mln_db.txt";
		String mln_file = "test.mln";
		String db_file = "test.db";

		/** Learning weights */
		
		// Compute all the counts required
		System.out.println("Learn Weights");
		computeCounts(mln_file, db_file);
		learnWeights();
	}
}
