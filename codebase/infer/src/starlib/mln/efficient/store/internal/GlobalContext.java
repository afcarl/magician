package starlib.mln.efficient.store.internal;

import java.util.ArrayList;
import java.util.List;

import starlib.gm.core.Variable;


public class GlobalContext {
	
	private static GlobalContext context;
	
	private List<ClauseSpecificContext> localContexts;
	
	private int currentContext;
	
	private GlobalContext() {
	}
	
	public static void init(int noOfLocalContexts) {
		if(context == null) {
			context = new GlobalContext();
			context.localContexts = new ArrayList<>(noOfLocalContexts);
			for (int i = 0; i < noOfLocalContexts; i++) {
				context.localContexts.add(new ClauseSpecificContext(i));
			}
		}
	}
	
	public static void setCurrentContext(int currentContext) {
		context.currentContext = currentContext;
	}
	
	public static void setVariables(List<Variable> variables) {
//		context.localContexts.get(context.currentContext).setVariables(variables);
	}
	
//	public static List<Variable> getVariables() {
//		return context.variables;
//	}
	
	public static void saveEvidence() {
//		context.localContexts.get(context.currentContext).saveEvidence();
	}

	public static void resetEvidence() {
//		context.localContexts.get(context.currentContext).resetEvidence();
	}
	
}
