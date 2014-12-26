/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package starlib.gm.core;

import java.io.Serializable;

/**
 *
 * @author somdeb
 */
public class LogDouble implements Comparable<LogDouble>, Serializable, Cloneable {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 6123803007121351048L;

	private double value;
    
    private boolean isZero = false;
    
    public static final LogDouble ZERO = new LogDouble(0d);

    public static final LogDouble ONE = new LogDouble(1d);
    
    public static final LogDouble MAX_VALUE;
    
    static {
    	MAX_VALUE = new LogDouble();
    	MAX_VALUE.value = Double.MAX_VALUE;
    }

    public LogDouble(Double value, boolean isLogScale) {
    	if(isLogScale) {
    		this.value = value;
    	} else {
            if(value == 0){
                isZero = true;
                return;
            }
            this.value = Math.log(value);
    	}
    }

    public LogDouble(Double value) {
    	this(value, false);
    }

    /**
     * Default constructor. Invisible.
     */
    private LogDouble() {
        
    }
    
    /**
     * Copy constructor
     * 
     * @param src
     */
    public LogDouble(LogDouble src) {
        this.isZero = src.isZero;
        this.value = src.value;
    }

    public LogDouble multiply(LogDouble d){
        if(this.isZero || d.isZero)
            return ZERO;
        
        LogDouble returnVal = new LogDouble();
        returnVal.value = value + d.value;
        
        return returnVal;
    }
    
    public double getValue() {
		return value;
	}
    
    public LogDouble divide(LogDouble d){
        if(this.isZero)
            return ZERO;
        
        if(d.isZero)
            throw new IllegalArgumentException("Argument 'divisor' is 0");
        
        LogDouble returnVal = new LogDouble();
        returnVal.value = value - d.value;
        
        return returnVal;
    }

    public LogDouble add(LogDouble d){
        if(this.isZero)
            return d;
        
        if(d.isZero)
            return this;
        
        double logDiff = value - d.value;
        double offset = value;
        
        if(logDiff > 0) {
        	logDiff = -logDiff;
        	offset = d.value;
        }
        
        LogDouble returnVal = new LogDouble();
        if(logDiff > 50.0d) {
        	returnVal.value = offset;
        	return returnVal;
        }
        
        returnVal.value = offset + Math.log(2 + Math.expm1(- logDiff));
        
        return returnVal;
    }

    @Override
    public int compareTo(LogDouble t) {
        if(this.isZero && t.isZero){
            return 0;
        }
        
        if(this.isZero && !t.isZero){
            return -1;
        }
        
        if(!this.isZero && t.isZero){
            return 1;
        }
        
        if(this.value > t.value)
            return 1;
        
        if(this.value < t.value)
            return -1;
        
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof LogDouble){
            
            LogDouble t = (LogDouble) o;
            
            if(isZero)
                return isZero == t.isZero;
            
            return value == t.value;
        }
        return false;
    }
    
    public static LogDouble max(LogDouble d1, LogDouble d2) {
    	if(d1.compareTo(d2) > 0)
    		return d1;
    	return d2;
    }
    
    public LogDouble power(double d){
        if(this.isZero)
            return ZERO;
        
        if(d == 0)
            return ONE;
        
        LogDouble returnVal = new LogDouble();
        returnVal.value = value * d;
        
        return returnVal;
    }
    
    @Override
    public LogDouble clone() {
    	return new LogDouble(this);
    }
    
    @Override
    public String toString() {
//    	return Double.toString(Math.exp(value));
        if(isZero)
        	return "ZERO";
    	return Double.toString(value);
    }
    
    public Double toDouble() {
        if(isZero)
        	return 0.0;
    	return Math.exp(value);
    }

	public boolean isZero() {
		return isZero;
	}
	
	public boolean isInfinite() {
		return value == Double.MAX_VALUE || value == Double.POSITIVE_INFINITY;
	}
	
}
