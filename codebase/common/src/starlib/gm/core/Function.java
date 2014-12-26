package starlib.gm.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import starlib.util.Pair;

public class Function {
	
	/**
	 * The scope of the function. It is assumed that the variable list is sorted in ascending order
	 */
	protected List<Variable> variables = new ArrayList<Variable>();
	
	protected List<LogDouble> table;
	
	protected LogDouble constantValue = LogDouble.ZERO;
	
	public Function() {
		
	}
	
	public Function(List<Variable> variables) {
		this.variables.addAll(variables);
		
		int tableSize = this.getFunctionSize();
		
		table = new ArrayList<LogDouble>(tableSize);
		for (int i = 0; i < tableSize; i++) {
			table.add(LogDouble.ZERO);
		}
	}

	public List<Variable> getVariables() {
		return variables;
	}

	public void setVariables(List<Variable> variables) {
		this.variables.addAll(variables);
	}

	public List<LogDouble> getTable() {
		return table;
	}

	public void setTable(List<LogDouble> table) {
		this.table = table;
	}
	
	public void setTableEntry(LogDouble value, int address) {
		this.table.set(address, value);
	}
	
	public int getFunctionSize() {
		return Variable.getDomainSize(variables);
	}
	
	public LogDouble getConstantValue() {
		return constantValue;
	}
	
	public void setConstantValue(LogDouble constantValue) {
		this.constantValue = constantValue;
		variables.clear();
		table = null;
	}
	
	public Function multiply(Function function) {
		
		//Perform the set union of two function scopes.
		Set<Variable> set = new TreeSet<Variable>(); //To preserve the sorted ordering
		set.addAll(variables);
		set.addAll(function.variables);
		
		Function newFunction = new Function();
		newFunction.variables = new ArrayList<Variable>(set);
		
		newFunction.table = new ArrayList<LogDouble>();
		for (int i = 0; i < newFunction.getFunctionSize(); i++) {
			
			//Project the new set of variables in existing functions
			Variable.setAddress(newFunction.variables, i);
			int thisEntry = Variable.getAddress(variables);
			int functionEntry = Variable.getAddress(function.variables);
			
			//Multiply the two projected function entries
			newFunction.table.add(table.get(thisEntry).multiply(function.table.get(functionEntry))); 
		}
		
		return newFunction;
	}
	
	public static Function multiplyAndSumOut(List<Function> functions, Variable v) {
		
		//Perform the set union of two function scopes.
		Set<Variable> set = new TreeSet<Variable>(); //To preserve the sorted ordering
		for (Function function : functions) {
			set.addAll(function.variables);
		}
		
		List<Variable> toBeRemoved = new ArrayList<Variable>();
		for (Variable variable : set) {
			if(variable.isInstantiated()){ 
				toBeRemoved.add(variable);
			}
		}
		
		Function newFunction = new Function();
		newFunction.variables = new ArrayList<Variable>(set);
		newFunction.variables.removeAll(toBeRemoved);
		
		newFunction.table = new ArrayList<LogDouble>();
		for (int i = 0; i < newFunction.getFunctionSize(); i++) {
			newFunction.table.add(LogDouble.ZERO);
		}
		
		List<Variable> allVariables = new ArrayList<Variable>(newFunction.variables);
		newFunction.variables.remove(v);
		
		for (int i = 0; i < Variable.getDomainSize(allVariables); i++) {
			
			//Project the new set of variables in existing functions
			Variable.setAddress(allVariables, i);

			LogDouble value = LogDouble.ONE;
			for (Function f : functions) {
				int functionEntry = Variable.getAddress(f.variables);
				value = value.multiply(f.table.get(functionEntry));
			}
	        
			int newEntry = Variable.getAddress(newFunction.variables);
			newFunction.table.set(newEntry, newFunction.table.get(newEntry).add(value)); //Do an addition
		}
		
		if(newFunction.variables.size() == 0) {
			newFunction.setConstantValue(newFunction.table.get(0));
		}
		
		return newFunction;
	}
	
	public Function maxOut(Variable v) {
		Function newFunction = new Function();
		newFunction.variables = new ArrayList<Variable>(variables);
		newFunction.variables.remove(v);
		
		newFunction.table = new ArrayList<LogDouble>();
		for (int i = 0; i < newFunction.getFunctionSize(); i++) {
			newFunction.table.add(LogDouble.ZERO);
		}
		
		for (int i = 0; i < table.size(); i++) {
			Variable.setAddress(variables, i);
			int newEntry = Variable.getAddress(newFunction.variables);
			newFunction.table.set(newEntry, LogDouble.max(newFunction.table.get(newEntry), table.get(i))); //Do a max operation
		}
		
		if(newFunction.variables.size() == 0) {
			newFunction.setConstantValue(newFunction.table.get(0));
		}
		
		return newFunction;
	}
	
	public Function sumOut(Variable v) {
		Function newFunction = new Function();
		newFunction.variables = new ArrayList<Variable>(variables);
		newFunction.variables.remove(v);
		
		newFunction.table = new ArrayList<LogDouble>();
		for (int i = 0; i < newFunction.getFunctionSize(); i++) {
			newFunction.table.add(LogDouble.ZERO);
		}
		
		for (int i = 0; i < table.size(); i++) {
			Variable.setAddress(variables, i);
			int newEntry = Variable.getAddress(newFunction.variables);
			newFunction.table.set(newEntry, newFunction.table.get(newEntry).add(table.get(i))); //Do an addition
		}
		
		if(newFunction.variables.size() == 0) {
			newFunction.setConstantValue(newFunction.table.get(0));
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
			this.setConstantValue(table.get(entry));
			
		} else {

			List<LogDouble> newTable = new ArrayList<LogDouble>();
			for (int i = 0; i < Variable.getDomainSize(newVariables); i++) {
				newTable.add(LogDouble.ZERO);
			}

			for (int i = 0; i < newTable.size(); i++) {
				Variable.setAddress(newVariables, i);
				int entry = Variable.getAddress(variables);
				newTable.set(i, table.get(entry)); //Copy from old address
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

	public static Function multiplyAndMarginalize(List<Variable> marg_variables_, List<Function> functions) 
	{
		//cout<<"Mult\n";
		// Compute the appropriate variables from the two functions
		List<Variable> variables;
		List<Variable> marg_variables;
		
		int num_functions=functions.size();
		if(num_functions==0)
			return null;
		
		Set<Variable> union = new TreeSet<Variable>();
		for(int i=0;i<num_functions;i++)
		{
			union.addAll(functions.get(i).getVariables());
		}
		int num_variables = union.size();
		if(num_variables==0)
			return null;
		
		variables = new ArrayList<Variable>(union);
		
		union.retainAll(marg_variables_); // Do Set intersection
		marg_variables = new ArrayList<Variable>(union);
		
		int num_marg_variables=marg_variables.size();
		//if(num_marg_variables==0)
		//	return;

		// 6 arrays for graycoding using Knuth's algorithm
		List<List< Pair<Integer,Integer>>> g = new ArrayList<List<Pair<Integer,Integer>>>(num_variables);
		int[] c = new int[num_functions];
		int[] a = new int[num_variables];
		int[] f = new int[num_variables+1];
		int[] o = new int[num_variables];
		int[] m = new int[num_variables];
		int[] t = new int[num_variables];
		LogDouble mult = LogDouble.ONE;
		int address=0;

		// Init variables for graycoding
		for(int i=0;i<num_variables;i++)
		{
			a[i]=0;
			f[i]=i;
			o[i]=1;
			t[i]=0;
			m[i]=variables.get(i).getDomainSize();
			variables.get(i).setTempValue(i);
		}
		for(int i=0;i<num_functions;i++)
			c[i]=0;
		f[num_variables]=num_variables;
		for(int i=0;i<num_functions;i++)
		{
			int multiplier = 1;
			assert(functions.get(i) != null);
			for(int j=0;j<functions.get(i).getVariables().size();j++)
			{
				g.get(functions.get(i).getVariables().get(j).getTempValue()).add(new Pair<Integer,Integer>(i,multiplier));
				multiplier *= functions.get(i).getVariables().get(j).getDomainSize();
			}
			
			if(!functions.get(i).table.isEmpty())
			{
				mult = mult.multiply(functions.get(i).table.get(0));
			}
		}
		
		int multiplier1 = 1;
		//cout<<"mult here\n";
		for(int i=0;i<num_marg_variables;i++)
		{
			t[marg_variables.get(i).getTempValue()] = multiplier1;
			multiplier1 *= marg_variables.get(i).getDomainSize();
		}
		//cout<<"mult initing log function\n";
		//Gray  code algorithm
		//Initialize LogFunction
		Function out_function = new Function();
		out_function.variables = marg_variables;
		out_function.table = new ArrayList<LogDouble> (Variable.getDomainSize(marg_variables));
		for (int i = 0; i < out_function.getFunctionSize(); i++) {
			out_function.table.add(LogDouble.ZERO);
		}
		//cout<<"Log function inited\n";
		
		while(true)
		{
			//cout<<address<<endl;
			// Step 1: Visit
			out_function.setTableEntry(mult.add(out_function.table.get(address)), address);
			
			// Step 2: Choose j
			int j=f[0];
			f[0]=0;
			
			if(j==num_variables) break;
			int old_aj=a[j];
			a[j]=a[j]+o[j];
			if(a[j]==0 || a[j]==(m[j]-1))
			{
				o[j]=-o[j];
				f[j]=f[j+1];
				f[j+1]=j+1;
			}
			if(!mult.isZero())
			for(int i=0;i<g.get(j).size();i++)
			{
				int index=g.get(j).get(i).getFirst();
				int multiplier = g.get(j).get(i).getSecond();
				mult = mult.divide(functions.get(index).table.get(c[index]));
				c[index]-=multiplier*old_aj;
				c[index]+=multiplier*a[j];
				mult = mult.multiply(functions.get(index).table.get(c[index]));
			}
			else
			{
				for(int i=0;i<g.get(j).size();i++)
				{	
					int index=g.get(j).get(i).getFirst();
					int multiplier=g.get(j).get(i).getSecond();
					c[index]-=multiplier*old_aj;
					c[index]+=multiplier*a[j];
				}
				mult=LogDouble.ONE;
				for(int i=0;i<num_functions;i++)
					mult = mult.multiply(functions.get(i).table.get(c[i]));
			}
			if(t[j]>0)
			{
				address-=t[j]*old_aj;
				address+=t[j]*a[j];
			}
		}
		//TODO: Do we need to normalize?
//		if(normalize)
//			out_function.normalize();
//		out_function.toLogTable();
		//cout<<"mult done\n";
		
		return out_function;
	}	
}
