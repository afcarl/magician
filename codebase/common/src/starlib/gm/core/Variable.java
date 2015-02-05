package starlib.gm.core;

import java.util.List;

public class Variable implements Comparable<Variable> {
	
	private int id;
	
	private int domainSize;
	
	private Integer value;
	
	private Integer addressValue;
	
	private Integer tempValue;
	
	public Variable() {
	}

	public Variable(int id, int domainSize) {
		this.id = id;
		this.domainSize = domainSize;
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Integer getValue() {
		if (value != null) {
			return value;
		}
		return addressValue;
	}
	
	public Integer getOnlyValue() {
			return value;
	}
	
	public void setValue(Integer value) {
		this.value = value;
	}
	
	public boolean isInstantiated() {
		return value != null;
	}
	
	public int getDomainSize() {
		return domainSize;
	}
	
	public void setAddressValue(Integer addressValue) {
		this.addressValue = addressValue;
	}
	
	public Integer getTempValue() {
		return tempValue;
	}

	public void setTempValue(Integer tempValue) {
		this.tempValue = tempValue;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		if (obj instanceof Variable) {
			Variable var = (Variable) obj;
			return (var.id == this.id);
		}
		return false;
	}
	
	@Override
	public int compareTo(Variable t) {
        if(this.id > t.id)
            return 1;
        
        if(this.id < t.id)
            return -1;
        
        return 0;
	}

	public static int getAddress(List<Variable> variables) {
		int address = 0;
		int multiplier = 1;
		
		int variableSize = variables.size();
		for (int i = variableSize - 1; i >= 0; i--) {
			Variable variable = variables.get(i);
			address += (multiplier * variable.getValue());
			multiplier *= variable.getDomainSize();
		}
		
		return address;
	}
	
	public static void setAddress(List<Variable> variables, int address) {

		int variableSize = variables.size();
		for (int i = variableSize - 1; i >= 0; i--) {
			Variable variable = variables.get(i);
			variable.setAddressValue( address % variable.getDomainSize());
			address /= variable.getDomainSize();
		}
		
	}

	public static int getDomainSize(List<Variable> variables) {
		int domainSize = 1;
		
		for (Variable variable : variables) {
			domainSize *= variable.getDomainSize();
		}
		
		return domainSize;
	}
	
	public static void instantiateVariables(List<Variable> variables, int address) {
		// Instantiate the variables
		Variable.setAddress(variables, address);
		for (Variable var : variables) {
			var.setValue(var.getValue());
		}
	}
	
	public static void freeVariables(List<Variable> variables) {
		for (Variable var : variables) {
			var.setAddressValue(null);
			var.setValue(null);
		}
	}
	
}
