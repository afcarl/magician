package starlib.mln.infer.store.internal;

import java.util.ArrayList;
import java.util.List;

import starlib.gm.core.LogDouble;
import starlib.gm.core.Variable;
import starlib.gm.core.VariableValuePair;

public class IntGraphMod {
	
	public static final String TYPE_BAYES = "BAYES";
	
	public static final String TYPE_MARKOV = "MARKOV";
	
	private List<Variable> variables;
	
	private List<IntFunction> functions;
	
	private String type;
	
	private List<VariableValuePair> evidence = new ArrayList<VariableValuePair>();
	
	private LogDouble globalConstant = LogDouble.ONE;
	
	public List<Variable> getVariables() {
		return variables;
	}
	
	public void setVariables(List<Variable> variables) {
		this.variables = variables;
	}
	
	public List<IntFunction> getFunctions() {
		return functions;
	}
	
	public void setFunctions(List<IntFunction> functions) {
		this.functions = functions;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public List<VariableValuePair> getEvidence() {
		return evidence;
	}
	
	public void setEvidence(List<VariableValuePair> evidence) {
		this.evidence = evidence;
	}
	
	public LogDouble getGlobalConstant() {
		return globalConstant;
	}
	
	public void setGlobalConstant(LogDouble globalConstant) {
		this.globalConstant = globalConstant;
	}
}
