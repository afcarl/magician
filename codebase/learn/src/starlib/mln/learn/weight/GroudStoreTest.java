package starlib.mln.learn.weight;

import java.io.FileNotFoundException;
import java.util.List;

import starlib.mln.core.Atom;
import starlib.mln.core.MLN;
import starlib.mln.core.WClause;
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
			WClause formula = mln.getClause(clause_id);
			formula.print();
			double original_count = gs.noOfTrueGroundings(clause_id);
			System.out.printf("Clause %d's original count: %.1f\n", clause_id, original_count);
			
			System.out.println("Ground atoms");
			
			// Iterate through all atoms
			for (Atom atom : formula.atoms) {
				System.out.println(atom.symbol.toString());
				
				// Iterate through all ground values of current atom
				
				// First approach (slower): update the ground store every time & count # true groundings directly
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
				
				// 2nd approach: count the number of false groundings increased
				for (int ground_atom_id = 0; ground_atom_id < atom.getNumberOfGroundings(); ground_atom_id++) {
					gs.flipAtom(atom.symbol, ground_atom_id);
					
					System.out.printf(": %.1f\n", original_count - gs.noOfFalseGroundingsIncreased(clause_id));
					
					gs.unflipAtom(atom.symbol, ground_atom_id);
				}
				System.out.println();
			}
		}
	}
}
