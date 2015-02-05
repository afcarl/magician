package starlib.mln.store.clause.jt;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import starlib.gm.core.Variable;
import starlib.mln.store.internal.IntFunction;
import starlib.util.ParallelUtil;

public class JGNodeInt {
	
	protected Random random = new Random();
	
	private static final int INVALID_VALUE = -1;
	private static final int MAX_TABLE_SIZE = 2000000;
	

	protected List<Variable> variables_;
	protected List<JGEdgeInt> edges_;
	protected List<IntFunction> functions;
	
	
	protected int id_;
	protected boolean deleted_;
	protected boolean outOfSyn;
	
	/** 
	 * If there are more than one functions we flatten them and store the result here
	 */
	protected IntFunction marginal;
	
	protected double partitionFunction = -1;
	
	protected boolean marginalStored = true;
	
	protected List<IntFunction> all_functions;

	// Default constructor
	public JGNodeInt() {
		id_ = INVALID_VALUE; 
		deleted_ = false;
		variables_ = new ArrayList<>();
		edges_ = new ArrayList<>();
		outOfSyn = false;
		functions = new ArrayList<>();
	}
	
	// Access to some internal variables
	public boolean isDeleted() {
		return deleted_;
	}

	public List<Variable> variables() {
		return variables_;
	}
	
	public List<JGEdgeInt> edges() {
		return edges_;
	}
	
	public int id() {
		return id_;
	}

	public void delete() {
		deleted_ = true;
		variables_ = null;
		edges_ = null;
		marginal = null;
		all_functions = null;
		partitionFunction = -1;
	}
	
	private List<IntFunction> compileAllFunctions()
	{
		if(all_functions != null && !all_functions.isEmpty())
			return all_functions;
		
		all_functions = new ArrayList<>(functions);
		for(int i=0;i<edges_.size();i++)
		{
			if(edges_.get(i).node1()==this)
			{
				if(isFunctionEmpty(edges_.get(i).message2()))
					continue;
				all_functions.add(edges_.get(i).message2());
			}
			else
			{
				if(isFunctionEmpty(edges_.get(i).message1()))
					continue;
				all_functions.add(edges_.get(i).message1());
			}
		}
		
		return all_functions;
	}
	
	private boolean isFunctionEmpty(IntFunction function) {
		if(function == null)
			return true;
		
		return function.isEmpty();
	}
	
	private void updateVariables()
	{
		List<IntFunction> all_functions = compileAllFunctions();
		Set<Variable> union = new TreeSet<>();
		for(int i=0; i < all_functions.size();i++){
			union.addAll(all_functions.get(i).getVariables());
		}
		variables_ = new ArrayList<>(union);
	}
	
	public void addFunction(IntFunction function)
	{
		functions.add(function);
	}
	
	public IntFunction getMarginal(List<Variable> marg_variables)
	{
		List<IntFunction> all_functions = compileAllFunctions();
		return IntFunction.multiplyAndMarginalize(marg_variables,all_functions);
	}	
	
	public void updateMarginal(IntFunction outFunction)
	{
		// Update the marginal table in-place
		IntFunction.approximateMultiplyAndMarginalize(all_functions, outFunction);
	}	
	
	public void computeZ()
	{
		this.updateVariables();
		int tableSize = Variable.getDomainSize(variables_);
		if(tableSize > MAX_TABLE_SIZE) {
			marginalStored = false;
			return;
		}
		
		marginal =  IntFunction.multiplyAndMarginalize(variables_, all_functions);
		partitionFunction = marginal.getPartiotionFunction();
	}
	
	public double computeDeltaZ(IntFunction changedFunction, int address) {
		
		List<Variable> marg_variables = new ArrayList<>(variables_);

		// Remove changed function scopes to compute marginal vars
		marg_variables.removeAll(changedFunction.variables);
		
		if(!marginalStored) {
			// Compute the new Z
			IntFunction newMarginal = IntFunction.multiplyAndMarginalize(marg_variables, all_functions);
			double newZ = newMarginal.getPartiotionFunction();
			
			// Now reset the changes by toggling address
			changedFunction.toggleTableEntry(address);
			
			// Compute old Z
			IntFunction oldMarginal = IntFunction.multiplyAndMarginalize(marg_variables, all_functions);
			double oldZ = oldMarginal.getPartiotionFunction();
			
			// Toggle the changed address again to reinstate the new value 
			changedFunction.toggleTableEntry(address);
			
			return newZ-oldZ;
		}
		
		// First initialize ZERO in places where change is going to happen
		List<Variable> freeFunctionVars = getFreeVariables(marginal.variables);
		
		// First compute the old Z
		int oldZ = this.getAffectedZ(freeFunctionVars);
		
		// Next compute the new Z
		IntFunction marginal2 = IntFunction.multiplyAndMarginalize(marg_variables, all_functions);
		double newZ = marginal2.getPartiotionFunction();
		
		return newZ-oldZ;
	}
	
	public void updateZ(boolean singleChange) {
		if(!marginalStored) {
			// Do nothing
			return;
		}
		
		if(singleChange) {
			List<Variable> freeMarginalVars = getFreeVariables(marginal.variables);

			// First compute the old (affected) Z
			int oldZ = this.getAffectedZ(freeMarginalVars);

			// Update the marginal table in-place
			IntFunction.multiplyAndMarginalizeInPlace(all_functions, marginal);

			// Now compute the new (affected) Z
			int newZ = this.getAffectedZ(freeMarginalVars);

			partitionFunction += (newZ - oldZ);
		} else {
			// All entries will be updated
			IntFunction.multiplyAndMarginalizeInPlace(all_functions, marginal);
			partitionFunction = marginal.getPartiotionFunction();
			outOfSyn = false;
		}
	}
	
	public void updateZ(List<IntFunction> changedFunctions, int changedAddress) {
		if(!marginalStored) {
			// Do nothing
			return;
		}
		
		// All entries will be updated
		List<Integer> changedAddressList = new ArrayList<>(all_functions.size());
		boolean atLeastOneChanged = false;
		
		for (IntFunction function : all_functions) {
			if(changedFunctions.contains(function)) {
				changedAddressList.add(changedAddress);
				atLeastOneChanged = true;
			} else {
				changedAddressList.add(null);
			}
		}
		
		if(atLeastOneChanged)
			IntFunction.multiplyAndMarginalizeInPlaceForMultipleChange(all_functions, marginal, changedAddressList);
		else
			IntFunction.multiplyAndMarginalizeInPlace(all_functions, marginal);
		partitionFunction = marginal.getPartiotionFunction();
		outOfSyn = false;
	}
	
	public void resetChangeMarginal() {
		if(!marginalStored) {
			// Do nothing
			return;
		}
		
		IntFunction.resetChanges(marginal);
	}
	
	public double getZ() {
		return partitionFunction;
	}
	
	public void initialize()
	{
		for(int i=0;i<edges_.size();i++)
		{
			edges_.get(i).node1_to_node2_message_ = new IntFunction();
			edges_.get(i).node2_to_node1_message_ = new IntFunction();
		}
	}
	
	public void sample() {
		List<Variable> freeMarginalVars = getFreeVariables(variables_);
		if(freeMarginalVars.isEmpty())
			return; // All variables instantiated, nothing to do

		double norm_const;
		double[] entries;
		if(!marginalStored) {
			// Marginal is not stored. Compute the marginal
			IntFunction tempMarginal =  IntFunction.multiplyAndMarginalize(freeMarginalVars, all_functions);
			entries = tempMarginal.getTable();
			norm_const = ParallelUtil.sum(entries); // Recompute Z
		} else if(freeMarginalVars.size() == marginal.variables.size()) {
			// All variables are free proceed with the cached values
			entries = marginal.getTable();
			norm_const = partitionFunction;
		} else {
			// Get affected entries
			entries = marginal.getTableEntries(freeMarginalVars);
			norm_const = ParallelUtil.sum(entries); // Recompute Z
		}

		int address = -1;
		if(norm_const <= 0) {
			address = ParallelUtil.max(entries);
		} else {
			address = ParallelUtil.sample(entries, norm_const);
		}

		if(address < 0) {
			System.err.println("Failed sampling!!!");
		}

		// Set variable values
		Variable.setAddress(freeMarginalVars, address);
		for (Variable variable : freeMarginalVars) {
			variable.setValue(variable.getValue());
		}
	}
	
	private static List<Variable> getFreeVariables(List<Variable> variables) {
		List<Variable> toBeRemoved = new ArrayList<>(variables.size());
		for (Variable variable : variables) {
			if(variable.isInstantiated()){ 
				toBeRemoved.add(variable);
			}
		}
		
		// First initialize ZERO in places where change is going to happen
		List<Variable> freeVariables = new ArrayList<>(variables);
		freeVariables.removeAll(toBeRemoved);
		
		return freeVariables;
	}
	
	private int getAffectedZ(List<Variable> freeVariables) {
		// Sum-out those entries to compute affected Z
		return marginal.getPartialSum(freeVariables);
	}
	
	public boolean isEmpty() {
		if(functions == null)
			return true;
		
		if(functions.isEmpty())
			return true;
		
		return false;
	}
}
