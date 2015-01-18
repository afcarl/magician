/*
 * 	FormulaCounts.java
 * 	Support class that contains numbers of true groundings of a formula,
 *  including the number of true groundings of the original formula and
 *  a number of true groundings of the formula with each ground atom flipped.
 *
 *  Created on: Jan 17, 2015
 *      Author: Tuan Anh Pham
 *      Email: tuanh118@gmail.com
 *      The University of Texas at Dallas
 *      All rights reserved.
 */

package starlib.mln.learn.weight;

import starlib.mln.core.Atom;
import starlib.mln.core.WClause;

public class FormulaCounts {
	private WClause formula;
	private double original_count;
	private double[] flipped_counts;
	
	// Use for indexing for different atoms in the formula
	private int[] start_index;

	/** Constructor with a formula */
	public FormulaCounts(WClause formula) {
		this.formula = formula;
		
		// Compute values for indexing
		int atom_count = formula.atoms.size();
		start_index = new int[atom_count];
		start_index[0] = 0;
		
		for (int i = 1; i < atom_count; i++) {
			start_index[i] = start_index[i-1] + formula.atoms.get(i-1).getNumberOfGroundings(); 
		}
		
		// Initialize number of true groundings for flipped formula
		flipped_counts = new double[start_index[atom_count-1] + formula.atoms.get(atom_count-1).getNumberOfGroundings()];
	}
	
	public double getOriginalCount() {
		return original_count;
	}
	
	public void setOriginalCount(double original_count) {
		this.original_count = original_count;
	}
	
	/** Get the number of true groundings given the atom id RELATIVE to
	 *  the current formula and the flipped grounding id of that atom */
	public double getFlippedCount(int atom_id, int ground_id) {
		return flipped_counts[start_index[atom_id] + ground_id];
	}
	
	/** Get the number of true groundings given the atom pointer and the
	 *  flipped grounding id of that atom. 
	 *  Requires looking for atom in formula */
	public double getFlippedCount(Atom atom, int ground_id) {
		int atom_id = -1;
		
		// Look for the given atom in the current formula
		for (int i = 0; i < formula.atoms.size(); i++)
			if (formula.atoms.get(i).symbol.parentId == atom.symbol.parentId) {
				atom_id = i;
				break;
			}
		
		return getFlippedCount(atom_id, ground_id);
	}

	/** Set the number of true groundings given the atom id RELATIVE to
	 *  the current formula and the flipped grounding id of that atom */
	public void setFlippedCount(int atom_id, int ground_id, double flipped_count) {
		flipped_counts[start_index[atom_id] + ground_id] = flipped_count;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer("Original count: " + original_count + "\nFlipped counts: ");
		
		for (int i = 0; i < flipped_counts.length; i++) {
			buffer.append(flipped_counts[i]);
			buffer.append(", ");
		}
		
		buffer.append("\n");
		
		return buffer.toString();
	}
}
