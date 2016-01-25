package com.ui.compute.lib;

import java.io.Serializable;
import java.util.List;

public abstract class UnitTask implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Class<?> mInputType;
	private Class<?> mOutputType;
	
	public UnitTask(Class<?> inputType, Class<?> outputType){
		mInputType = inputType;
		mOutputType = outputType;
	}

	public abstract Object execute(SynchronizedQueue<?> unitData, List<Serializable> parameters) throws Exception;
	
	public Class<?> getInputType(){
		return mInputType;
	}
	public Class<?> getOutputType(){
		return mOutputType;
	}
}