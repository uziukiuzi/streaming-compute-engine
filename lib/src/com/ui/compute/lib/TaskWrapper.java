package com.ui.compute.lib;

import java.io.Serializable;
import java.util.List;


public class TaskWrapper implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private SynchronizedQueue<?> mUnitData;
	private UnitTask mUnitTask;
	private List<Serializable> mParameters;
	private Object mResult;
	private int mPosition;
	private Class<?> mInputType;
	private Class<?> mOutputType;
	
	public <T> TaskWrapper(SynchronizedQueue<T> unitData, UnitTask task, List<Serializable> parameters, int position, Class<?> inputType, Class<?> outputType){
		synchronized(this){
			mUnitData = unitData;
			mUnitTask = task;
			mParameters = parameters;
			mPosition = position;
			mInputType = inputType;
			mOutputType = outputType;
		}
	}
	
	
	public synchronized void execute() throws Exception{
		mResult = mUnitTask.execute(mUnitData, mParameters);
		if(mOutputType == null){
			return;
		}
		if(!mResult.getClass().isAssignableFrom(mOutputType)){
			throw new ClassCastException();
		}
	}
	
	public synchronized Object getResult(){
		return mResult;
	}
	
	public synchronized int getPosition(){
		return mPosition;
	}
	
}