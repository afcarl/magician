package starlib.mln.infer.store.internal.jt;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import starlib.gm.core.Variable;
import starlib.mln.infer.store.internal.IntFunction;

public abstract class JGNodeInt {
	
	protected Random random = new Random();
	
	private static int INVALID_VALUE = -1;

	protected List<Variable> variables_;
	protected List<JGEdgeInt> edges_;
	
	
	protected int id_;
	protected boolean deleted_;
	protected boolean outOfSyn;
	
	// Default constructor
	public JGNodeInt() {
		id_ = INVALID_VALUE; 
		deleted_ = false;
		variables_ = new ArrayList<>();
		edges_ = new ArrayList<>();
		outOfSyn = false;
	}
	
	// Access to some internal variables
	public boolean isDeleted() {
		return deleted_;
	}

	public List<Variable> variables() {
		return variables_;
	}
	
	public List<JGEdgeInt> edges() {
		return edges_;
	}
	
	//JGNode neighbor(int i);
	
	public int id() {
		return id_;
	}

	public void delete() {
		deleted_ = true;
		variables_ = null;
		edges_ = null;
	}
	
	// Main functions which are to be overloaded by various architectures
	public abstract void addFunction(IntFunction function);
	
	public abstract IntFunction getMarginal(List<Variable> marg_variables);
	
//	public abstract void updateMarginal(IntFunction outFunction)
	
	public abstract void computeZ();
	
	public abstract double computeDeltaZ(IntFunction changedFunction, int address);
	
	public abstract void initialize();
	
	public abstract double getZ();

	public abstract void updateZ(boolean singleChange);
	
	public abstract void sample();
	
	public boolean isEmpty() {
		return false;
	}
	
	public abstract void resetChangeMarginal();

}
