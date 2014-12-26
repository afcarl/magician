package starlib.gm.core;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class GraphicalModel {
	
	public static final String TYPE_BAYES = "BAYES";
	
	public static final String TYPE_MARKOV = "MARKOV";
	
	private List<Variable> variables;
	
	private List<Function> functions;
	
	private String type;
	
	private List<VariableValuePair> evidence = new ArrayList<VariableValuePair>();
	
	private LogDouble globalConstant = LogDouble.ONE;
	
	public List<Variable> getVariables() {
		return variables;
	}
	
	public void setVariables(List<Variable> variables) {
		this.variables = variables;
	}
	
	public List<Function> getFunctions() {
		return functions;
	}
	
	public void setFunctions(List<Function> functions) {
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
	
	public void readUAI08(String uaiFIle) throws IOException {
		Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(uaiFIle))));
		
		type = scanner.next();
		
		if(type.equalsIgnoreCase(TYPE_BAYES)) {
			
			int varCount = scanner.nextInt();
			variables = new ArrayList<Variable>();
			
			for (int i = 0; i < varCount; i++) {
				
				int domainSize = scanner.nextInt();
				
				variables.add(new Variable(i,domainSize));
				
			}
			
			int functionCount = scanner.nextInt();
			Map<Integer, List<Variable>> parents = new HashMap<Integer, List<Variable>>();
			List<Integer> functionOrder = new ArrayList<Integer>();
			
			for (int i = 0; i < functionCount; i++) {
				int parentCount = scanner.nextInt();
				parentCount--;
				
				List<Variable> currentParent = new ArrayList<Variable>();
				for (int j = 0; j < parentCount; j++) {
					currentParent.add(variables.get(scanner.nextInt()));
				}
				int var = scanner.nextInt();
				functionOrder.add(var);
				parents.put(var,currentParent);
			}

			functions = new ArrayList<Function>(functionCount);
			for (int i = 0; i < functionCount; i++) {
				functions.add(null);
			}
			
			for (int i = 0; i < functionCount; i++) {
				int var = functionOrder.get(i);
				int numOfProbabilities = scanner.nextInt();
				
				CPT cpt = new CPT(parents.get(var), variables.get(var));
				functions.set(var, cpt);
				assert(numOfProbabilities == functions.get(var).getFunctionSize());
				
				int condVarDomainSize = Variable.getDomainSize(parents.get(var));
				
				for (int j = 0; j < condVarDomainSize; j++) {
					Variable.setAddress(cpt.getConditionalVariables(), j);
					
					for (int k = 0; k < cpt.getMarginalVariable().getDomainSize(); k++) {
						cpt.getMarginalVariable().setAddressValue(k);
						int address = Variable.getAddress(cpt.getVariables());
						
						double value = scanner.nextDouble();
						cpt.setTableEntry(new LogDouble(value), address);
						
					}
				}
				
				Collections.sort(cpt.getConditionalVariables());
			}
		} else if (type.equalsIgnoreCase(TYPE_MARKOV)) {
			
			int varCount = scanner.nextInt();
			variables = new ArrayList<Variable>();
			
			for (int i = 0; i < varCount; i++) {
				
				int domainSize = scanner.nextInt();
				
				variables.add(new Variable(i,domainSize));
				
			}
			
			int functionCount = scanner.nextInt();
			Map<Integer, List<Variable>> scope = new HashMap<Integer, List<Variable>>();
			
			for (int i = 0; i < functionCount; i++) {
				int functionVarCount = scanner.nextInt();
				
				List<Variable> scopedVars = new ArrayList<Variable>();
				for (int j = 0; j < functionVarCount; j++) {
					scopedVars.add(variables.get(scanner.nextInt()));
				}
				scope.put(i,scopedVars);
			}

			functions = new ArrayList<Function>(functionCount);
			for (int i = 0; i < functionCount; i++) {
				functions.add(null);
			}
			
			for (int i = 0; i < functionCount; i++) {
				int var = i;
				int numOfProbabilities = scanner.nextInt();
				
				Function function = new Function(scope.get(var));
				functions.set(var, function);
				
				Collections.sort(function.getVariables());
				
				int functionSize = function.getFunctionSize();
				assert(numOfProbabilities == functionSize);
				
				for (int j = 0; j < functionSize; j++) {
					Variable.setAddress(function.getVariables(), j);
					int address = Variable.getAddress(function.getVariables());

					double value = scanner.nextDouble();
					function.setTableEntry(new LogDouble(value), address);

				}
				
			}
			
		}
		scanner.close();
	}
	
	public void readEvidence(String evidanceFile) throws IOException {
		Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(evidanceFile))));

		int num_evidence = scanner.nextInt();
		for (int i = 0; i < num_evidence; i++) {
			int var = scanner.nextInt();
			int val = scanner.nextInt();
			
			variables.get(var).setValue(val);
			evidence.add(new VariableValuePair(var, val));
		}
		System.out.println("Evidence read");
		scanner.close();
		
		List<Function> toRemove = new ArrayList<Function>();
		
		for (Function function : functions) {
			function.removeEvidence(evidence);
			if(function.isEmpty()) {
				globalConstant = globalConstant.multiply(function.getConstantValue());
				toRemove.add(function);
			}
		}
		
		functions.removeAll(toRemove);

	}
}
