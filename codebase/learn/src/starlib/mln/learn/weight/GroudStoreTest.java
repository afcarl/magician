package starlib.mln.learn.weight;

import java.io.FileNotFoundException;

import starlib.mln.core.MLN;
import starlib.mln.core.WClause;
import starlib.mln.store.GroundStore;
import starlib.mln.store.GroundStoreFactory;

public class GroudStoreTest {
	
	
	public static void main(String[] args) throws FileNotFoundException {
		// Input files
//		String mln_file = "test.mln";
//		String db_file = "test.db";

		String mln_file = "love_mln.txt";
		String db_file = "love_mln_db.txt";
		
		// GroundStore creation
		GroundStore gs = GroundStoreFactory.createGraphModBasedGroundStore(mln_file, db_file);
	
		// Count # true groundings
		System.out.println("Numbers of true groundings");
		MLN mln = gs.getMln();
		
		gs.update();
		
		for (int clause_id = 0; clause_id < mln.clauses.size(); clause_id++) {
			mln.clauses.get(clause_id).print();
			System.out.printf("Clause %d: %.1f\n\n", clause_id, gs.noOfTrueGroundings(clause_id));
		}
	}
}
