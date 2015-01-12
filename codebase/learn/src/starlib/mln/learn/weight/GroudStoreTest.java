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
	static void backtrackCall(List<Integer> a, int index, int n) {
		if (index >= n) {
			System.out.println(a.toString());
			return;
		}
		
		for (int val = 0; val < 2; val++) {
			a.add(index, val);
			backtrackCall(a, index+1, n);
			a.remove(index);
		}
	}
	
	static void backtrackTest(int n) {
		List<Integer> a = new ArrayList<Integer>(n);
		backtrackCall(a, 0, n);
	}

	/** Get all combinations of term value in an atom */
	static void backtrack(List<Integer> term_values, int term_index, List<Term> terms) {
		if (term_index >= terms.size()) {
			System.out.println(term_values.toString());
			return;
		}
		
		for (int val : terms.get(term_index).domain) {
			term_values.add(term_index, val);
			backtrack(term_values, term_index+1, terms);
			term_values.remove(term_index);
		}
	}
	
	
	public static void main(String[] args) throws FileNotFoundException {
		// Backtrack
//		backtrackTest(3);
		
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
			
			System.out.println("Domains");
			
			for (Atom atom : mln.clauses.get(clause_id).atoms) {
				backtrack(new ArrayList<Integer>(atom.terms.size()), 0, atom.terms);
			}
			
			Atom atom = mln.clauses.get(clause_id).atoms.get(0);
			
			List<Integer> termConstants = new ArrayList<Integer>();
			termConstants.add(0);
			termConstants.add(0);
			
			int atomId = gs.getGroundAtomId(atom.symbol, termConstants);
			
			gs.flipAtom(atom.symbol, atomId);
			List<Integer> formula_list = new ArrayList<Integer>();
			formula_list.add(clause_id);
			gs.update(formula_list);
			System.out.printf("Flipped: %.1f\n", gs.noOfTrueGroundings(clause_id));
			gs.unflipAtom(atom.symbol, atomId);
			
			System.out.println();
		}
		
		// Flip a ground atom then count
		List<Integer> termConstants = new ArrayList<Integer>();
		termConstants.add(0);
		termConstants.add(0);

//		// Get the predicate symbol
//		PredicateSymbol symbol = ...;
//
//		// Get atomId
//		int atomId = gs.getGroundAtomId(symbol, termConstatnts);
//		
//		// Use atomId to flip atoms etc.
//		gs.flipAtom(symbol, atomId);
//		gs.update(symbol);
	}
}
