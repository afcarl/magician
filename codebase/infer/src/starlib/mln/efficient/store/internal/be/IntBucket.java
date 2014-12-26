package starlib.mln.efficient.store.internal.be;

import java.util.ArrayList;
import java.util.List;

import starlib.gm.core.Variable;
import starlib.mln.efficient.store.internal.IntFunction;

public class IntBucket {

	private Variable bucketVariable;
	
	private List<IntFunction> functions = new ArrayList<IntFunction>();
	
	private List<IntFunction> all_functions;
	
	private List<BucketChannelInt> input = new ArrayList<>();

	private IntFunction result;
	
	private int Z = 1;
	
	public IntBucket(Variable bucketVariable) {
		this.bucketVariable = bucketVariable;
	}

	/**
	 * adds function f to the bucket
	 * 
	 * @param f
	 */
	void addFunction(IntFunction f) {
		functions.add(f);
	}
	
	public void addInputChannel(BucketChannelInt inputChannel) {
		this.input.add(inputChannel);
	}
	
	private void compile_all_functions() {
		all_functions = new ArrayList<>();
		all_functions.addAll(functions);
		for (BucketChannelInt channel : input) {
			all_functions.add(channel.getMessage());
		}
	}

	/**
	 * Joins the MB functions, eliminate the bucket variable, and returns the
	 * resulting function
	 * 
	 * @return
	 */
	public IntFunction eliminate() {
		this.compile_all_functions();
		result = IntFunction.multiplyAndSumOut(all_functions, bucketVariable);		
		return result;
	}
	
	public void computeZ() {
		this.compile_all_functions();
		for (IntFunction function : all_functions) {
			Z *= function.getConstantValue();
		}
	}
	
	public void updateZ() {
		for (IntFunction function : all_functions) {
			Z *= function.getConstantValue();
		}
	}
	
	public int getZ() {
		return Z;
	}
	
	public void reCalibrate() {
		if(result.isEmpty()) 
			// Result is constant function
			IntFunction.multiplyAndMarginalizeConstantFunction(all_functions, result);
		else
			IntFunction.multiplyAndMarginalizeInPlace(all_functions, result);
	}

	public int size() {
		return functions.size();
	}
	
	public List<IntFunction> getFunctions() {
		return functions;
	}

	public IntFunction getFunction(int index) {
		return functions.get(index);
	}
}
