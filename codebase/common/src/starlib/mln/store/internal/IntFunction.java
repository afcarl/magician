package starlib.mln.store.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import starlib.gm.core.Variable;
import starlib.gm.core.VariableValuePair;
import starlib.util.ParallelUtil;

public class IntFunction {
	
	/**
	 * The scope of the function. It is assumed that the variable list is sorted in ascending order
	 */
	public List<Variable> variables;
	
	public List<Integer> multipliers;
	
	protected double[] table;
//	protected int[] change;
	
	protected double constantValue = 0;
	
	protected boolean negated = false;
	
	public IntFunction() {
		
	}
	
	public IntFunction(boolean negated) {
		this.negated = negated;
	}
	
	public IntFunction(List<Variable> variables) {
		this.variables = new ArrayList<>(variables);
		this.populateMultipliers();
		
		int tableSize = this.getFunctionSize();
		table = new double[tableSize];
//		Arrays.fill(table, 1.0);
	}
	
	public IntFunction sharedCopy() {
		IntFunction newFunction = new IntFunction();

		newFunction.table = table;
//		newFunction.change = change;
		newFunction.constantValue = constantValue;
		newFunction.negated = negated;
		
		return newFunction;
	}

	public List<Variable> getVariables() {
		return variables;
	}

	public void setVariables(List<Variable> variables) {
		this.variables = new ArrayList<>(variables);
		this.populateMultipliers();
	}

	public double[] getTable() {
		return table;
	}

	public void setTable(double[] table) {
		this.table = table;
//		if(change == null)
//			change = new int[table.length];
//		else Arrays.fill(change, 0);
	}
	
	public double getTableEntry(int address) {
		return this.table[address];
	}
	
	public void setTableEntry(double value, int address) {
		this.table[address] = value;
	}
	
	public void toggleTableEntry(int address) {
//		this.change[address] = (1 - 2*this.table[address]);
		this.table[address] = (1 - this.table[address]);
	}
	
	public void resetChangeEntry(int address) {
//		this.change[address] = 0;
	}
	
	
	public int getFunctionSize() {
		return Variable.getDomainSize(variables);
	}
	
	public double getConstantValue() {
		return constantValue;
	}
	
	public void setConstantValue(double constantValue) {
		this.constantValue = constantValue;
		variables.clear();
		table = null;
	}
	
	public boolean isNegated() {
		return negated;
	}
	
	public void setNegated(boolean negated) {
		this.negated = negated;
	}
	
	private void populateMultipliers() {
		multipliers = new ArrayList<>(variables.size());
		int multiplier = 1;
		
		int variableSize = variables.size();
		for (int i = variableSize - 1; i >= 0; i--) {
			Variable variable = variables.get(i);
			multipliers.add(multiplier);
			multiplier *= variable.getDomainSize();
		}
		
		Collections.reverse(multipliers);
	}
	
	public int getAddress() {
		int address = 0;
		
		int variableSize = variables.size();
		for (int i = variableSize - 1; i >= 0; i--) {
			Variable variable = variables.get(i);
			address += (multipliers.get(i) * variable.getValue());
		}
		
		return address;
	}
	
	/**
	 * Computes the address of the table corresponding to the complete assignment provided
	 * to all its variables (in the same order as variables)
	 * 
	 * @param values Complete assignment to variables 
	 * @return
	 */
	public int getAddress(List<Integer> values) {
		int address = 0;
		
		int variableSize = variables.size();
		for (int i = variableSize - 1; i >= 0; i--) {
			address += (multipliers.get(i) * values.get(i));
		}
		
		return address;
	}
	
	public IntFunction multiply(IntFunction function) {
		
		//Perform the set union of two function scopes.
		Set<Variable> set = new TreeSet<Variable>(); //To preserve the sorted ordering
		set.addAll(variables);
		set.addAll(function.variables);
		
		IntFunction newFunction = new IntFunction();
		newFunction.variables = new ArrayList<Variable>(set);
		newFunction.populateMultipliers();

		newFunction.table = new double[newFunction.getFunctionSize()];
		for (int i = 0; i < newFunction.getFunctionSize(); i++) {
			
			//Project the new set of variables in existing functions
			Variable.setAddress(newFunction.variables, i);
			int thisEntry = Variable.getAddress(variables);
			int functionEntry = Variable.getAddress(function.variables);
			
			//Multiply the two projected function entries
			double thisValue = negated ? (1 - table[thisEntry]) : table[thisEntry];
			double functionValue = function.negated ? (1 - function.table[functionEntry]) : function.table[functionEntry];
			newFunction.table[i] = thisValue * functionValue; 
		}
		
		return newFunction;
	}
	
	public static IntFunction multiplyAndSumOut(List<IntFunction> functions, Variable v) {
		
		//Perform the set union of two function scopes.
		Set<Variable> set = new TreeSet<Variable>(); //To preserve the sorted ordering
		for (IntFunction function : functions) {
			set.addAll(function.variables);
		}
		
		List<Variable> toBeRemoved = new ArrayList<Variable>();
		for (Variable variable : set) {
			if(variable.isInstantiated()){ 
				toBeRemoved.add(variable);
			}
		}
		
		IntFunction newFunction = new IntFunction();
		newFunction.variables = new ArrayList<Variable>(set);
		newFunction.variables.removeAll(toBeRemoved);
		
		List<Variable> allVariables = new ArrayList<Variable>(newFunction.variables);
		newFunction.variables.remove(v);
		
		newFunction.table = new double[newFunction.getFunctionSize()];
		
		int allVarDomainSize = Variable.getDomainSize(allVariables);
		
		for (int i = 0; i < allVarDomainSize; i++) {
			
			//Project the new set of variables in existing functions
			Variable.setAddress(allVariables, i);

			int value = 1;
			for (IntFunction f : functions) {
				int functionEntry = Variable.getAddress(f.variables);
				if(f.negated) {
					value *= (1 - f.table[functionEntry]);
				} else {
					value *= f.table[functionEntry];
				}
			}
	        
			int newEntry = Variable.getAddress(newFunction.variables);
			newFunction.table[newEntry] += value; //Do an addition
		}
		
		if(newFunction.variables.size() == 0) {
			newFunction.setConstantValue(newFunction.table[0]);
		}
		
		return newFunction;
	}
	
	public IntFunction maxOut(Variable v) {
		IntFunction newFunction = new IntFunction();
		newFunction.variables = new ArrayList<Variable>(variables);
		newFunction.variables.remove(v);
		
		newFunction.table = new double[newFunction.getFunctionSize()];
		
		for (int i = 0; i < table.length; i++) {
			Variable.setAddress(variables, i);
			int newEntry = Variable.getAddress(newFunction.variables);
			newFunction.table[newEntry] = Math.max(newFunction.table[newEntry], table[i]); //Do a max operation
		}
		
		if(newFunction.variables.size() == 0) {
			newFunction.setConstantValue(newFunction.table[0]);
		}
		
		return newFunction;
	}
	
	public IntFunction sumOut(Variable v) {
		IntFunction newFunction = new IntFunction();
		newFunction.variables = new ArrayList<Variable>(variables);
		newFunction.variables.remove(v);
		
		newFunction.table = new double[newFunction.getFunctionSize()];
		
		for (int i = 0; i < table.length; i++) {
			Variable.setAddress(variables, i);
			int newEntry = Variable.getAddress(newFunction.variables);
			newFunction.table[newEntry] += table[i]; //Do an addition
		}
		
		if(newFunction.variables.size() == 0) {
			newFunction.setConstantValue(newFunction.table[0]);
		}
		
		return newFunction;
	}
	
	public void removeEvidence(List<VariableValuePair> evidence) {
		List<Variable> newVariables = new ArrayList<Variable>(variables);
		
		for (VariableValuePair variableValuePair : evidence) {
			Variable v = new Variable(variableValuePair.getVaribaleId(), 0);
			
			int index = variables.indexOf(v);
			if(index > -1) {
				variables.get(index).setValue(variableValuePair.getValueIndex());
				variables.get(index).setAddressValue(variableValuePair.getValueIndex());
			}
			
			newVariables.remove(v);
		}
		
		if(newVariables.size() == 0) {
			
			int entry = Variable.getAddress(variables);
			this.setConstantValue(table[entry]);
			
		} else {

			double[] newTable = new double[Variable.getDomainSize(newVariables)];

			for (int i = 0; i < newTable.length; i++) {
				Variable.setAddress(newVariables, i);
				int entry = Variable.getAddress(variables);
				newTable[i] = table[entry]; //Copy from old address
			}

			variables = newVariables;
			table = newTable;
		}
	}

	public boolean isEmpty() {
		return variables.size() == 0;
	}
	
	public boolean isFullyInstantiated() {
		boolean fullyInstantiated = false;
		
		for (Variable v : variables) {
			if (v.isInstantiated()) {
				fullyInstantiated = true;
				continue;
			} else { 
				return false;
			}
		}

		return fullyInstantiated;
	}

	public boolean contains(int var) {
		
		for (Variable variable : variables) {
			if(variable.getId() == var) {
				return true;
			}
		}
		
		return false;
	}

	public static IntFunction multiplyAndMarginalize(List<Variable> marg_variables_, List<IntFunction> functions) 
	{
		// Compute the appropriate variables from the functions
		List<Variable> all_variables;
		List<Variable> marg_variables;
		
		int num_functions = functions.size();
		if(num_functions==0)
			return null;
		
		//Perform the set union of all function scopes.
		Set<Variable> union = new TreeSet<Variable>();
		for (IntFunction function : functions) {
			union.addAll(function.variables);
		}
		
		List<Variable> toBeRemoved = new ArrayList<Variable>();
		for (Variable variable : union) {
			if(variable.isInstantiated()){ 
				toBeRemoved.add(variable);
			}
		}
		
		// Remove evidence instantiated variables
		union.removeAll(toBeRemoved);
		
		all_variables = new ArrayList<Variable>(union);
		
		union.retainAll(marg_variables_); // Do Set intersection
		marg_variables = new ArrayList<Variable>(union);
		
		int newTableSize = Variable.getDomainSize(marg_variables);
		
		IntFunction newFunction = new IntFunction();
		newFunction.variables = marg_variables;
		newFunction.table = new double[newTableSize];
		newFunction.populateMultipliers();
//		newFunction.change = new int[newTableSize];

		
		int allVarDomainSize = Variable.getDomainSize(all_variables);
		
		for (int i = 0; i < allVarDomainSize; i++) {
			
			//Project the new set of variables in existing functions
			Variable.setAddress(all_variables, i);

			int value = 1;
			for (IntFunction f : functions) {
				int functionEntry = Variable.getAddress(f.variables);
				if(f.negated) {
					value *= (1 - f.table[functionEntry]);
				} else {
					value *= f.table[functionEntry];
				}
			}
	        
			int newEntry = Variable.getAddress(newFunction.variables);
			newFunction.table[newEntry] += value; //Do an addition
		}
		
		if(newFunction.isEmpty()) {
			newFunction.setConstantValue(newFunction.table[0]);
		}
		
		
		return newFunction;
	}
	
	
	/**
	 * The function outFunction is already created it just modifies the new entries
	 * 
	 * @param functions
	 * @param outFunction
	 */
	public static void approximateMultiplyAndMarginalize(List<IntFunction> functions, IntFunction outFunction) 
	{
		// Compute the appropriate variables from the functions
		int num_functions = functions.size();
		if(num_functions==0)
			return;
		
		//Perform the set union of all function scopes.
		Set<Variable> union = new TreeSet<Variable>();
		for (IntFunction function : functions) {
			union.addAll(function.variables);
		}
		
		List<Variable> toBeRemoved = new ArrayList<>(union.size());
		for (Variable variable : union) {
			if(variable.isInstantiated()){ 
				toBeRemoved.add(variable);
			}
		}
		
		// First initialize ZERO in places where change is going to happen
		List<Variable> freeFunctionVars = new ArrayList<Variable>(outFunction.variables);
		freeFunctionVars.removeAll(toBeRemoved);
		int freeVarDomainSize = Variable.getDomainSize(freeFunctionVars);
		
		for (int i = 0; i < freeVarDomainSize; i++) {
			//Project the new set of variables in existing functions
			Variable.setAddress(freeFunctionVars, i);
			int newEntry = Variable.getAddress(outFunction.variables);
//			outFunction.change[newEntry] = 0; // Initialize to zero before doing an addition
		}
		
		List<Variable> all_variables = new ArrayList<Variable>(union);
		all_variables.removeAll(toBeRemoved);
	
		int allVarDomainSize = Variable.getDomainSize(all_variables);
		
		for (int i = 0; i < allVarDomainSize; i++) {
			
			//Project the new set of variables in existing functions
			Variable.setAddress(all_variables, i);

			int value = 1;
			int oldValue = 1;
			for (IntFunction f : functions) {
				int functionEntry = Variable.getAddress(f.variables);
				if(f.negated) {
					value *= (1 - f.table[functionEntry]);
//					oldValue *= (1 - f.table[functionEntry]) + f.change[functionEntry];
				} else {
					value *= f.table[functionEntry];
//					oldValue *= f.table[functionEntry] - f.change[functionEntry];
				}
			}
	        
			int newEntry = Variable.getAddress(outFunction.variables);
			outFunction.table[newEntry] += (value - oldValue); //Do an addition
//			outFunction.change[newEntry] += (value - oldValue); //Do an addition
		}
		
	}
	
	public static void resetChanges(IntFunction outFunction) {
		List<Variable> toBeRemoved = new ArrayList<Variable>();
		for (Variable variable : outFunction.variables) {
			if(variable.isInstantiated()){ 
				toBeRemoved.add(variable);
			}
		}
		
		// First initialize ZERO in places where change is going to happen
		List<Variable> freeFunctionVars = new ArrayList<Variable>(outFunction.variables);
		freeFunctionVars.removeAll(toBeRemoved);
		int freeVarDomainSize = Variable.getDomainSize(freeFunctionVars);
		
		for (int i = 0; i < freeVarDomainSize; i++) {
			//Project the new set of variables in existing functions
			Variable.setAddress(freeFunctionVars, i);
			int newEntry = Variable.getAddress(outFunction.variables);
//			outFunction.change[newEntry] = 0; // Initialize to zero before doing an addition
		}
		
	}
	
	
	/**
	 * The function outFunction is already created it just modifies the new entries
	 * 
	 * @param functions
	 * @param outFunction
	 */
	public static void multiplyAndMarginalizeInPlace(final List<IntFunction> functions, final IntFunction outFunction) 
	{
		// Compute the appropriate variables from the functions
		final int num_functions = functions.size();
		if(num_functions==0)
			return;
		

		List<Variable> freeFunctionVars = new ArrayList<Variable>(outFunction.variables.size());
		List<Variable> all_variables = new ArrayList<Variable>();
		List<Variable> nonOutFunctionVariables =  new ArrayList<Variable>();
		List<Integer> nonFnVarValues =  new ArrayList<Integer>();
		
		calculateVariables(functions, outFunction, freeFunctionVars, all_variables, nonOutFunctionVariables, nonFnVarValues);
		
		// Use Gray-Code to visit the out-function addresses and set zero as value
		List<IntFunction> outFunctionAsList = new ArrayList<>(1);
		outFunctionAsList.add(outFunction);
		
		GrayCodeFunctionVisitor outFunctionVisitor = new GrayCodeFunctionVisitor(freeFunctionVars, outFunctionAsList) {
			@Override
			protected void visitAddress() {
				int newEntry = this.getFunctionAddress(0); // Only one function, get the current address for it
				outFunction.table[newEntry] = 0; // Initialize to zero before doing an addition
			}
		};
		outFunctionVisitor.visit();
		
		// Use Gray-Code to visit the function addresses
		List<IntFunction> all_functions = new ArrayList<>(functions.size() + 1);
		all_functions.addAll(functions);
		all_functions.add(outFunction);
		
		GrayCodeFunctionVisitor visitor = new GrayCodeFunctionVisitor(all_variables, all_functions) {
			@Override
			protected void visitAddress() {
				// Visit the gray coded address locations
				int value = 1;
				for (int i = 0; i < num_functions; i++) {
					int functionEntry = this.getFunctionAddress(i);
					IntFunction f = functions.get(i);
					if(f.negated) {
						value *= (1 - f.table[functionEntry]);
					} else {
						value *= f.table[functionEntry];
					}
				}
		        
				int newEntry = this.getFunctionAddress(num_functions);
				outFunction.table[newEntry] += value; //Do an addition
			}
		};
		visitor.visit();

		// Now reset the evidence on non-out-function variables
		resetEvidence(nonOutFunctionVariables, nonFnVarValues);
	}
	
	public static void multiplyAndMarginalizeConstantFunction(List<IntFunction> functions, IntFunction outFunction) 
	{
		// Compute the appropriate variables from the functions
		int num_functions = functions.size();
		if(num_functions==0)
			return;

		List<Variable> freeFunctionVars = new ArrayList<Variable>(outFunction.variables.size());
		List<Variable> all_variables = new ArrayList<Variable>();
		List<Variable> nonOutFunctionVariables =  new ArrayList<Variable>();
		List<Integer> nonFnVarValues =  new ArrayList<Integer>();
		
		calculateVariables(functions, outFunction, freeFunctionVars, all_variables, nonOutFunctionVariables, nonFnVarValues);
		

		outFunction.constantValue = 0;
		
		int allVarDomainSize = Variable.getDomainSize(all_variables);
		
		for (int i = 0; i < allVarDomainSize; i++) {
			
			//Project the new set of variables in existing functions
			Variable.setAddress(all_variables, i);

			int value = 1;
			for (IntFunction f : functions) {
				int functionEntry = f.getAddress();
				if(f.negated) {
					value *= (1 - f.table[functionEntry]);
				} else {
					value *= f.table[functionEntry];
				}
			}
	        
			outFunction.constantValue += value; //Do an addition
		}
		
		// Now reset the evidence on non-out-function variables
		resetEvidence(nonOutFunctionVariables, nonFnVarValues);
	}
	
	private static void calculateVariables(List<IntFunction> functions, IntFunction outFunction, List<Variable> freeFunctionVars, List<Variable> all_variables, List<Variable> nonOutFunctionVariables, List<Integer> nonFnVarValues) {
		//Perform the set union of all function scopes.
		Set<Variable> union = new TreeSet<>();
		for (IntFunction function : functions) {
			union.addAll(function.variables);
		}
		
		List<Variable> toBeRemoved = new ArrayList<Variable>();
		for (Variable variable : union) {
			if(variable.isInstantiated()){ 
				toBeRemoved.add(variable);
			}
		}
		
		List<Variable> instantiatedFunctionVars = new ArrayList<Variable>(outFunction.variables);
		instantiatedFunctionVars.retainAll(toBeRemoved);
		
		// First initialize ZERO in places where change is going to happen
		freeFunctionVars.addAll(outFunction.getVariables());
		freeFunctionVars.removeAll(toBeRemoved);
		
		nonOutFunctionVariables.addAll(union);
		nonOutFunctionVariables.removeAll(outFunction.variables);

		// Clear evidence on non-out-function variables (storing the old values)
		for (Variable nonFnVariable : nonOutFunctionVariables) {
			nonFnVariable.setAddressValue(null);
			nonFnVarValues.add(nonFnVariable.getValue());
			nonFnVariable.setValue(null);
		}
		
		all_variables.addAll(union);
		all_variables.removeAll(instantiatedFunctionVars);
	}
	
	private static void resetEvidence(List<Variable> nonOutFunctionVariables, List<Integer> nonFnVarValues) {
		int index = 0;
		for (Variable nonFnVariable : nonOutFunctionVariables) {
			nonFnVariable.setValue(nonFnVarValues.get(index));
			nonFnVariable.setAddressValue(null);
			index++;
		}
	}
	
	
	/**
	 * Returns table entries addressed by free variables (i.e.- variables which are not instantiated) 
	 * @param freeVariables
	 * @return
	 */
	public double[] getTableEntries(List<Variable> freeVariables) {
		int size = Variable.getDomainSize(freeVariables);
		double[] returnVal = new double[size];
		
		for (int i = 0; i < size ; i++) {
			Variable.setAddress(freeVariables, i);
			int newEntry = Variable.getAddress(variables);
			returnVal[i] = table[newEntry];
		}

		return returnVal;
	}

	
	/**
	 * Returns partial sum of table entries addressed by free variables (i.e.- variables which are not instantiated) 
	 * @param freeVariables
	 * @return
	 */
	public int getPartialSum(List<Variable> freeVariables) {
		int size = Variable.getDomainSize(freeVariables);
		int sum = 0;
		
		for (int i = 0; i < size ; i++) {
			Variable.setAddress(freeVariables, i);
			int newEntry = Variable.getAddress(variables);
			sum += table[newEntry];
		}

		return sum;
	}

	
	
	public double getPartiotionFunction() {
		if(isEmpty())
			return constantValue;
		else 
			return ParallelUtil.sum(table);
	}
	
	/**
	 * outFunction is already created, just modify the new entries using the change addresses
	 * Address of change for all the functions are provided through functionWiseChangedAddresses.
	 * It is assumed that a value is changed only in a single position, also if nothing is changed 
	 * for a function a null is provided. This method acts similar to multiplyAndMarginalizeInPlace
	 * however it first calculates the addresses which would be visited more than once due to 
	 * different function changes and it ensures that those addresses are visited only once.
	 * 
	 * @param functions
	 * @param outFunction
	 * @param functionWiseChangedAddresses
	 */
	public static void multiplyAndMarginalizeInPlaceForMultipleChange(final List<IntFunction> functions, final IntFunction outFunction, List<Integer> functionWiseChangedAddresses) 
	{
		// Compute the appropriate variables from the functions
		final int num_functions = functions.size();
		if(num_functions==0)
			return;
		
		Integer[] outFunctionVarValuesForOverlapAddress = new Integer[outFunction.variables.size()];
		int overLapAddressSize  = 1;
		
		// First clear the table values at out-function
		for (int i = 0; i < functionWiseChangedAddresses.size(); i++) {
			Integer changedAddress = functionWiseChangedAddresses.get(i);
			if(changedAddress == null)
				continue; //Nothing changed
			
			IntFunction function = functions.get(i);
			
			// Free variables in out function, ie- out function variables that do not appear in function 
			List<Variable> freeFunctionVars = new ArrayList<>(outFunction.variables);
			freeFunctionVars.removeAll(function.variables);

			// Instantiated variables in out function, ie- intersection of out function variables and function variable 
			List<Variable> instantiatedFunctionVars = new ArrayList<>(outFunction.variables);
			instantiatedFunctionVars.retainAll(function.variables);
			
			// Set the changed address into function variables
			Variable.instantiateVariables(function.variables, changedAddress);

			// Compute the variable-values for overlapped addresses
			for (int j = 0; j < outFunctionVarValuesForOverlapAddress.length; j++) {
				Variable variable = outFunction.variables.get(j);
				
				if(variable.isInstantiated()) {
					
					int variableValue = variable.getValue();
					Integer savedValue = outFunctionVarValuesForOverlapAddress[j];
					
					if(savedValue == null || savedValue.equals(variableValue))
						outFunctionVarValuesForOverlapAddress[j] = variable.getValue();
					else {
						// Conflict! There is no overlap
						outFunctionVarValuesForOverlapAddress = null; // Free array
						overLapAddressSize = 0;
						break;
					}
				}
			}
			
			// Use Gray-Code to visit the out-function addresses and set zero as value
			List<IntFunction> outFunctionAsList = new ArrayList<>(1);
			outFunctionAsList.add(outFunction);
			
			GrayCodeFunctionVisitor outFunctionVisitor = new GrayCodeFunctionVisitor(freeFunctionVars, outFunctionAsList) {
				@Override
				protected void visitAddress() {
					int newEntry = this.getFunctionAddress(0); // Only one function, get the current address for it
					outFunction.table[newEntry] = 0; // Initialize to zero before doing an addition
				}
			};
			outFunctionVisitor.visit();
			
			Variable.freeVariables(function.variables);
			Variable.freeVariables(outFunction.variables);
		}
		
		// Compute the size of the overlapped address
		for (int i = 0; overLapAddressSize > 0 && i < outFunctionVarValuesForOverlapAddress.length; i++) {
			Variable variable = outFunction.variables.get(i);
			Integer varValue = outFunctionVarValuesForOverlapAddress[i];
			if(varValue != null) {
				variable.setValue(varValue);
			} else {
				// Variable is not instantiated, we need to visit all possible assignments of it
				overLapAddressSize *= variable.getDomainSize();
			}
		}
		
		// Store all the overlapped addresses, so that we visit them only once;
		// Do NOT use GrayCode to visit as we want the addresses to be sorted
		final int[] overLappedAddresses = new int[overLapAddressSize];
		if(overLapAddressSize > 0) {
			for (int i = 0; i < overLappedAddresses.length; i++) {
				Variable.setAddress(outFunction.variables, i);
				overLappedAddresses[i] = outFunction.getAddress();
			}
			Variable.freeVariables(outFunction.variables);
		}
		
		//Perform the set union of all function scopes.
		Set<Variable> union = new TreeSet<>();
		for (IntFunction function : functions)
			union.addAll(function.variables);
		
		List<Variable> all_variables = new ArrayList<>(union);

		
		// Compute the marginal value by visiting the changed functions one at a time, skipping the overlapped Addresses
		for (int i = 0; i < functionWiseChangedAddresses.size(); i++) {
			Integer changedAddress = functionWiseChangedAddresses.get(i);
			if(changedAddress == null)
				continue; //Nothing changed

			IntFunction function = functions.get(i);

			// Set the changed address into function variables
			Variable.instantiateVariables(function.variables, changedAddress);
			
			// Instantiated variables in out function, ie- intersection of out function variables and function variable 
			List<Variable> instantiatedFunctionVars = new ArrayList<>(outFunction.variables);
			instantiatedFunctionVars.retainAll(function.variables);
			
			// Un-instantiate all the variables that do not belong to outFunction
			List<Variable> nonOutFunctionVariables =  new ArrayList<>(union);
			nonOutFunctionVariables.removeAll(outFunction.variables);
			Variable.freeVariables(nonOutFunctionVariables);

			// Compute free variables
			List<Variable> freeVariables = new ArrayList<>(all_variables);
			freeVariables.removeAll(instantiatedFunctionVars);
			
			// Use Gray-Code to visit the function addresses
			List<IntFunction> all_functions = new ArrayList<>(functions.size() + 1);
			all_functions.addAll(functions);
			all_functions.add(outFunction);

			GrayCodeFunctionVisitor visitor = new GrayCodeFunctionVisitor(freeVariables, all_functions) {
				@Override
				protected void visitAddress() {
					// Visit the gray coded address locations
					
					// The address to be visited in outFunction
					int newEntry = this.getFunctionAddress(num_functions);
					if(Arrays.binarySearch(overLappedAddresses, newEntry) >= 0)
						return; // The address is in overlap. Skip it
					
					int value = 1;
					for (int i = 0; i < num_functions; i++) {
						int functionEntry = this.getFunctionAddress(i);
						IntFunction f = functions.get(i);
						if(f.negated) 
							value *= (1 - f.table[functionEntry]);
						else
							value *= f.table[functionEntry];
					}
					outFunction.table[newEntry] += value; //Do an addition
				}
			};
			visitor.visit();

			// Now reset the evidence on non-out-function variables
			Variable.freeVariables(all_variables);
		}
		
		// Compute the marginal value by visiting the changed functions one at a time, skipping the overlapped Addresses
		for (int i = 0; i < overLappedAddresses.length; i++) {
			int changedAddress = overLappedAddresses[i];

			// Set the changed address into function variables
			Variable.instantiateVariables(outFunction.variables, changedAddress);
			
			// Compute free variables
			List<Variable> freeVariables = new ArrayList<>(all_variables);
			freeVariables.removeAll(outFunction.variables);
			
			// Use Gray-Code to visit the function addresses
			List<IntFunction> all_functions = new ArrayList<>(functions.size() + 1);
			all_functions.addAll(functions);
			all_functions.add(outFunction);

			GrayCodeFunctionVisitor visitor = new GrayCodeFunctionVisitor(freeVariables, all_functions) {
				@Override
				protected void visitAddress() {
					// Visit the gray coded address locations
					
					// The address to be visited in outFunction
					int newEntry = this.getFunctionAddress(num_functions);
					
					int value = 1;
					for (int i = 0; i < num_functions; i++) {
						int functionEntry = this.getFunctionAddress(i);
						IntFunction f = functions.get(i);
						if(f.negated) 
							value *= (1 - f.table[functionEntry]);
						else 
							value *= f.table[functionEntry];
					}
					outFunction.table[newEntry] += value; //Do an addition
				}
			};
			visitor.visit();

			// Now reset the evidence on non-out-function variables
			Variable.freeVariables(all_variables);
		}
	}
	
	public static void main(String[] args) {
		Variable x = new Variable(0, 2);
		Variable y = new Variable(1, 2);
//		Variable z = new Variable(2, 2);
		
		List<Variable> vars1 = new ArrayList<>();
		vars1.add(x);
		vars1.add(y);
		
		IntFunction f1 = new IntFunction(vars1);
		f1.table[1] = 1;
		f1.table[2] = 1;
		f1.table[3] = 1;
		
		List<Variable> vars2 = new ArrayList<>();
		vars2.add(y);
		vars2.add(x);
		
		IntFunction f2 = new IntFunction(vars2);
		f2.table[1] = 1;
		f2.table[2] = 1;
		f2.table[3] = 1;
		
		List<Variable> vars3 = new ArrayList<>();
		vars3.add(x);
		vars3.add(y);
		
		IntFunction f3 = new IntFunction(vars3);
		f3.table[0] = -1;
		f3.table[1] = -1;
		f3.table[2] = -1;
		f3.table[3] = -1;
		
		List<IntFunction> functions = new ArrayList<>();
		functions.add(f1);
		functions.add(f2);
		
		List<Integer> changedAddressList = new ArrayList<>();
		changedAddressList.add(2);
		changedAddressList.add(2);
		
		multiplyAndMarginalizeInPlaceForMultipleChange(functions, f3, changedAddressList);
		System.out.println(Arrays.toString(f3.table));
	}
}
