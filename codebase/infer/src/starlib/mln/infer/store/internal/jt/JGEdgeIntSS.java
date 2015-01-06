package starlib.mln.infer.store.internal.jt;

import java.util.ArrayList;
import java.util.List;

import starlib.mln.infer.store.internal.IntFunction;

public class JGEdgeIntSS extends JGEdgeInt {
	
	JGNodeIntSS ss_node1_;
	JGNodeIntSS ss_node2_;
	
	public List<IntFunction> all_1to2_functions;	
	public List<IntFunction> all_2to1_functions;
	
	public JGEdgeIntSS() {
		node1_to_node2_message_ = new IntFunction();
		node2_to_node1_message_ = new IntFunction();
	}

	public JGEdgeIntSS(JGNodeInt ss_node1_, JGNodeInt ss_node2_) {
		node1_ = ss_node1_;
		node2_ = ss_node2_;
		
		this.ss_node1_ = (JGNodeIntSS) ss_node1_;
		this.ss_node2_ = (JGNodeIntSS) ss_node2_;
		
		node1_to_node2_message_ = new IntFunction();
		node2_to_node1_message_ = new IntFunction();
	}
	
	public void initialize() {
		node1_to_node2_message_ = new IntFunction();
		node2_to_node1_message_ = new IntFunction();
	}
	
	public void sendMessage1to2()
	{
		all_1to2_functions = new ArrayList<>(ss_node1_.functions);
		
		for(int i=0; i < ss_node1_.edges().size(); i++) {
			JGEdgeInt curr_edge = ss_node1_.edges().get(i);
			
			if(curr_edge == this) {
				continue;
			}
			
			if(curr_edge.node1() == ss_node1_) {
				if(curr_edge.message2() != null)
					all_1to2_functions.add(curr_edge.message2());
			} else {
				if(curr_edge.message1() != null)
					all_1to2_functions.add(curr_edge.message1());
			}
		}
		node1_to_node2_message_ = IntFunction.multiplyAndMarginalize(variables(),all_1to2_functions);
	}
	
	public void sendMessage2to1()
	{
		all_2to1_functions = new ArrayList<>(ss_node2_.functions);
		
		for(int i=0; i < ss_node2_.edges().size(); i++) {
			JGEdgeInt curr_edge = ss_node2_.edges().get(i);
			
			if(curr_edge == this) {
				continue;
			}
			
			if(curr_edge.node1() == ss_node2_) {
				if(curr_edge.message2() != null)
					all_2to1_functions.add(curr_edge.message2());
			} else {
				if(curr_edge.message1() != null)
					all_2to1_functions.add(curr_edge.message1());
			}
		}
		node2_to_node1_message_ = IntFunction.multiplyAndMarginalize(this.variables(),all_2to1_functions);
	}
	
	public void updateMessage1to2(boolean singleChange) {
		if(singleChange)
			IntFunction.approximateMultiplyAndMarginalize(all_1to2_functions, node1_to_node2_message_);
		else
			IntFunction.multiplyAndMarginalizeInPlace(all_1to2_functions, node1_to_node2_message_);
	}
	
	public void updateMessage2to1(boolean singleChange) {
		if(singleChange)
			IntFunction.approximateMultiplyAndMarginalize(all_2to1_functions, node2_to_node1_message_);
		else
			IntFunction.multiplyAndMarginalizeInPlace(all_2to1_functions, node2_to_node1_message_);
	}
	
	public void resetMessage1to2()
	{
		if(node1_to_node2_message_ != null)
			IntFunction.resetChanges(node1_to_node2_message_);
	}
	
	public void resetMessage2to1()
	{
		if(node2_to_node1_message_ != null)
			IntFunction.resetChanges(node2_to_node1_message_);
	}
	
}
