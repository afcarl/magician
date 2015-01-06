package starlib.mln.infer.store.internal;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Set;

import starlib.gm.core.Variable;

public class RandomizedMinFillHeuristic {
	
	private Random random = new Random();
	
	private int width = 0;
	
	private List<Variable> variables;
	
	private List<IntFunction> functions;
	
	private double estimate;
	
	private int maxClusterSize;
	
	private int maxClusterIndex;
	
	private int minClusterSize = Integer.MAX_VALUE;
	
	private int minClusterIndex;
	
	private List<Integer> order; 
	
	private List<Set<Integer>> clusters;
	
	public RandomizedMinFillHeuristic(List<Variable> variables, List<IntFunction> functions) {
		this.variables = variables;
		this.functions = functions;
		order = new ArrayList<Integer> (variables.size());
		clusters = new ArrayList<Set<Integer>> (variables.size());
		
		for (int i = 0; i < variables.size(); i++) {
			order.add(null);
			clusters.add(null);
		}
	}
	
	public int getWidth() {
		return width;
	}
	
	public double getEstimate() {
		return estimate;
	}
	
	public int getMaxClusterSize() {
		return maxClusterSize;
	}
	
	public int getMaxClusterIndex() {
		return maxClusterIndex;
	}
	
	public int getMinClusterSize() {
		return minClusterSize;
	}
	
	public int getMinClusterIndex() {
		return minClusterIndex;
	}
	
	public List<Integer> getOrder() {
		return order;
	}
	
	public List<Set<Integer>> getClusters() {
		return clusters;
	}
	
	public void selectOrdering() {
		estimate = 0.0;
		maxClusterSize = 0;
		boolean[][] adj_matrix = new boolean[variables.size()][];

		// Create the interaction graph of the functions in this graphical model - i.e.
		// create a graph structure such that an edge is drawn between variables in the
		// model that appear in the same function
		for (int i = 0; i < variables.size(); i++) {
			adj_matrix[i] = new boolean[variables.size()];
		}
		List<Set<Integer>> graphSet = new ArrayList<>(variables.size());
		for (int i = 0; i < variables.size(); i++) {
			graphSet.add(new LinkedHashSet<Integer>(variables.size()));
		}
		
		boolean[] processed = new boolean[variables.size()];
		for (int i = 0; i < functions.size(); i++) {
			for (int j = 0; j < functions.get(i).getVariables().size(); j++) {
				for (int k = j + 1; k < functions.get(i).getVariables().size(); k++) {
					int a = functions.get(i).getVariables().get(j).getId();
					int b = functions.get(i).getVariables().get(k).getId();
					graphSet.get(a).add(b);
					graphSet.get(b).add(a);
					adj_matrix[a][b] = true;
					adj_matrix[b][a] = true;
				}
			}
		}
		
		List<List<Integer>> graph = new ArrayList<>(variables.size());
		for (int i = 0; i < variables.size(); i++) {
			graph.add(new ArrayList<Integer>(graphSet.get(i)));
		}
		
		
		List<Integer> zero_list = new ArrayList<>();

		// For i = 1 to number of variables in the model
		// 1) Identify the variables that if deleted would add the fewest number of edges to the
		//    interaction graph
		// 2) Choose a variable, pi(i), from among this set
		// 3) Add an edge between every pair of non-adjacent neighbors of pi(i)
		// 4) Delete pi(i) from the interaction graph
		for (int i = 0; i < variables.size(); i++) {
			// Find variables with the minimum number of edges added
			double min = Double.MAX_VALUE;
			int min_id = -1;
			boolean first = true;

			// Flag indicating whether the variable to be removed is from the
			// zero list - i.e. adds no edges to interaction graph when deleted
			boolean fromZeroList = false;

			// Vector to keep track of the ID of each minimum fill variable
			List<Integer> minFillIDs = new ArrayList<>(variables.size());

			// If there are no variables that, when deleted, add no edges...
			if (zero_list.isEmpty()) {

				// For each unprocessed (non-deleted) variable
				for (int j = 0; j < variables.size(); j++) {
					if (processed[j])
						continue;
					double curr_min = 0.0;
					
					ListIterator<Integer> iteratorA = graph.get(j).listIterator(0);
					while (iteratorA.hasNext()) {
						Integer a = iteratorA.next();
						ListIterator<Integer> iteratorB = graph.get(j).listIterator(iteratorA.nextIndex());
						while (iteratorB.hasNext()) {
							int b = iteratorB.next();
							if (!adj_matrix[a][b]) {
								curr_min += (variables.get(a).getDomainSize() * variables.get(b).getDomainSize());
								if (curr_min > min)
									break;
							}
						}
						if (curr_min > min)
							break;
					}
					

					// Store the first non-deleted variable as a potential minimum
					if (first) {
						minFillIDs.add(j);
						min = curr_min;
						first = false;
					} else {
						// If this is a new minimum...
						if (min > curr_min) {
							min = curr_min;
							minFillIDs.clear();
							minFillIDs.add(j);
						}
						// Otherwise, if the number of edges removed is also a minimum, but
						// the minimum is zero
						else if (curr_min < Double.MIN_VALUE) {
							zero_list.add(j);
						}
						// Else if this is another potential min_fill
						else if (min == curr_min) {
							minFillIDs.add(j);
						}
					}
				}
			}
			// Else...delete variables from graph that don't add any edges
			else {
				min_id = zero_list.get(0);
				zero_list.remove(0);
				fromZeroList = true;
			}

			// If not from zero_list, choose one of the variables at random
			// from the set of min fill variables
			if (!fromZeroList) {
				int indexInVector = random.nextInt(minFillIDs.size());
				min_id = minFillIDs.get(indexInVector);
			}

			assert(min_id!=-1);
			order.set(i, min_id);
			// Now form the cluster
			clusters.set(i, new LinkedHashSet<>(graph.get(min_id)));
			clusters.get(i).add(min_id);

			// Triangulate min id and remove it from the graph
			ListIterator<Integer> iteratorA = graph.get(min_id).listIterator(0);
			while (iteratorA.hasNext()) {
				Integer a = iteratorA.next();
				ListIterator<Integer> iteratorB = graph.get(min_id).listIterator(iteratorA.nextIndex());
				while (iteratorB.hasNext()) {
					int b = iteratorB.next();
					if (!adj_matrix[a][b]) {
						adj_matrix[a][b] = true;
						adj_matrix[b][a] = true;
						graph.get(a).add(b);
						graph.get(b).add(a);
					}
				}
			}
			for (Integer a : graph.get(min_id)) {
				graph.get(a).remove((Integer) min_id); // Call remove(Object o) and not remove(int index)
				adj_matrix[a][min_id] = false;
				adj_matrix[min_id][a] = false;
			}
			graph.get(min_id).clear();
			processed[min_id] = true;
		}

		// compute the estimate
		for (int i = 0; i < clusters.size(); i++) {
			if (clusters.get(i).size() > maxClusterSize) {
				maxClusterSize = clusters.get(i).size();
				maxClusterIndex = i;
			}
			
			if (clusters.get(i).size() < minClusterSize) {
				minClusterSize = clusters.get(i).size();
				minClusterIndex = i;
			}
			
			double curr_estimate = 1.0;
			for (Integer j : clusters.get(i)) {
				curr_estimate *= variables.get(j).getDomainSize();
			}
			estimate += curr_estimate;
		}
	}
}
