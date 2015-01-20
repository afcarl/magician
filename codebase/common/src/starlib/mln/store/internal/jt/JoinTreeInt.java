package starlib.mln.store.internal.jt;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import starlib.gm.core.Variable;
import starlib.mln.store.internal.CompiledStructure;
import starlib.mln.store.internal.GlobalContext;
import starlib.mln.store.internal.IntFunction;
import starlib.mln.store.internal.RandomizedMinFillHeuristic;

public class JoinTreeInt implements CompiledStructure {

	List<Variable> variables;
	List<IntFunction> functions;
	IntFunction[]  marginals;
	
	protected int noOfEffectiveNodes;
	JGNodeInt[] nodes;
	
	JGNodeInt[] funct_to_node;
	JGNodeInt[] var_to_node;
	JGNodeInt[] samplingOrder;
	
	List<JGEdgeInt> edges;
	List<JGEdgeInt> message_order;
	
	int id;
	
	/* (non-Javadoc)
	 * @see starlib.mln.efficient.store.internal.jt.CompiledStructure#getVariables()
	 */
	@Override
	public List<Variable> getVariables() {
		return variables;
	}
	
	public JoinTreeInt(int id, List<IntFunction> functions)
	{
		this.id = id;
		this.functions = functions;
		
		Set<Variable> variableSet = new TreeSet<>();
		for (IntFunction function : functions) {
			variableSet.addAll(function.getVariables());
		}
		this.variables = new ArrayList<>(variableSet);
		int variablesSize = this.variables.size();
		int noOfFunctions = functions.size();
		
		edges = new ArrayList<>();
		message_order = new ArrayList<>();
		
		RandomizedMinFillHeuristic heuristic = new RandomizedMinFillHeuristic(variables, functions);
		heuristic.selectOrdering();
		
		List<Set<Integer> > clusters = heuristic.getClusters(); 
		List<Integer> order = heuristic.getOrder();
		
		int[] var_in_pos = new int[order.size()];
		for(int i=0;i<var_in_pos.length;i++)
			var_in_pos[order.get(i)] = i;

		noOfEffectiveNodes = variablesSize;
		nodes = new JGNodeInt[variablesSize];
		var_to_node = new JGNodeInt[variablesSize];
		samplingOrder = new JGNodeInt[variablesSize];

		funct_to_node = new JGNodeInt[noOfFunctions];
		
		for(int i=0;i<variablesSize;i++)
		{
			nodes[i] = new JGNodeIntSS();
			var_to_node[order.get(i)] = nodes[i];
		}
		
		//Put the functions in the appropriate nodes
		// First put the functions in the proper buckets
		for(int i=0; i < noOfFunctions; i++)
		{
			int pos = order.size();
			IntFunction function = functions.get(i);
			if(function.getVariables().isEmpty())
			{
				continue;
			}
			for(int j=0;j<function.getVariables().size();j++)
			{
				if(var_in_pos[function.getVariables().get(j).getId()] < pos)
					pos = var_in_pos[function.getVariables().get(j).getId()];
			}
			nodes[pos].addFunction(function);
			funct_to_node[i] = nodes[pos];
		}
		
		boolean[][] adj_matrix = new boolean[clusters.size()][];
		for (int i = 0; i < clusters.size(); i++) {
			adj_matrix[i] = new boolean[clusters.size()];
		}
		int rootNode = heuristic.getMinClusterIndex();

		// Now create the edges and message order
		for (int i = 0; i < clusters.size(); i++) 
		{
			Set<Integer> curr_cluster = clusters.get(i);
			curr_cluster.remove((Integer) order.get(i));  // remove(Integer object) emphasis on Integer
			// Find where to put this cluster
			int pos = order.size();
			for (Integer j : curr_cluster)
			{
				if(var_in_pos[j] < pos)
					pos=var_in_pos[j];
			}

			if(pos != order.size())
			{
				// Create an edge between node[i] and node[pos]
				edges.add(new JGEdgeIntSS(nodes[i],nodes[pos]));
				adj_matrix[i][pos] = true;
				adj_matrix[pos][i] = true;

				Set<Integer> temp = new TreeSet<>(clusters.get(i));
				temp.retainAll(clusters.get(pos)); // Set intersection between clusters[i] and clusters[pos]
				for (Integer j : temp) {
					edges.get(edges.size()-1).variables().add(variables.get(j));
				}

				message_order.add(edges.get(edges.size()-1));
				nodes[i].edges().add(edges.get(edges.size()-1));
				nodes[pos].edges().add(edges.get(edges.size()-1));
			}
		}
		
		GlobalContext.setCurrentContext(id);
		GlobalContext.setVariables(variables);
		
		// Create the DFS tree (sample ordering) using the message ordering
		this.DFS_visit(adj_matrix, rootNode);
	}
	
	private void DFS_visit(boolean[][] adj_matrix, int rootNode) {
		Stack<Integer> stack = new Stack<>();
		int number_of_nodes = adj_matrix.length;
		boolean[] visited = new boolean[number_of_nodes];
		
		int index = 0;
		
		stack.push(rootNode);
		while(!stack.isEmpty()) {
			// get the element from the stack and visit the node i.e.- Store in sample order
			int element = stack.pop();
			samplingOrder[index] = nodes[element];
			index++;
			visited[element] = true; // Change color to black
			
			// Generate children of the node
			for (int i = 0; i < number_of_nodes; i++) {
				if(adj_matrix[element][i] && !visited[i]) {
					stack.push(i);
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see starlib.mln.efficient.store.internal.jt.CompiledStructure#calibrate()
	 */
	@Override
	public void calibrate()
	{
		GlobalContext.setCurrentContext(id);
		for(int i=0; i < noOfEffectiveNodes; i++) {
			nodes[i].initialize();
		}
		
		for(int i=0;i<message_order.size();i++)
			message_order.get(i).sendMessage1to2();
		for(int i=message_order.size()-1;i>-1;i--)
			message_order.get(i).sendMessage2to1();
		
		for(int i=0; i < noOfEffectiveNodes; i++) {
			nodes[i].computeZ();
		}
				
	}
	
	/* (non-Javadoc)
	 * @see starlib.mln.efficient.store.internal.jt.CompiledStructure#reCalibrate(int, int)
	 */
	@Override
	public void reCalibrate(int changedFunctionId, int address)
	{
		IntFunction changedFunction = functions.get(changedFunctionId);
		JGNodeInt affectedNode = funct_to_node[changedFunctionId];
		
		GlobalContext.setCurrentContext(id);
		instantiateVariables(changedFunction.variables, address);
		
		int messageOrderSize = message_order.size();
		boolean start = false;
		
		for(int i=0; i < messageOrderSize; i++) {
			if(!start && message_order.get(i).node1() == affectedNode ){
				start = true;
			}
			if(start)
				message_order.get(i).updateMessage1to2(false);
		}

		start = false;
		
		for(int i = messageOrderSize - 1; i > -1; i--) {
			if(!start && message_order.get(i).node2() == affectedNode ){
				start = true;
			}
			if(start)
				message_order.get(i).updateMessage2to1(false);
		}
		
		for(int i=0; i < noOfEffectiveNodes; i++) {
			nodes[i].updateZ(true);
		}
		
		freeVariables(changedFunction.variables);
		
	}
	
	/* (non-Javadoc)
	 * @see starlib.mln.efficient.store.internal.jt.CompiledStructure#reCalibrateAll()
	 */
	@Override
	public void reCalibrateAll()
	{
		GlobalContext.setCurrentContext(id);
		int messageOrderSize = message_order.size();

		for(int i=0;i<messageOrderSize;i++)
			message_order.get(i).updateMessage1to2(false);

		for(int i=messageOrderSize-1;i>-1;i--)
			message_order.get(i).updateMessage2to1(false);


		for(int i=0; i < noOfEffectiveNodes; i++) {
			nodes[i].updateZ(false);
		}
	}
	
	/* (non-Javadoc)
	 * @see starlib.mln.efficient.store.internal.jt.CompiledStructure#getZ()
	 */
	@Override
	public double getZ() {
		return samplingOrder[0].getZ();
	}
	
	/* (non-Javadoc)
	 * @see starlib.mln.efficient.store.internal.jt.CompiledStructure#computeDeltaZ(int, int)
	 */
	@Override
	public double computeDeltaZ(int changedFunctionId, int address) {
		IntFunction changedFunction = functions.get(changedFunctionId);
		
		GlobalContext.setCurrentContext(id);
		instantiateVariables(changedFunction.variables, address);
		
		double partFn = funct_to_node[changedFunctionId].computeDeltaZ(changedFunction, address);
		
		freeVariables(changedFunction.variables);
		return partFn;
	}
	
	/* (non-Javadoc)
	 * @see starlib.mln.efficient.store.internal.jt.CompiledStructure#sample()
	 */
	@Override
	public void sample() {
		// First clear address value of all variables
		for (Variable currentVar : variables) {
			currentVar.setAddressValue(null);
		}
		
		GlobalContext.setCurrentContext(id);
		GlobalContext.saveEvidence();

		for(int i=0; i < noOfEffectiveNodes; i++) {
			samplingOrder[i].sample();
		}
	}
	
	protected static void instantiateVariables(List<Variable> variables, int address) {
		// Instantiate the variables
		Variable.setAddress(variables, address);
		for (Variable var : variables) {
			var.setValue(var.getValue());
		}
		
		GlobalContext.saveEvidence();
	}
	
	protected static void freeVariables(List<Variable> variables) {
		for (Variable var : variables) {
			var.setAddressValue(null);
			var.setValue(null);
		}
	}
	
}
