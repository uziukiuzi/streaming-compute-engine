package com.ui.compute.lib;

import java.io.Serializable;


public class IndexedTask implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int mPosition;
	private Task<? extends Serializable> mTask;
	
	public IndexedTask(int position, Task<? extends Serializable> task){
		
		
		synchronized(this){
			mPosition = position;
			mTask = task;
		}
		
	}
	
	public synchronized int getPosition(){
		return mPosition;
	}
	
	public synchronized Task<? extends Serializable> getTask(){
		return mTask;
	}
	
	public synchronized void setPosition(int position){
		mPosition = position;
	}
	
	public synchronized void setTask(Task<? extends Serializable> task){
		mTask = task;
	}
	
}
