package com.ui.compute.lib;

import java.io.Serializable;

public class IndexedResult implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int mPosition;
	private Object mResult;
	
	public IndexedResult(int position, Object result){
		
		if(result != null && !(result instanceof Serializable)){
			throw new IllegalArgumentException("Object must be serializable!");
		}
		if(result == null){
			System.out.println("Result is null");
		}
		synchronized(this){
			mPosition = position;
			mResult = result;
		}
		
	}
	
	public synchronized int getPosition(){
		return mPosition;
	}
	
	public synchronized Object getResult(){
		return mResult;
	}
	
	public synchronized void setPosition(int position){
		mPosition = position;
	}
	
	public synchronized void setResult(Object result){
		
		if(!(result instanceof Serializable)){
			throw new IllegalArgumentException();
		}
		
		mResult = result;
	}
	
}
