package starlib.mln.store.clause.jt;

import java.util.ArrayList;
import java.util.List;

import starlib.gm.core.Variable;
import starlib.mln.store.internal.IntFunction;

public class JGEdgeInt {
	protected JGNodeInt node1_;
	protected JGNodeInt node2_;
	protected List<Variable> variables_; // FIXME: This was marked as 'mutable'. What is 'mutable' ???
	public IntFunction node1_to_node2_message_;
	public IntFunction node2_to_node1_message_;

	public List<IntFunction> all_1to2_functions;	
	public List<IntFunction> all_2to1_functions;
	
	public JGEdgeInt(JGNodeInt ss_node1_, JGNodeInt ss_node2_) {
		variables_ = new ArrayList<>();

		node1_ = ss_node1_;
		node2_ = ss_node2_;
		
		node1_to_node2_message_ = new IntFunction();
		node2_to_node1_message_ = new IntFunction();
	}
	
	public void initialize() {
		node1_to_node2_message_ = new IntFunction();
		node2_to_node1_message_ = new IntFunction();
	}
	
	// Access to internal data structure
	public JGEdgeInt() {
		variables_ = new ArrayList<>();
		node1_to_node2_message_ = new IntFunction();
		node2_to_node1_message_ = new IntFunction();
	}

	public JGNodeInt node1() {
		return node1_;
	}

	public JGNodeInt node2() {
		return node2_;
	}

	public List<Variable> variables() {
		return variables_;
	}

	public IntFunction message1() {
		return node1_to_node2_message_;
	}

	public IntFunction message2() {
		return node2_to_node1_message_;
	}

	// Functions

	public void sendMessage1to2()
	{
		all_1to2_functions = new ArrayList<>(node1_.functions);
		
		for(int i=0; i < node1_.edges().size(); i++) {
			JGEdgeInt curr_edge = node1_.edges().get(i);
			
			if(curr_edge == this) {
				continue;
			}
			
			if(curr_edge.node1() == node1_) {
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
		all_2to1_functions = new ArrayList<>(node2_.functions);
		
		for(int i=0; i < node2_.edges().size(); i++) {
			JGEdgeInt curr_edge = node2_.edges().get(i);
			
			if(curr_edge == this) {
				continue;
			}
			
			if(curr_edge.node1() == node2_) {
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
	
	public void resetMessage1to2() {
		if(node1_to_node2_message_ != null)
			IntFunction.resetChanges(node1_to_node2_message_);
	}
	
	public void resetMessage2to1() {
		if(node2_to_node1_message_ != null)
			IntFunction.resetChanges(node2_to_node1_message_);
	}
	
	private void multiplyAndMarginalizeInPlaceForChanges(List<IntFunction> functions, IntFunction outFunction, List<IntFunction> changedFunctions, int changedAddress) {
		List<Integer> changedAddressList = new ArrayList<>(functions.size());
		boolean atLeastOneChanged = false;
		
		for (IntFunction function : functions) {
			if(changedFunctions.contains(function)) {
				changedAddressList.add(changedAddress);
				atLeastOneChanged = true;
			} else {
				changedAddressList.add(null);
			}
		}
		
		if(atLeastOneChanged)
			IntFunction.multiplyAndMarginalizeInPlaceForMultipleChange(functions, outFunction, changedAddressList);
		else
			IntFunction.multiplyAndMarginalizeInPlace(functions, outFunction);
	}
	
	public void updateMessage1to2(List<IntFunction> changedFunctions, int changedAddress) {
		multiplyAndMarginalizeInPlaceForChanges(all_1to2_functions, node1_to_node2_message_, changedFunctions, changedAddress);
	}
	
	public void updateMessage2to1(List<IntFunction> changedFunctions, int changedAddress) {
		multiplyAndMarginalizeInPlaceForChanges(all_2to1_functions, node2_to_node1_message_, changedFunctions, changedAddress);
	}
	
}
