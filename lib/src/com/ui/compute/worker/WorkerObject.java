package com.ui.compute.worker;


import java.util.concurrent.LinkedBlockingQueue;

import com.ui.compute.lib.IndexedResult;
import com.ui.compute.lib.IndexedTask;
import com.ui.compute.lib.SynchronizedQueue;
import com.ui.compute.lib.TaskType;
import com.ui.compute.lib.TaskWrapper;



public class WorkerObject{
	
	private Class<?> mInputType;
	private Class<?> mOutputType;
	private LinkedBlockingQueue<IndexedTask> mWorkLBQ;
	private SynchronizedQueue<TaskWrapper> mWorkQueue;
	private SynchronizedQueue<IndexedResult> mResultQueue;
	
	public WorkerObject(Class<?> inputType, Class<?> outputType){
		mInputType = inputType;
		mOutputType = outputType;
		mWorkLBQ = new LinkedBlockingQueue<IndexedTask>();
		mWorkQueue = new SynchronizedQueue<TaskWrapper>();
		mResultQueue = new SynchronizedQueue<IndexedResult>();
	}
	
	public synchronized void init(TaskType type) throws ClassCastException{
		if(type.equals(TaskType.INTEGRATION)){
			Thread t = new Thread(new IntegrateRunnable());
			t.start();
		} else{
			Thread t = new Thread(new ProcessRunnable(this));
			t.start();
		}
		
	}
	

	public synchronized void submitIntegration(LinkedBlockingQueue<IndexedTask> taskQueue) throws InterruptedException{
		// TODO Auto-generated method stub
		for(int i = 0; i < taskQueue.size(); i++){
			mWorkLBQ.put(taskQueue.take());
		}
		notifyAll();
	}
	
	public synchronized void submitGeneric(SynchronizedQueue<TaskWrapper> taskQueue)throws InterruptedException{
		// TODO Auto-generated method stub
		
		int size = taskQueue.size();
		for(int i = 0; i < size; i++){
			mWorkQueue.put(taskQueue.take());
		}
		
		notifyAll();
	}

	public IndexedResult getIndexedResult() throws InterruptedException {
		// TODO Auto-generated method stub
		return mResultQueue.take();
	}
	
	
	class IntegrateRunnable implements Runnable{

		
		public IntegrateRunnable(){
			
		}
		
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				while(!mWorkLBQ.isEmpty()){
				IndexedTask indexedTask = mWorkLBQ.take();
				int position = indexedTask.getPosition();
				Object result = indexedTask.getTask().execute();
				
				
				IndexedResult indexedResult = new IndexedResult(position, result);
				mResultQueue.put(indexedResult);
				}
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	class ProcessRunnable implements Runnable{
		
		public ProcessRunnable(Object lock){
			
		}
		
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				
				while(mWorkQueue.isEmpty()){
					Thread.sleep(50);
				}
				
				int position = 0;
				Object result = null;
				IndexedResult indexedResult = null;
				while(!mWorkQueue.isEmpty()){
					TaskWrapper taskWrapper = mWorkQueue.take();
					position = taskWrapper.getPosition();
					taskWrapper.execute();
					result = taskWrapper.getResult();
					
					
					indexedResult = new IndexedResult(position, result);
					mResultQueue.put(indexedResult);
				}
				
				// Send a poison pill
				mResultQueue.put(new IndexedResult(-50, null));
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
}
