package starlib.gm.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CPT extends Function {
	
	private List<Variable> conditionalVariables = new ArrayList<Variable>();
	
	private Variable marginalVariable;
	
	public CPT() {
		
	}
	
	public CPT(List<Variable> conditionalVariables, Variable marginalVariable) {
		super(conditionalVariables);
		variables.add(marginalVariable);
		Collections.sort(variables);
		
		this.conditionalVariables.addAll(conditionalVariables);
		this.marginalVariable = marginalVariable;
		
		int tableSize = this.getFunctionSize();
		table = new ArrayList<LogDouble>(tableSize);
		for (int i = 0; i < tableSize; i++) {
			table.add(LogDouble.ZERO);
		}
	}
	
	public CPT(Function function, Variable marginalVariable, List<Variable> conditionalVariables)
	{
		super(function.getVariables());
		
		this.conditionalVariables.addAll(conditionalVariables);
		this.marginalVariable = marginalVariable;

		// TODO Assertion ignored
		
//		table = function.getTable();
		
		/*vector<Variable> test_vars;
		do_set_intersection(function.variables(),cond_variables_,test_vars,less_than_comparator_variable);
		assert((int)test_vars.size()==(int)cond_variables_.size());
		do_set_difference(function.variables(),cond_variables_,test_vars,less_than_comparator_variable);
		assert((int) test_vars.size() >0);
		assert(test_vars[0]->id()==marg_variable_->id());*/
		
		int cond_num_values = Variable.getDomainSize(conditionalVariables);
		table = new ArrayList<LogDouble>(function.getTable().size());
		for(int i=0; i<cond_num_values;i++)
		{
			Variable.setAddress(conditionalVariables,i);
			LogDouble norm_const = LogDouble.ZERO;
			
			for(int j=0;j<marginalVariable.getDomainSize();j++)
			{
				marginalVariable.setAddressValue(j);
				int address = Variable.getAddress(variables);
				norm_const = norm_const.add(function.getTable().get(address));
			}
			
			for(int j=0; j<marginalVariable.getDomainSize();j++)
			{
				marginalVariable.setAddressValue(j);
				int address = Variable.getAddress(variables);
				this.setTableEntry(function.getTable().get(address).divide(norm_const), address);
			}
		}
	}

	public List<Variable> getConditionalVariables() {
		return conditionalVariables;
	}

	public void setConditionalVariables(List<Variable> conditionalVariables) {
		this.conditionalVariables.addAll(conditionalVariables);
	}

	public Variable getMarginalVariable() {
		return marginalVariable;
	}

	public void setMarginalVariable(Variable marginalVariable) {
		this.marginalVariable = marginalVariable;
	}
	
	
}
