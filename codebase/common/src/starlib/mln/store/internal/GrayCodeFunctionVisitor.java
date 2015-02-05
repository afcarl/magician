package starlib.mln.store.internal;

import java.util.List;

import starlib.gm.core.Variable;

public abstract class GrayCodeFunctionVisitor {
	
	private List<Variable> variables;
	private int numOfVariables;

	private int[] functionAdresses;
	
	private int[] n;
	private int[] g;
	private int[] u;

	private int flippedIndex = 0;  // The last flipped index for Gray code
	private int g_plus_u; // Temporary variable to compute Gray code
	
	private int[][] projectedFunctionMultipliers;
	
	public GrayCodeFunctionVisitor(List<Variable> variables, List<IntFunction> functions) {
		
		this.variables = variables;
		numOfVariables = variables.size();
		
		int num_functions = functions.size();

		
		// Initialize variables for gray code implementation
		n = new int[numOfVariables + 1]; /* the maximum for each digit */
		g = new int[numOfVariables + 1]; /* the Gray code */
		u = new int[numOfVariables + 1]; /* +1 or −1 */
		
		// Addresses corresponding to different functions
		functionAdresses = new int[num_functions+1];
		Variable.setAddress(variables, 0); // Initialize starting address as zero
		for (int i = 0; i < num_functions; i++) {
			functionAdresses[i] = functions.get(i).getAddress();
		}

		// Multipliers corresponding to different functions projected according to variable ordering in allVar
		projectedFunctionMultipliers = new int[num_functions + 1][numOfVariables];
		for (int i = 0; i < num_functions; i++) {
			IntFunction function = functions.get(i);
			for (int j = 0; j < variables.size(); j++) {
				Variable variable = variables.get(j);
				int variableIndex = function.variables.indexOf(variable);
				if(variableIndex == -1)
					projectedFunctionMultipliers[i][j] = 0; // Variable not found. No effect
				else 
					projectedFunctionMultipliers[i][j] = function.multipliers.get(variableIndex); // Project pre-computed multiplier
			}
		}
		
	}
	
	public int getFunctionAddress(int functionIndex) {
		return functionAdresses[functionIndex];
	}
	
	public void visit() {
		
		for (int i = 0; i < numOfVariables; i++) {
			g[i] = 0;
			u[i] = 1;
			n[i] = variables.get(i).getDomainSize();  // Domain size of each variables
		}
		
		g[numOfVariables] = 0;
		u[numOfVariables] = 1;
		if(numOfVariables > 0)
			n[numOfVariables] = n[numOfVariables-1];
		else
			n[numOfVariables] = 1; 
		
		while (g[numOfVariables] == 0) {
			
			this.visitAddress();
			
			if(numOfVariables == 0) {
				// All addresses visited
				return;
			}
			
			flippedIndex = 0; /* enumerate next Gray code */
			g_plus_u = g[0] + u[0];
			while ((g_plus_u >= n[flippedIndex]) || (g_plus_u < 0)) {
				u[flippedIndex] = -u[flippedIndex];
				flippedIndex++;
				g_plus_u = g[flippedIndex] + u[flippedIndex];
			}
			g[flippedIndex] = g_plus_u;

			// Update addresses
			for (int i = 0; g[numOfVariables] == 0 && i < functionAdresses.length; i++) {
				functionAdresses[i] += projectedFunctionMultipliers[i][flippedIndex] * u[flippedIndex];
			}
		}
	}
	
	protected abstract void visitAddress();

}
