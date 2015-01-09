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
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import starlib.gm.core.LogDouble;
import starlib.mln.core.Atom;
import starlib.mln.core.MLN;
import starlib.mln.core.WClause;
import starlib.mln.util.Parser;

public class GenerativeLearningTest {
	// Store counts of true groundings in a Hash Table for synchronized access
	static Hashtable<String, Integer> true_grouding_count = new Hashtable<String, Integer>();

	/** Return the unnormalized probability associated with the original world */
	private double unnormalizedOriginalProb(MLN mln, Atom atom) {
		double exponent = 0;
		
		// Create a list of formula containing current atom
		List<WClause> formula_list = new ArrayList<WClause>();
		
		for (WClause formula : mln.clauses) {
			for (Atom atom_flag : formula.atoms) {
				if (atom_flag.symbol.parentId == atom.symbol.parentId) {
					formula_list.add(formula);
					break;
				}								
			}
		}
		
		// Loop through the list
		for (WClause formula : formula_list)
			exponent += true_grouding_count.get(formula.toString()) * formula.weight;
		return Math.exp(exponent);
	}
	
	/** Compute and cache all the required true grounding counts with each ground atom flipped */
	private void computeCounts(MLN mln) {
		for (WClause formula : mln.clauses) {
			true_grouding_count.put(formula.toString(), noOfTrueGroundings(formula));
			for (Atom atom : formula.atoms)
				true_grouding_count.put(f.toString() + ga.toString(), noOfTrueGroundings(f/ga, flipped(ga)));
		}
	}
	
	/** Learn weights for formulas based on given world (database file) */
	public static void learnWeights(MLN mln) {
//		while (true) {
			for (WClause formula : mln.clauses) {
				double delta = 0;
//				int original_count = true_grouding_count.get(formula.toString());
				
				for (Atom a : formula.atoms) {
				}
				
//				for ga : formula.groundAtoms {
//					int flipped_count = true_grouding_count.get(f.toString() + ga.toString());
//					double prob1 = unnormalizedOriginalProb(world, ga);
//					double prob2 = unnormalizedFlippedProb(world, ga);
//					delta += original_count - (original_count * prob1 + flipped_count * prob2) / (prob1 + prob2);
//				}
//				formula.weight += delta;
			}
//		}
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		/** Parse the MLN file and the DB file into an MLN object*/
		String mlnFile = "love_mln.txt";
		String dbFile = "love_mln_db.txt";
		
		MLN mln = new MLN();
		Parser parser = new Parser(mln);
		parser.parseInputMLNFile(mlnFile);
		parser.parseDbFile(dbFile);
		
		// LogDouble test
		LogDouble a = new LogDouble(5d, true);
		LogDouble b = new LogDouble(3d, true);
		System.out.println(a.add(b));
		
		// Printing out formulas
		System.out.println();
		System.out.println("Formulas:");
		for (WClause wc : mln.clauses) {
			wc.print();
			System.out.println(wc.weight);
			for (Atom atom : wc.atoms) {
				System.out.println(atom.symbol);
				// TODO: add WClause to formula_list while parsing
				System.out.println(atom.formula_list.size());
			}
			System.out.println();
		}
		
		// Run learnWeights
		System.out.println();
		System.out.println("Learn Weights");
		learnWeights(mln);
	}
}
