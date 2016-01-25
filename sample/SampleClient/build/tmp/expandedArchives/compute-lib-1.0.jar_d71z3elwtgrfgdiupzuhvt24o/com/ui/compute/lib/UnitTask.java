package com.ui.compute.lib;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class UnitTask implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Class<?> mInputType;
	private Class<?> mOutputType;
	private ArrayList<Serializable> mSupplementaries;
	private int mIndex;
	private int mTotalTasks;
	
	public UnitTask(Class<?> inputType, Class<?> outputType){
		mInputType = inputType;
		mOutputType = outputType;
		mSupplementaries = new ArrayList<Serializable>();
	}

	/**
	 * Set any extra data points required by this chunk from the data, such as for boundary conditions etc.
	 * The current chunk index can be found by calling getIndex().
	 * 
	 * @param chunks the data, separated into the chunks which will be distributed over the workers
	 * @param chunkIndex the index of the current chunk
	 * @param supplementaries populate this list with the supplementaries required by the current chunk
	 */
	public abstract void setSupplementaries(List<SynchronizedQueue<Serializable>> chunks, List<Serializable> supplementaries);
	
	public abstract <T> Object execute(SynchronizedQueue<T> unitData, List<Serializable> parameters) throws Exception;
	
	public Class<?> getInputType(){
		return mInputType;
	}
	public Class<?> getOutputType(){
		return mOutputType;
	}
	public List<Serializable> getSupplementaries(){
		return mSupplementaries;
	}
	public void setIndex(int index){
		mIndex = index;
	}
	public int getIndex(){
		return mIndex;
	}
	public void setTotalTasks(int tasks){
		mTotalTasks = tasks;
	}
	public int getTotalTasks(){
		return mTotalTasks;
	}
}