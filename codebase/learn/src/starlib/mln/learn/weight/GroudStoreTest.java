package starlib.mln.learn.weight;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import starlib.mln.core.Atom;
import starlib.mln.core.MLN;
import starlib.mln.core.Term;
import starlib.mln.store.GroundStore;
import starlib.mln.store.GroundStoreFactory;

public class GroudStoreTest {
	public static void main(String[] args) throws FileNotFoundException {
		// Input files
		String mln_file = "test.mln";
		String db_file = "test.db";

//		String mln_file = "love_mln.txt";
//		String db_file = "love_mln_db.txt";
		
		// GroundStore creation
		GroundStore gs = GroundStoreFactory.createGraphModBasedGroundStore(mln_file, db_file);
		gs.update();
		
		// Count # true groundings
		System.out.println("Numbers of true groundings");
		MLN mln = gs.getMln();
		
		for (int clause_id = 0; clause_id < mln.getClauses().size(); clause_id++) {
			mln.getClauses().get(clause_id).print();
			System.out.printf("Clause %d's original count: %.1f\n", clause_id, gs.noOfTrueGroundings(clause_id));
			
			System.out.println("Ground atoms");
			
			// Iterate through all atoms
			for (Atom atom : mln.getClause(clause_id).atoms) {
				System.out.println(atom.symbol.toString());
				
				// Iterate through all ground values of current atom
				for (int ground_atom_id = 0; ground_atom_id < atom.getNumberOfGroundings(); ground_atom_id++) {
					gs.flipAtom(atom.symbol, ground_atom_id);
					
					// Get a list of formulas containing current atom and update ground store accordingly - Somdeb
					List<Integer> formula_list = gs.getMln().getClauseIdsBySymbol(atom.symbol);
					gs.update(formula_list);
					
					// Debug printing
					System.out.printf("Flipped %d: %.1f\n", ground_atom_id, gs.noOfTrueGroundings(clause_id));
					gs.flipAtom(atom.symbol, ground_atom_id);
					gs.update(formula_list);
					
					// Cache in the count for later use
				}
				System.out.println();
			}
		}
	}
}
