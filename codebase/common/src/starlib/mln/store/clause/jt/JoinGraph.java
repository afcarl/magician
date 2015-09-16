package starlib.mln.store.clause.jt;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import starlib.gm.core.Variable;
import starlib.mln.store.StoreParams;
import starlib.mln.store.clause.CompiledStructure;
import starlib.mln.store.internal.IntFunction;
import starlib.mln.store.internal.RandomizedMinFillHeuristic;

class JGHelperFunction {
	List<Variable> variables;
	int parent_id;
	int functionId;
	JGHelperFunction(List<Variable> variables_, int id_, int func_id_) {
		variables = variables_; parent_id = id_;
		functionId = func_id_ ;
	}
};



public class JoinGraph implements CompiledStructure {

	private static final int INVALID_VALUE = -1;

	List<Variable> variables;
	List<IntFunction> functions;
	List<IntFunction>  marginals;

	JGNodeInt[] funct_to_node;
	JGNodeInt[] samplingOrder;

	List<JGNodeInt> nodes = new ArrayList<>();
	List<JGEdgeInt> edges = new ArrayList<>();

	int id;

	private int num_iterations_calibration_time;

	private int num_iterations_recalibration_time;

	private int i_bound_;

	public JoinGraph(int id, List<IntFunction> functions) {
		this.id = id;
		this.functions = functions;
		
		// Param init
		this.i_bound_ = StoreParams.I_Bound;
		this.num_iterations_calibration_time = StoreParams.Iterations_for_Calibration;
		this.num_iterations_recalibration_time = StoreParams.Iterations_for_ReCalibration;

		Set<Variable> variableSet = new TreeSet<>();
		for (IntFunction function : functions) {
			variableSet.addAll(function.getVariables());
		}
		this.variables = new ArrayList<>(variableSet);
		int noOfFunctions = functions.size();

		funct_to_node = new JGNodeInt[noOfFunctions];

		RandomizedMinFillHeuristic heuristic = new RandomizedMinFillHeuristic(variables, functions);
		heuristic.selectOrdering();

		//		List<Set<Integer> > clusters = heuristic.getClusters(); 
		List<Integer> order = heuristic.getOrder();

		int[] mapped_order = new int[order.size()];
		for (int i = 0; i < order.size(); i++)
			mapped_order[order.get(i)] = i;

		// Create buckets
		List<List<JGHelperFunction>> buckets = new ArrayList<>(order.size());
		for (int i = 0; i < order.size(); i++) {
			buckets.add(new ArrayList<JGHelperFunction>());
		}

		for (int i = 0; i < functions.size(); i++) {
			int min_size = (int) variables.size();
			for (int j = 0; j < functions.get(i).variables.size(); j++) {
				if (mapped_order[functions.get(i).variables.get(j).getId()] < min_size) {
					min_size = mapped_order[functions.get(i).variables.get(j).getId()];
				}
			}
			buckets.get(min_size).add(new JGHelperFunction(functions.get(i).variables, INVALID_VALUE, i));
		}

		//max_cluster_size=i_bound;

		int old_nodes = 0;
		int l_id = 0;

		// Run schematic mini-buckets
		for (int i = 0; i < order.size(); i++) {

			// Form mini-buckets by using the first fill rule
			for (int j = 0; j < buckets.get(i).size(); j++) {
				boolean found = false;
				int loc = INVALID_VALUE;
				for (int k = old_nodes; k < nodes.size(); k++) {

					Set<Variable> temp_variables = new TreeSet<>(buckets.get(i).get(j).variables);
					temp_variables.addAll(nodes.get(k).variables());

					if ((int) temp_variables.size() <= i_bound_) {
						found = true; loc = k; 
						break;
					}
				}
				// If not found create a mini-bucket
				if (!found) {
					nodes.add(new JGNodeInt());
					nodes.get(nodes.size() - 1).setId(l_id);
					l_id++;
					nodes.get(nodes.size() - 1).variables().addAll(buckets.get(i).get(j).variables);
					if (buckets.get(i).get(j).parent_id != INVALID_VALUE) {
						JGEdgeInt edge = new JGEdgeInt(nodes.get(nodes.size() - 1), nodes.get(buckets.get(i).get(j).parent_id));

						// Edge variables are set intersection of node variables
						Set<Variable> edgeVariables = new TreeSet<>(nodes.get(nodes.size() - 1).variables());
						edgeVariables.retainAll(nodes.get(buckets.get(i).get(j).parent_id).variables());
						edge.variables().addAll(edgeVariables);

						nodes.get(nodes.size() - 1).edges().add(edge);
						nodes.get(buckets.get(i).get(j).parent_id).edges().add(edge);
						edges.add(edge);

					} else {
						//cout<<"F2\n";
						int functionId = buckets.get(i).get(j).functionId;
						if(functionId != INVALID_VALUE) {
							nodes.get(nodes.size() - 1).addFunction(functions.get(functionId));
							funct_to_node[functionId] = nodes.get(nodes.size() - 1);
						} else {
							nodes.get(nodes.size() - 1).addFunction(null);
						}
					}
				} else {

					nodes.get(loc).variables().addAll(buckets.get(i).get(j).variables); //TODO: Sort??

					if (buckets.get(i).get(j).parent_id != INVALID_VALUE) {
						JGEdgeInt edge = new JGEdgeInt(nodes.get(loc), nodes.get(buckets.get(i).get(j).parent_id));

						Set<Variable> edgeVariables = new TreeSet<>(nodes.get(loc).variables());
						edgeVariables.retainAll(nodes.get(buckets.get(i).get(j).parent_id).variables());
						edge.variables().addAll(edgeVariables);

						nodes.get(loc).edges().add(edge);
						nodes.get(buckets.get(i).get(j).parent_id).edges().add(edge);
						edges.add(edge);

					} else {
						//cout<<"F4\n";
						int functionId = buckets.get(i).get(j).functionId;
						if(functionId != INVALID_VALUE) {
							nodes.get(loc).addFunction(functions.get(functionId));
							funct_to_node[functionId] = nodes.get(loc);
						} else {
							nodes.get(loc).addFunction(null);
						}
					}

				}
			}
			List<Variable> curr_variable = new ArrayList<>();
			curr_variable.add(variables.get(order.get(i)));

			// Connect the newly created nodes to each other
			for (int j = old_nodes; j < (int) nodes.size() - 1; j++) {
				JGEdgeInt edge = new JGEdgeInt(nodes.get(j), nodes.get(j + 1));
				edge.variables().addAll(curr_variable);

				nodes.get(j).edges().add(edge);
				nodes.get(j + 1).edges().add(edge);
				edges.add(edge);
			}

			if (i < (int) order.size() - 1) {
				for (int j = old_nodes; j < nodes.size(); j++) {

					// do_set_difference
					List<Variable> temp_variables = new ArrayList<>(nodes.get(j).variables());
					temp_variables.removeAll(curr_variable);

					if (temp_variables.isEmpty())
						continue;
					// Put the node in the appropriate bucket
					int min_size = (int) variables.size();
					for (int k = 0; k < temp_variables.size(); k++) {

						if (min_size > mapped_order[temp_variables.get(k).getId()]) {
							min_size = mapped_order[temp_variables.get(k).getId()];
						}
					}
					buckets.get(min_size).add(new JGHelperFunction(temp_variables, j, INVALID_VALUE));
				}
			}
			old_nodes = (int) nodes.size();
		}

		samplingOrder = new JGNodeInt[nodes.size()];
		for (int i = 0; i < samplingOrder.length; i++) {
			samplingOrder[i] = nodes.get(i);
		}
	}


	@Override
	public void calibrate() {
		//Initialize
		for (int i = 0; i < nodes.size(); i++)
			nodes.get(i).initialize();

		for (int i = 0; i < num_iterations_calibration_time; i++) {
			//			if (i>=10 && convergence_test())
			//				break;

			for (int j = 0; j < nodes.size(); j++) {
				for (int k = 0; k < nodes.get(j).edges().size(); k++) {
					if (nodes.get(j).edges().get(k).node1().id() == nodes.get(j).id())
						nodes.get(j).edges().get(k).sendMessage1to2();
					else
						nodes.get(j).edges().get(k).sendMessage2to1();
				}
			}
		}

		for(int i=0; i < nodes.size(); i++) {
			nodes.get(i).computeZ();
		}
	}

	//	boolean convergence_test()
	//	{
	//		List<IntFunction> old_marginals = marginals;
	//		updateMarginals();
	//		if (old_marginals.empty()){
	//			return false;
	//		}
	//		double error = 0.0;
	//		for(int i=0; i < marginals.size(); i++){
	//			for(int j=0;j<marginals[i].table().size();j++){
	//				error+=fabs(marginals[i].table()[j].value()-old_marginals[i].table()[j].value());
	//			}
	//		}
	//		error/=(long double)marginals.size();
	////		cout<<"Error = "<<error<<endl;
	//		if (error < (long double)0.00001){
	//			return true;
	//		}
	//		return false;
	//	}

	public static void main(String[] args) {
		Variable a  = new Variable(0, 2);
		Variable b  = new Variable(1, 2);
		Variable c  = new Variable(2, 2);
		Variable d  = new Variable(3, 2);
		Variable e  = new Variable(4, 2);
		Variable f  = new Variable(5, 2);
		Variable g  = new Variable(6, 2);

		// Scopes
		List<Variable> scopeA = new ArrayList<>();
		scopeA.add(a);

		List<Variable> scopeBA = new ArrayList<>();
		scopeBA.add(b);
		scopeBA.add(a);

		List<Variable> scopeCBA = new ArrayList<>();
		scopeCBA.add(c);
		scopeCBA.add(b);
		scopeCBA.add(a);

		List<Variable> scopeBD = new ArrayList<>();
		scopeBD.add(b);
		scopeBD.add(d);

		List<Variable> scopeFCD = new ArrayList<>();
		scopeFCD.add(f);
		scopeFCD.add(c);
		scopeFCD.add(d);

		List<Variable> scopeFEB = new ArrayList<>();
		scopeFEB.add(f);
		scopeFEB.add(e);
		scopeFEB.add(b);

		List<Variable> scopeFEG = new ArrayList<>();
		scopeFEG.add(f);
		scopeFEG.add(e);
		scopeFEG.add(g);

		IntFunction pA   = new IntFunction(scopeA);
		IntFunction pBA  = new IntFunction(scopeBA);
		IntFunction pCBA = new IntFunction(scopeCBA);
		IntFunction pBD  = new IntFunction(scopeBD);
		IntFunction pFCD = new IntFunction(scopeFCD);
		IntFunction pFEB = new IntFunction(scopeFEB);
		IntFunction pFEG = new IntFunction(scopeFEG);

		List<IntFunction> functions = new ArrayList<>();
		functions.add(pA);
		functions.add(pBA);
		functions.add(pCBA);
		functions.add(pBD);
		functions.add(pFCD);
		functions.add(pFEB);
		functions.add(pFEG);

		JoinGraph jg = new JoinGraph(0, functions);
		jg.calibrate();
		jg.reCalibrateAll();
		jg.sample();
		jg.computeDeltaZ(0, 0);
		jg.getZ();
	}

	@Override
	public List<Variable> getVariables() {
		return variables;
	}

	@Override
	public void reCalibrate(int changedFunctionId, int address) {
		this.reCalibrateAll();
	}

	@Override
	public void reCalibrateAll() {
		for (int i = 0; i < num_iterations_recalibration_time; i++) {
			for (int j = 0; j < nodes.size(); j++) {
				for (int k = 0; k < nodes.get(j).edges().size(); k++) {
					if (nodes.get(j).edges().get(k).node1().id() == nodes.get(j).id())
						nodes.get(j).edges().get(k).updateMessage1to2(false);
					else
						nodes.get(j).edges().get(k).updateMessage2to1(false);
				}
			}
		}

		for(int i=0; i < nodes.size(); i++) {
			nodes.get(i).updateZ(false);
		}
	}

	@Override
	public double getZ() {
		return samplingOrder[0].getZ();
	}

	@Override
	public double computeDeltaZ(int changedFunctionId, int address) {
		IntFunction changedFunction = functions.get(changedFunctionId);
		
		Variable.instantiateVariables(changedFunction.variables, address);
		
		double partFn = funct_to_node[changedFunctionId].computeDeltaZ(changedFunction, address);
		
		Variable.freeVariables(changedFunction.variables);
		return partFn;
	}

	@Override
	public void sample() {
		// First clear address value of all variables
		for (Variable currentVar : variables) {
			currentVar.setAddressValue(null);
		}
		
		for(int i=0; i < samplingOrder.length; i++) {
			samplingOrder[i].sample();
		}
	}

	@Override
	public void reCalibrate(List<Integer> changedFunctionIds, int address) {
		this.reCalibrateAll();
	}

}
