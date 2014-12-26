package starlib.mln.efficient.store.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import starlib.gm.core.Variable;

public class MinDegreeHeuristic {
	
	private Random random = new Random();
	
	private int width = 0;
	
	private List<Variable> variables;
	
	private List<IntFunction> functions;
	
	public MinDegreeHeuristic(List<Variable> variables, List<IntFunction> functions) {
		this.variables = variables;
		this.functions = functions;
	}
	
	public int getWidth() {
		return width;
	}
	
	public List<Variable> selectOrdering() {
		
		List<Variable> ordering = new ArrayList<Variable>();
		
		Map<Variable, Set<Variable>> interactionGraph = new HashMap<Variable, Set<Variable>>();
		for (Variable variable : variables) {
			
			if(variable.isInstantiated())
				continue;
			
			interactionGraph.put(variable, new HashSet<Variable>());
		}
		
		// Create an adjacency Matrix for computing min-degree heuristic
		for (IntFunction function : functions) {
			
			for (int i = 0; i < function.getVariables().size(); i++) {
				Variable vi = function.getVariables().get(i);
				if(vi.isInstantiated())
					continue;
				
				for (int j = i+1; j < function.getVariables().size(); j++) {
					Variable vj = function.getVariables().get(j);
					if(vj.isInstantiated())
						continue;

					interactionGraph.get(vi).add(vj);
					interactionGraph.get(vj).add(vi);
				}
			}
		}
		
		width = 0;
		
		while(true) {

			int minDegree = Integer.MAX_VALUE;
			Variable minDegVar = null;
			for (Variable variable : interactionGraph.keySet()) {
				
				int degree = interactionGraph.get(variable).size();
				
				if(degree < minDegree) {
					minDegree = degree;
					minDegVar = variable;
				}
				
				if(degree == minDegree && random.nextDouble() > 0.5) {
					minDegree = degree;
					minDegVar = variable;
				}
				
			}
			
			if(minDegree > width) {
				width = minDegree;
			}
			
			List<Variable> neighbors = new ArrayList<Variable>();
			neighbors.addAll(interactionGraph.get(minDegVar));

			for (int i = 0; i < neighbors.size(); i++) {
				interactionGraph.get(neighbors.get(i)).remove(minDegVar);

				for (int j = i+1; j < neighbors.size(); j++) {
					interactionGraph.get(neighbors.get(i)).add(neighbors.get(j));
					interactionGraph.get(neighbors.get(j)).add(neighbors.get(i));
				}
			}
			ordering.add(minDegVar);
			interactionGraph.remove(minDegVar);
			
			if(interactionGraph.isEmpty())
				break;
		}
		
//		System.out.println("The ordering selected is:");
//		for (Variable variable : ordering) {
//			System.out.print(variable.getId() + ", ");
//		}
//		System.out.println();
		
		return ordering;
			
	}
	


}
