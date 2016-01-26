package com.ui.compute.lib;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

public class SynchronizedQueue<T> implements Serializable{

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private LinkedBlockingQueue<T> mQueue;
	
	public SynchronizedQueue(SynchronizedQueue<T> q){
		mQueue = new LinkedBlockingQueue<T>(q.getUnderlyingLBQ());
	}
	
	public SynchronizedQueue(){
		mQueue = new LinkedBlockingQueue<T>();
	}
	
	public LinkedBlockingQueue<T> getUnderlyingLBQ(){
		return mQueue;
	}
	
	public synchronized void put(T t) throws InterruptedException{
		mQueue.put(t);
		notifyAll();
	}
	
	
	public synchronized T take() throws InterruptedException{
		while(mQueue.isEmpty()){
			wait();
		}
		return mQueue.take();
	}
	
	public synchronized void addAll(Collection<T> q){
		mQueue.addAll(q);
	}
	
	public synchronized void addAll(SynchronizedQueue<T> q){
		Iterator<T> it = q.iterator();
		while(it.hasNext()){
			mQueue.add(it.next());
		}
	}
	
	public synchronized Iterator<T> iterator(){
		return mQueue.iterator();
	}
	
	public synchronized boolean isEmpty(){
		return mQueue.isEmpty();
	}

	public synchronized int size() {
		return mQueue.size();
	}

	
	
}
