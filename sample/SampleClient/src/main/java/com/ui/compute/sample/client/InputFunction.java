package com.ui.compute.sample.client;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.function.Function;

public class InputFunction implements Function<BigDecimal, BigDecimal>, Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private FunctionName mName;
	private BigDecimal[] mCoeffs;
	
	public InputFunction(){
		this(FunctionName.LINEAR, new BigDecimal[]{new BigDecimal(0), new BigDecimal(1)});
	}
	
	public InputFunction(FunctionName name, BigDecimal[] coeffs){
		mName = name;
		mCoeffs = coeffs;
	}

	public BigDecimal apply(BigDecimal x) {
		// TODO Auto-generated method stub
		if(mName.equals(FunctionName.LINEAR)){
			return mCoeffs[0].add(mCoeffs[1].multiply(x, MathContext.DECIMAL64), MathContext.DECIMAL64);
		}
		return null;
	}


	


}
