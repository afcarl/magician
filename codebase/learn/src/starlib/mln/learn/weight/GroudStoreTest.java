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
	/** Get all combinations of term value in an atom */
	static void backtrack(List<Integer> term_values, int term_index, Atom atom, GroundStore gs, int clause_id) {
		if (term_index >= atom.terms.size()) {
			// Get atom id to flip
			int atomId = gs.getGroundAtomId(atom.symbol, term_values);
			gs.flipAtom(atom.symbol, atomId);
			
			// Get a list of formulas containing current atom and update ground store accordingly - Somdeb
			List<Integer> formula_list = new ArrayList<Integer>();
			formula_list.add(clause_id);
			gs.update(formula_list);
			
			// Debug printing
			System.out.printf("%s: %.1f\n", term_values.toString(), gs.noOfTrueGroundings(clause_id));
			gs.unflipAtom(atom.symbol, atomId);
			
			// Cache in the count for later use
			return;
		}
		
		for (int val : atom.terms.get(term_index).domain) {
			term_values.add(term_index, val);
			backtrack(term_values, term_index+1, atom, gs, clause_id);
			term_values.remove(term_index);
		}
	}
	
	
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
		
		gs.update();
		
		for (int clause_id = 0; clause_id < mln.clauses.size(); clause_id++) {
			mln.clauses.get(clause_id).print();
			System.out.printf("Clause %d: %.1f\n", clause_id, gs.noOfTrueGroundings(clause_id));
			
			System.out.println("Ground atoms");
			
			// Iterate through all ground atoms
			for (Atom atom : mln.clauses.get(clause_id).atoms) {
				System.out.println(atom.symbol.toString());
				backtrack(new ArrayList<Integer>(atom.terms.size()), 0, atom, gs, clause_id);
				System.out.println();
			}
		}
	}
}
