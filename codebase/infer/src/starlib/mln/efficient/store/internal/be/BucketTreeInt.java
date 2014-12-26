package starlib.mln.efficient.store.internal.be;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import starlib.gm.core.Variable;
import starlib.mln.efficient.store.internal.CompiledStructure;
import starlib.mln.efficient.store.internal.GlobalContext;
import starlib.mln.efficient.store.internal.IntFunction;
import starlib.mln.efficient.store.internal.MinDegreeHeuristic;

public class BucketTreeInt implements CompiledStructure {

	private List<Variable> variables;
	private List<IntFunction> functions;
	
	private int[] function_to_bucket;
	
	private List<Variable> ordering;
	
	private List<IntBucket> buckets;
	
	private IntBucket finalBucket;
	
	private Random random = new Random();
	
	int id;
	
	public BucketTreeInt(int id, List<IntFunction> functions_) {
		this.id = id;
		this.functions = functions_;
		function_to_bucket = new int[functions.size()];

		Set<Variable> variableSet = new TreeSet<>();
		for (IntFunction function : functions) {
			variableSet.addAll(function.getVariables());
		}
		this.variables = new ArrayList<>(variableSet);
//		int variablesSize = this.variables.size();
		
		Set<Variable> variables = new HashSet<>();
		for (IntFunction function : functions) {
			variables.addAll(function.getVariables());
		}
		
		MinDegreeHeuristic heuristic = new MinDegreeHeuristic(new ArrayList<>(variables), functions);
		ordering = heuristic.selectOrdering();
		
		buckets  = new ArrayList<IntBucket>(ordering.size());
		
		for (Variable bucketVariable : ordering) {
			buckets.add(new IntBucket(bucketVariable));
		}

		// Place functions in respective buckets
		for (int i = 0; i < functions.size(); i++) {
			IntFunction factor = functions.get(i);
			
			for (int j = 0; j < ordering.size(); j++) {
				if(factor.contains(ordering.get(j).getId())) {
					function_to_bucket[i] = j;
					buckets.get(j).addFunction(factor);
					break;
				}
			}
		}
		
	}
	

	@Override
	public List<Variable> getVariables() {
		return variables;
	}

	@Override
	public void calibrate() {
		// Form channels/edges
		for (int i = 0; i < ordering.size(); i++) {

			IntBucket bi  = buckets.get(i);
			
			if(bi.size() == 0) {
				continue;
			}

			IntFunction resultant = bi.eliminate();
			
			if(resultant.isEmpty()) {
				BucketChannelInt channel = new BucketChannelInt(bi, finalBucket);
				channel.setMessage(resultant);
			}
			
			for (int j = i + 1; j < ordering.size(); j++) {
				if(resultant.contains(ordering.get(j).getId())) {
					BucketChannelInt channel = new BucketChannelInt(bi, buckets.get(j));
					channel.setMessage(resultant);
					break;
				}
			}
		}
		
		finalBucket.computeZ();
	}

	@Override
	public void reCalibrate(int changedFunctionId, int address) {
		IntFunction changedFunction = functions.get(changedFunctionId);
		
		int bucketId = function_to_bucket[changedFunctionId];
		for (int i = bucketId; i < ordering.size(); i++) {

			IntBucket bi  = buckets.get(i);
			
			if(bi.size() == 0) {
				continue;
			}

			bi.reCalibrate();
		}
		
		finalBucket.updateZ();
		freeVariables(changedFunction.variables);
		
	}

	@Override
	public void reCalibrateAll() {
		for (int i = 0; i < ordering.size(); i++) {

			IntBucket bi  = buckets.get(i);
			
			if(bi.size() == 0) {
				continue;
			}

			bi.reCalibrate();
		}
		
		finalBucket.updateZ();
			
	}

	@Override
	public double getZ() {
		return finalBucket.getZ();
	}

	@Override
	public double computeDeltaZ(int changedFunctionId, int address) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void sample() {
		// TODO Auto-generated method stub
		
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
