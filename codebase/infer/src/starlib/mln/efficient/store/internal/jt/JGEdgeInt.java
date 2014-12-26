package starlib.mln.efficient.store.internal.jt;

import java.util.ArrayList;
import java.util.List;

import starlib.gm.core.Variable;
import starlib.mln.efficient.store.internal.IntFunction;

public abstract class JGEdgeInt {
	protected JGNodeInt node1_;
	protected JGNodeInt node2_;
	protected List<Variable> variables_; // FIXME: This was marked as 'mutable'. What is 'mutable' ???
	public IntFunction node1_to_node2_message_;
	public IntFunction node2_to_node1_message_;

	// Access to internal data structure
	public JGEdgeInt() {
		variables_ = new ArrayList<>();
		node1_to_node2_message_ = new IntFunction();
		node2_to_node1_message_ = new IntFunction();
	}

	public abstract void initialize();

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

	public void printMessages() {
		// do nothing
	}

	public abstract void sendMessage1to2();

	public abstract void sendMessage2to1();

	public abstract void updateMessage1to2(boolean singleChange);

	public abstract void updateMessage2to1(boolean singleChange);

	public abstract void resetMessage1to2();
	
	public abstract void resetMessage2to1();
	
}
