package starlib.mln.efficient.store.internal.be;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import starlib.gm.core.Variable;
import starlib.mln.efficient.store.internal.IntFunction;
import starlib.mln.efficient.store.internal.MinDegreeHeuristic;

public class IntBucketElimination {
	
	private List<IntFunction> functions;
	
	private List<Variable> ordering;
	
	private MinDegreeHeuristic heuristic;
	
	private List<IntBucket> buckets;
	
	private Random random = new Random();
	
	public IntBucketElimination(List<IntFunction> functions) {
		this.functions = functions;

		Set<Variable> variables = new HashSet<>();
		for (IntFunction function : functions) {
			variables.addAll(function.getVariables());
		}
		heuristic = new MinDegreeHeuristic(new ArrayList<>(variables), functions);
		ordering = heuristic.selectOrdering();
	}
	
	public List<Variable> getOrdering() {
		return ordering;
	}
	
	public double process() {
		
		double prob = 1.0;

		buckets  = new ArrayList<IntBucket>(ordering.size());
		
		for (Variable bucketVariable : ordering) {
			buckets.add(new IntBucket(bucketVariable));
		}

		for (IntFunction factor : functions) {
			
			if(factor.isFullyInstantiated()) {
				prob *= factor.getTableEntry(Variable.getAddress(factor.getVariables()));
			}
			
			for (int i = 0; i < ordering.size(); i++) {
				if(factor.contains(ordering.get(i).getId())) {
					buckets.get(i).addFunction(factor);
					break;
				}
			}
		}
		
		for (int i = 0; i < ordering.size(); i++) {

			IntBucket bi  = buckets.get(i);
			
			if(bi.size() == 0) {
				continue;
			}

			IntFunction resultant = bi.eliminate();
			
			if(resultant.isEmpty()) {
				prob *= resultant.getConstantValue();
			}
			
			for (int j = i + 1; j < ordering.size(); j++) {
				if(resultant.contains(ordering.get(j).getId())) {
					buckets.get(j).addFunction(resultant);
					break;
				}
			}
		}
		
		return prob;
	}
	
	public void sample() {
		if(buckets == null || buckets.isEmpty())
			return;
		
		// First clear address value of all variables
		for(int i=buckets.size()-1;i>-1;i--){
			Variable currentVar = ordering.get(i);
			currentVar.setAddressValue(null);
		}
		
		/* Generate a sample from the buckets */
		for(int i=buckets.size()-1;i>-1;i--){
			Variable currentVar = ordering.get(i);
			if(currentVar.getValue() != null){
				continue;
			}
			
			double[] marginal = new double[currentVar.getDomainSize()];
			
			for (int j=0; j < currentVar.getDomainSize(); j++)
				marginal[j] = 1.0;
			
			for(int j=0; j < buckets.get(i).size();j++)
			{
				//FIXME: Ignoring assertion
//				for(int k=0;k<buckets[i][j]->variables().size();k++)
//				{
//					if (buckets[i][j]->variables()[k]->id()==curr_var)
//						continue;
//					assert(buckets[i][j]->variables()[k]->value()!=INVALID_VALUE);
//				}
				
				for(int k=0; k < marginal.length; k++)
				{
					int entry;
					currentVar.setAddressValue(k);
					entry = Variable.getAddress(buckets.get(i).getFunction(j).getVariables());
					marginal[k] *= buckets.get(i).getFunction(j).getTableEntry(entry);
				}
			}
			Double norm_const = 0.0;
			for(int j=0; j < marginal.length; j++){
				norm_const += marginal[j];
			}
			double rand_num = random.nextDouble();
			
			Double cdf = 0.0;
			for(int j=0 ; j < marginal.length ; j++){
				marginal[j]/=norm_const;
				cdf+=marginal[j];
				if (rand_num <= cdf){
					currentVar.setValue(j);
					break;
				}
			}
			//FIXME: Ignoring assertion
//			assert(variables[curr_var]->value()!=INVALID_VALUE);
		}
	}
}
