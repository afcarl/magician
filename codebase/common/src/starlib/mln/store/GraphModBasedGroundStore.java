package starlib.mln.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import starlib.gm.core.Variable;
import starlib.mln.core.Atom;
import starlib.mln.core.MLN;
import starlib.mln.core.PredicateSymbol;
import starlib.mln.core.Term;
import starlib.mln.core.WClause;
import starlib.mln.store.internal.CompiledStructure;
import starlib.mln.store.internal.GlobalContext;
import starlib.mln.store.internal.IntFunction;
import starlib.mln.store.internal.IntGraphMod;
import starlib.mln.store.internal.jt.JoinTreeInt;

public class GraphModBasedGroundStore implements GroundStore {
	
	protected static final int UNDEFINED = -1;
	
	protected Random random = new Random();
	
	protected MLN mln;
	
	protected IntGraphMod graphicalModel;
	
	protected CompiledStructure[] joinTrees;
	
	protected double[] satCounts;
	
	protected double[] totalCounts;
	
	/**
	 * These variables will be used to modify the scope of the functions
	 */
	protected Variable[][][] clauseWiseFunctionScopes;
	
	// temporary members for flipping/unflipping
	protected int lastFlippedSymbol;
	
	protected int lastFlippedAtomId;
	
	protected boolean selfJoinFlipped = false;
	
	protected List<Integer> selfJoinedClauses;
	
	// Parameters:
	protected final double PARAM_REJECTION_SAMPLING_PROB = 0.4;   
	
	protected final int PARAM_REJECTION_TRY = 3;   
	
	
	public GraphModBasedGroundStore(MLN mln) {
		this.mln = mln;
	}
	
	@Override
	public MLN getMln() {
		return mln;
	}
	
	@Override
	public void init() {
		joinTrees = new CompiledStructure[mln.clauses.size()];
		satCounts = new double[mln.clauses.size()];
		totalCounts = new double[mln.clauses.size()];
		selfJoinedClauses = new ArrayList<>(mln.clauses.size());
		
		graphicalModel = new IntGraphMod();
		
		List<Variable> variables = new ArrayList<>();
		int variableId = 0;
		clauseWiseFunctionScopes = new Variable[mln.clauses.size()][][];

		IntFunction[] functions = new IntFunction[mln.symbols.size()];
		
		int[] symbolProcessed = new int[mln.symbols.size()];
		

		// Create variables and functions
		for (int i=0; i<mln.clauses.size(); i++) {
			WClause clause = mln.clauses.get(i);
			
			for (int j = 0; j < clause.atoms.size(); j++) {
				Atom atom = clause.atoms.get(j);
				
				if(symbolProcessed[atom.symbol.id] == 0) {
					// Symbol is not yet processed
					// Scope of the function
					List<Variable> scope = new ArrayList<>(atom.terms.size());
					
					for (Term term : atom.terms) {
						Variable v = new Variable(variableId, term.domain.size());
						scope.add(v); 
						variables.add(v);
						variableId++;
					}

					// One function per predicate symbol
					IntFunction function = new IntFunction(scope);
					// No need to create the table. Table is automatically created with every entry zero
					functions[atom.symbol.id] = function;
					symbolProcessed[atom.symbol.id] = 1;
					
				} else {
					// Variable for the symbol has already been created so skip
					continue;
				}
			}
		}
		
		// Create clause-wise scope valriables
		for (int i=0; i<mln.clauses.size(); i++) {
			WClause clause = mln.clauses.get(i);
			List<Term> oldTerms = new ArrayList<Term>();
			List<Variable> newVariables = new ArrayList<>();
			int newVariableId = 0;
			
			for (int j = 0; j < clause.atoms.size(); j++) 
			{
				for (int k = 0; k < clause.atoms.get(j).terms.size(); k++)
				{
					int termPosition=-1;
					for(int m=0; m<oldTerms.size(); m++)
					{
						if(oldTerms.get(m)==clause.atoms.get(j).terms.get(k))
						{
							termPosition = m;
						}
					}
					
					if(termPosition==-1)
					{
						// Term not found; create a variable 
						Variable newVariable = new Variable(newVariableId, functions[clause.atoms.get(j).symbol.id].getVariables().get(k).getDomainSize());
						newVariableId++;
						newVariables.add(newVariable);
						oldTerms.add(clause.atoms.get(j).terms.get(k));
					}
					else
					{
						newVariables.add(newVariables.get(termPosition));
						oldTerms.add(clause.atoms.get(j).terms.get(k));
					}
				}
			}
			
			// Populate clause-wise-function-scope
			clauseWiseFunctionScopes[i] = new Variable[clause.atoms.size()][];
			int ind=0;
			for (int j = 0; j < clause.atoms.size(); j++) {
				clauseWiseFunctionScopes[i][j] = new Variable[clause.atoms.get(j).terms.size()];
				for (int k = 0; k < clause.atoms.get(j).terms.size(); k++) {
					clauseWiseFunctionScopes[i][j][k] = newVariables.get(ind);
					ind++;
				}
			}
		}
		
		graphicalModel.setFunctions(Arrays.asList(functions));
		graphicalModel.setVariables(variables);
		
		
		// XXX: Hack:: Violates coupling. I am still gonna use it anyway t save computing same thing over and over
		GlobalContext.init(mln.clauses.size());
		
		this.setUpInternalClauseStore();
		this.incoporateEvidence();
	}
	
	/**
	 * Create clause-wise compiled structure (junction tree/ Bucket tree etc)
	 */
	protected void setUpInternalClauseStore() {
		// Populate Bucket-Trees. Each tree will have separate function			
		for (int i=0; i<mln.clauses.size(); i++) {
			WClause clause = mln.clauses.get(i);
			List<IntFunction> junctionTreeFunctions = new ArrayList<>(clause.atoms.size());
			for (int j = 0; j < clause.atoms.size(); j++) {
				Atom atom = clause.atoms.get(j);

				IntFunction newFunction = graphicalModel.getFunctions().get(atom.symbol.id).sharedCopy();
				// Change the functions scopes
				newFunction.setVariables(Arrays.asList(clauseWiseFunctionScopes[i][j]));
				newFunction.setNegated(!clause.sign.get(j));

				junctionTreeFunctions.add(newFunction);
			}
			CompiledStructure jt = new JoinTreeInt(i, junctionTreeFunctions);
			joinTrees[i] = jt;
			totalCounts[i] = Variable.getDomainSize(jt.getVariables());
		}

	}
	
	/**
	 * Incorporate the already present evidence in the MLN file
	 */
	protected void incoporateEvidence() {
		
		for (int i=0; i<mln.evidence.size(); i++) {
			WClause evidence = mln.evidence.get(i);
			
			// Each evidence is a Unit clause
			Atom evidencAtom = evidence.atoms.get(0);
			List<Integer> termConstatnts = new ArrayList<>(evidencAtom.terms.size());
			for (Term term : evidencAtom.terms) {
				termConstatnts.add(term.domain.get(0)); // Each terms domain is only a constant
			}
			
			PredicateSymbol symbol = evidencAtom.symbol;
			int atomId = this.getGroundAtomId(symbol, termConstatnts);
			
			// A zero Evidence Value in the store represents a positive evidence in the file 
			double evidenceValue = evidence.sign.get(0) ? 1 : 0;
			
			graphicalModel.getFunctions().get(symbol.id).setTableEntry(evidenceValue, atomId);

		}
		
	}
	
	/**
	 * Get the ground atom id of a Predicate given assignment to its terms
	 * 
	 * @param symbol
	 * @param termConstatnts
	 * @return
	 */
	@Override
	public int getGroundAtomId(PredicateSymbol symbol, List<Integer> termConstatnts) {
		IntFunction function = graphicalModel.getFunctions().get(symbol.id);
		return function.getAddress(termConstatnts);
	}

	
	/**
	 * Clones a KB to another KB. For reducing memory foot print only the tables for 
	 * each function is cloned. All the other members are pointed to the old object.
	 */
	@Override
	public GroundStore clone() {
		GraphModBasedGroundStore newKB = new GraphModBasedGroundStore(mln);
		
		newKB.graphicalModel = new IntGraphMod();
		newKB.graphicalModel.setType(graphicalModel.getType());
		newKB.graphicalModel.setVariables(graphicalModel.getVariables());
		newKB.graphicalModel.setEvidence(graphicalModel.getEvidence());

		// For each function create a new table which is cloned from the old table
		// However the variables will point to the old variables (no cloning needed)
		List<IntFunction> functions = new ArrayList<>(graphicalModel.getFunctions().size());
		for (IntFunction function : graphicalModel.getFunctions()) {
			IntFunction newFunction = new IntFunction();
			newFunction.setVariables(function.getVariables());
			newFunction.setTable(Arrays.copyOf(function.getTable(), function.getTable().length));
			
			functions.add(newFunction);
		}
		
		newKB.graphicalModel.setFunctions(functions);
		
		return newKB;
	}
	
	@Override
	public void setAssignment(GroundStore anotherKB) {
		if (anotherKB instanceof GraphModBasedGroundStore) {
			GraphModBasedGroundStore gmBasedKB = (GraphModBasedGroundStore) anotherKB;
			
			for (int i = 0; i < graphicalModel.getFunctions().size(); i++) {
				IntFunction thisFunction =  graphicalModel.getFunctions().get(i);
				IntFunction copyFromFunction =  gmBasedKB.graphicalModel.getFunctions().get(i);
				thisFunction.setTable(Arrays.copyOf(copyFromFunction.getTable(), copyFromFunction.getTable().length));
			}
		}

	}

	@Override
	public void randomAssignment() {
		for (IntFunction function : graphicalModel.getFunctions()) {
			int functionSize = function.getFunctionSize();
			
			for (int j = 0; j < functionSize; j++) {
				if(random.nextBoolean()) {
					function.setTableEntry(1, j);
				} else {
					function.setTableEntry(0, j);
				}
			}
		}
	}
	
	
	@Override
	public Double noOfTrueGroundings(int clauseId) {
		return (double) satCounts[clauseId];
	}

	
	@Override
	public Double noOfFalseGroundings(int clauseId) {
		return (double) (totalCounts[clauseId] - satCounts[clauseId]);
	}

	@Override
	public Double noOfFalseGroundingsIncreased(int clauseId) {
		CompiledStructure jt = joinTrees[clauseId];
		WClause clause = mln.clauses.get(clauseId);
		
		int clauseLength = clause.atoms.size();
		List<Integer> changedFunctionIdList = new ArrayList<>(clauseLength);
		for (int i = 0; i < clauseLength; i++) {
			if(clause.atoms.get(i).symbol.id == lastFlippedSymbol) {
				changedFunctionIdList.add(i);
			}
		}
		
		if(changedFunctionIdList.size() > 1) {
			// Self-Join case
			selfJoinFlipped = true;
			selfJoinedClauses.add(clauseId);

			double oldZ = jt.getZ();
			jt.reCalibrateAll();
			double newZ = jt.getZ();
			
			return newZ - oldZ;
		} else {
			int changedFunctionId = changedFunctionIdList.get(0);
			return jt.computeDeltaZ(changedFunctionId, lastFlippedAtomId);
		}
	}

	@Override
	public void update() {
		for (int i = 0; i < joinTrees.length; i++) {
			CompiledStructure jt = joinTrees[i];
			jt.calibrate();
			satCounts[i] = totalCounts[i] - jt.getZ();
			if(satCounts[i] < 0)
				System.err.println("Sat counts negative!!!");
		}
	}

	@Override
	public void update(List<Integer> clauseIds) {
		for (Integer clauseId : clauseIds) {
			this.update(clauseId);
		}
		// JTs have been re-calibrated now reset the changes
		graphicalModel.getFunctions().get(lastFlippedSymbol).resetChangeEntry(lastFlippedAtomId);
	}
	
	private void update(int clauseId) {
		CompiledStructure jt = joinTrees[clauseId];
		WClause clause = mln.clauses.get(clauseId);
		
		int clauseLength = clause.atoms.size();
		List<Integer> changedFunctionIdList = new ArrayList<>(clauseLength);
		for (int i = 0; i < clauseLength; i++) {
			if(clause.atoms.get(i).symbol.id == lastFlippedSymbol) {
				changedFunctionIdList.add(i);
			}
		}
		
		if(changedFunctionIdList.size() == 0) {
			// Do nothing
		} else {
			if(changedFunctionIdList.size() > 1) {
				// Self-Join case
				jt.reCalibrateAll();
			} else {
				int changedFunctionId = changedFunctionIdList.get(0);
				jt.reCalibrate(changedFunctionId, lastFlippedAtomId);
			}
			satCounts[clauseId] = totalCounts[clauseId] - jt.getZ();
		}
	}

	@Override
	public void getRandomGroundClause(int clauseIndex, List<Integer> groundClauseToBeReturned) {
		groundClauseToBeReturned.clear();
		groundClauseToBeReturned.add(clauseIndex);
		
		WClause clause = mln.clauses.get(clauseIndex);
		
		List<Variable> variables = joinTrees[clauseIndex].getVariables();
		
		for (Variable variable : variables) {
			// Pick a random value for the variable
			variable.setAddressValue(random.nextInt(variable.getDomainSize()));
		}
		
		int clauseLength = clause.atoms.size();
		for (int i = 0; i < clauseLength; i++) {
			groundClauseToBeReturned.add(Variable.getAddress(Arrays.asList(clauseWiseFunctionScopes[clauseIndex][i])));
		}
		
		// Clear the address assignment
		for (Variable variable : variables) {
			variable.setAddressValue(null);
		}
	}
	
	private boolean isGroundClauseSatisfied(int liftedClauseIndex, List<Integer> groundClause) {
		boolean satisfied = false;
		WClause clause = mln.clauses.get(liftedClauseIndex);

		int clauseLength = clause.atoms.size();
		for (int i = 0; i < clauseLength; i++) {
			boolean signed = clause.sign.get(i);
			double storedValue = graphicalModel.getFunctions().get(clause.atoms.get(i).symbol.id).getTableEntry(groundClause.get(i+1));
			if(signed == (storedValue == 1.0))
				return true;
		}

		return satisfied;
	}
	
	private boolean rejectionSampling(int liftedClauseIndex, List<Integer> groundClauseToBeReturned) {
		// Before doing rejection sampling check whether the unsat count is at-least 40%
		if(satCounts[liftedClauseIndex] > totalCounts[liftedClauseIndex] * PARAM_REJECTION_SAMPLING_PROB)
			return false;
		
		for (int i = 0; i < PARAM_REJECTION_TRY; i++) {
			getRandomGroundClause(liftedClauseIndex, groundClauseToBeReturned);
			if(!isGroundClauseSatisfied(liftedClauseIndex, groundClauseToBeReturned))
				return true;
		}
		
		return false;
	}

	@Override
	public void getRandomUnsatGroundClause(int clauseIndex, List<Integer> groundClauseToBeReturned) {
		
		// XXX Hack !!! First try rejection sampling 
		boolean found = rejectionSampling(clauseIndex, groundClauseToBeReturned);
		if(found)
			return;
		
		groundClauseToBeReturned.clear();
		groundClauseToBeReturned.add(clauseIndex);
		
		WClause clause = mln.clauses.get(clauseIndex);

		CompiledStructure jt = joinTrees[clauseIndex];
		// Generate a sample
		jt.sample();
		List<Variable> variables = jt.getVariables();
		
		int clauseLength = clause.atoms.size();
		for (int i = 0; i < clauseLength; i++) {
			groundClauseToBeReturned.add(Variable.getAddress(Arrays.asList(clauseWiseFunctionScopes[clauseIndex][i])));
		}
		
		// Clear the address assignment
		for (Variable variable : variables) {
			variable.setAddressValue(null);
			variable.setValue(null);
		}
	}

	@Override
	public void flipAtom(PredicateSymbol symbol, int atomId) {
		graphicalModel.getFunctions().get(symbol.id).toggleTableEntry(atomId);
		lastFlippedSymbol = symbol.id;
		lastFlippedAtomId = atomId;
	}

	@Override
	public void unflipAtom(PredicateSymbol symbol, int atomId) {
		graphicalModel.getFunctions().get(symbol.id).toggleTableEntry(atomId);
		graphicalModel.getFunctions().get(symbol.id).resetChangeEntry(atomId);
		if (selfJoinFlipped) {
			for (Integer clauseId : selfJoinedClauses) {
				CompiledStructure jt = joinTrees[clauseId];
				jt.reCalibrateAll();
			}
			selfJoinedClauses.clear();
			selfJoinFlipped = false;
		}
		lastFlippedSymbol = UNDEFINED;
		lastFlippedAtomId = UNDEFINED;
	}
	
}
