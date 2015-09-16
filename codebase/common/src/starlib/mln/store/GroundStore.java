package starlib.mln.store;

import java.util.List;

import starlib.mln.core.MLN;
import starlib.mln.core.PredicateSymbol;

public interface GroundStore {
	
	public abstract MLN getMln();

	public abstract void init();

	/**
	 * Get the ground atom id of a Predicate given assignment to its terms
	 * 
	 * @param symbol
	 * @param termConstants
	 * @return
	 */
	public int getGroundAtomId(PredicateSymbol symbol, List<Integer> termConstants);
	
	/**
	 * Clones a KB to another KB. For reducing memory foot print only the tables for 
	 * each function is cloned. All the other members are pointed to the old object.
	 */
	public abstract GroundStore clone();

	public abstract void setAssignment(GroundStore anotherKB);

	public abstract void randomAssignment();

	public abstract void randomAssignment(PredicateSymbol symbol);

	public abstract Double noOfTrueGroundings(int clauseId);

	public abstract Double noOfFalseGroundings(int clauseId);

	public abstract Double noOfGroundings(int clauseId);

	public abstract Double noOfFalseGroundingsIncreased(int clauseId);

	public abstract void update();

	public abstract void update(List<Integer> clauseIds);

	public abstract void getRandomGroundClause(int clauseIndex,
			List<Integer> groundClauseToBeReturned);

	public abstract void getRandomUnsatGroundClause(int clauseIndex,
			List<Integer> groundClauseToBeReturned);

	public abstract void flipAtom(PredicateSymbol symbol, int atomId);

	public abstract void unflipAtom(PredicateSymbol symbol, int atomId);

}