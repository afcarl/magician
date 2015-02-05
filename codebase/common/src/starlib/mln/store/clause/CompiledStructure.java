package starlib.mln.store.clause;

import java.util.List;

import starlib.gm.core.Variable;

public interface CompiledStructure {

	public abstract List<Variable> getVariables();

	public abstract void calibrate();

	public abstract void reCalibrate(int changedFunctionId, int address);

	public abstract void reCalibrateAll();

	public abstract double getZ();

	public abstract double computeDeltaZ(int changedFunctionId, int address);

	public abstract void sample();

	public abstract void reCalibrate(List<Integer> changedFunctionIds, int address);

}