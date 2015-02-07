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

	private double logValue;
    
    private boolean isZero = false;
    
    public static final LogDouble ZERO = new LogDouble(0d);

    public static final LogDouble ONE = new LogDouble(1d);
    
    public static final LogDouble MAX_VALUE;
    
    static {
    	MAX_VALUE = new LogDouble();
    	MAX_VALUE.logValue = Double.MAX_VALUE;
    }

    public LogDouble(Double value, boolean isLogScale) {
    	if(isLogScale) {
    		this.logValue = value;
    	} else {
            if(value == 0){
                isZero = true;
                return;
            } else if (value < 0) {
            	System.out.println("FUCKKKK");
            	throw new IllegalArgumentException("Log of a negative number");
            }
            else this.logValue = Math.log(value);
    	}
    }

    public LogDouble(Double value) {
    	this(value, false);
    }

    /**
     * Default constructor. Invisible.
     */
    private LogDouble() {}
    
    /**
     * Copy constructor
     * 
     * @param src
     */
    public LogDouble(LogDouble src) {
        this.isZero = src.isZero;
        this.logValue = src.logValue;
    }

    public LogDouble multiply(LogDouble d){
        if(this.isZero || d.isZero)
            return ZERO;
        
//        LogDouble returnVal = new LogDouble();
//        returnVal.logValue = logValue + d.logValue;
//        
//        return returnVal;
        return new LogDouble(logValue + d.logValue, true);
    }
    
    public double getLogValue() {
		return logValue;
	}
    
    public double getValue() {
    	return Math.exp(logValue);
    }
    
    public LogDouble divide(LogDouble d){
        if(this.isZero)
            return ZERO;
        
        if(d.isZero)
            throw new IllegalArgumentException("Argument 'divisor' is 0");
        
//        LogDouble returnVal = new LogDouble();
//        returnVal.logValue = logValue - d.logValue;
//        
//        return returnVal;
        return new LogDouble(logValue - d.logValue, true);
    }

    public LogDouble add(LogDouble d){
        if(this.isZero)
            return d;
        
        if(d.isZero)
            return this;
        
        double logDiff = logValue - d.logValue;
        double offset = logValue;
        
        if(logDiff > 0) {
        	logDiff = -logDiff;
        	offset = d.logValue;
        }
        
//        LogDouble returnVal = new LogDouble();
//        if(logDiff > 50.0d) {
//        	returnVal.logValue = offset;
//        	return returnVal;
//        }
//        
//        returnVal.logValue = offset + Math.log(2 + Math.expm1(- logDiff));
//        
//        return returnVal;
        
        if(logDiff > 50.0d) {
        	return new LogDouble(offset, true);
        }
        
        return new LogDouble(offset + Math.log(2 + Math.expm1(- logDiff)), true);
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
        
        if(this.logValue > t.logValue)
            return 1;
        
        if(this.logValue < t.logValue)
            return -1;
        
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof LogDouble){
            
            LogDouble t = (LogDouble) o;
            
            if(isZero)
                return isZero == t.isZero;
            
            return logValue == t.logValue;
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
        
//        LogDouble returnVal = new LogDouble();
//        returnVal.logValue = logValue * d;
//        
//        return returnVal;
        return new LogDouble(logValue * d, true);
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
    	return Double.toString(logValue);
    }
    
    public Double toDouble() {
        if(isZero)
        	return 0.0;
    	return Math.exp(logValue);
    }

	public boolean isZero() {
		return isZero;
	}
	
	public boolean isInfinite() {
		return logValue == Double.MAX_VALUE || logValue == Double.POSITIVE_INFINITY;
	}
}
