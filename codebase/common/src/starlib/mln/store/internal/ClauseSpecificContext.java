package starlib.mln.store.internal;

import java.util.ArrayList;
import java.util.List;

import starlib.gm.core.Variable;

public class ClauseSpecificContext {
	
	private int id;
	
//	private List<Variable> instantiatedVars;

//	private List<Variable> unInstantiatedVars;
//	private int noOfInstantiatedVars;
	
	private List<Variable> variables;
	
	private List<Integer> values;
	
	public ClauseSpecificContext(int id) {
		this.id = id;
	}
	
	public int getId() {
		return id;
	}

	public void setVariables(List<Variable> variables) {
		this.variables = variables;
		values = new ArrayList<>(variables.size());
		for (int i = 0; i < variables.size(); i++) {
			values.add(null);
		}
	}
	
	public List<Variable> getVariables() {
		return variables;
	}
	
	public void saveEvidence() {
		for (Variable variable : variables) {
			values.set(variable.getId(), variable.getOnlyValue());
		}
	}

	public void resetEvidence() {
		for (Variable variable : variables) {
			variable.setValue(values.get(variable.getId()));
		}
	}
	
}
