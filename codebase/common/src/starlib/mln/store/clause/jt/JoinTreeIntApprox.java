package starlib.mln.store.clause.jt;

import java.util.List;

import starlib.mln.store.internal.IntFunction;

public class JoinTreeIntApprox extends JoinTreeInt {
	
	private boolean selfJoinCase = false;
	
	private double tempZ;
	
	public JoinTreeIntApprox(int id, List<IntFunction> functions) {
		super(id, functions);
	}
	
	@Override
	public void reCalibrate(List<Integer> changedFunctionIds, int address) {
		selfJoinCase = !selfJoinCase;
		
		if(selfJoinCase) {
			double oldZ = super.getZ();
			
			double sumOfAllZs = 0;
			for (Integer changedFunctionId : changedFunctionIds) {
				sumOfAllZs += super.computeDeltaZ(changedFunctionId, address);
			}
			
			tempZ = oldZ + sumOfAllZs / changedFunctionIds.size();
		} else {
			// Called after unflip. Nothing to do
		}
		
	}
	
	@Override
	public double getZ() {
		if(selfJoinCase) {
			return tempZ;
		}
		return super.getZ();
	}
	
}
