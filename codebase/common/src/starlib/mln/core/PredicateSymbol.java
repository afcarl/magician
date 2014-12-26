package starlib.mln.core;

import java.util.ArrayList;
import java.util.List;

import starlib.gm.core.LogDouble;

public class PredicateSymbol {

	public int id;
	public int parentId;
	public String symbol;
	public List<Integer> variable_types = new ArrayList<Integer>();
	public boolean isOriginalSymbol;
	public LogDouble pweight;
	public LogDouble nweight;
	
	public String printString;
	
	public PredicateSymbol() {
	}

	public PredicateSymbol(int id_, String symbol_, List<Integer> var_types,
			LogDouble pweight_, LogDouble nweight_) {
		id = id_;
		symbol = symbol_;
		variable_types = var_types;
		pweight = pweight_;
		nweight = nweight_;
		parentId = id;
		
		printString = symbol_ + "(";
		for (int i = 0; i < var_types.size() - 1; i++) {
			printString += "_, ";
		}
		printString += "_)";
	}
	
	@Override
	public String toString() {
		if(printString == null)
			return symbol + "_" + id + "()";
		return printString;
	}

}
