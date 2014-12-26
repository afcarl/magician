package starlib.mln.efficient.store.internal.jt;

import java.util.List;

import starlib.mln.efficient.store.internal.IntFunction;

public class JoinTreeIntApprox extends JoinTreeInt {
	
	protected boolean outOfSync = false;
	
	protected boolean firstTime = true;

	public JoinTreeIntApprox(int id, List<IntFunction> functions) {
		super(id, functions);
	}
	
	@Override
	public void calibrate() {
		if(firstTime) {
			super.calibrate();
			firstTime = false;
		} else 
			this.reCalibrateAll();
	}

	public void reCalibrate(int changedFunctionId, int address)
	{
		this.approximateReCalibrate(changedFunctionId, address);
	}
	
	private void approximateReCalibrate(int changedFunctionId, int address) {
		IntFunction changedFunction = functions.get(changedFunctionId);
		JGNodeInt affectedNode = funct_to_node[changedFunctionId];
		
		instantiateVariables(changedFunction.variables, address);
		
		int messageOrderSize = message_order.size();
		boolean start = false;
		
		for(int i=0; i < messageOrderSize; i++) {
			if(!start && message_order.get(i).node1() == affectedNode ){
				start = true;
			}
			if(start)
				message_order.get(i).updateMessage1to2(true);
		}

		start = false;
		
		for(int i = messageOrderSize - 1; i > -1; i--) {
			if(!start && message_order.get(i).node2() == affectedNode ){
				start = true;
			}
			if(start)
				message_order.get(i).updateMessage2to1(true);
		}
		
		for(int i=0; i < noOfEffectiveNodes; i++) {
			nodes[i].updateZ(true);
		}
		
		this.resetChanges(affectedNode);

		freeVariables(changedFunction.variables);
	}
	
	private void resetChanges(JGNodeInt affectedNode) {

		int messageOrderSize = message_order.size();
		boolean start = false;
		
		for(int i=0; i < messageOrderSize; i++) {
			if(!start && message_order.get(i).node1() == affectedNode ){
				start = true;
			}
			if(start)
				message_order.get(i).resetMessage1to2();
		}

		start = false;
		
		for(int i = messageOrderSize - 1; i > -1; i--) {
			if(!start && message_order.get(i).node2() == affectedNode ){
				start = true;
			}
			if(start)
				message_order.get(i).resetMessage2to1();
		}
		
		for(int i=0; i < noOfEffectiveNodes; i++) {
			nodes[i].resetChangeMarginal();
		}
		
	}
	
	@Override
	public void sample() {
		super.sample();
	}
}
