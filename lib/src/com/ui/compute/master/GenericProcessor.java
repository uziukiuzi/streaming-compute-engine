package com.ui.compute.master;

import java.io.Serializable;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.List;

import com.ui.compute.lib.SynchronizedQueue;
import com.ui.compute.lib.TaskType;
import com.ui.compute.lib.TaskWrapper;
import com.ui.compute.lib.UnitTask;
import com.ui.compute.lib.IndexedResult;


public class GenericProcessor{
	
	private String[] mWorkers;
	private Class<?> mInputType;
	private Class<?> mOutputType;
	private URL[] mArgURLs;
	private URL[] mWorkURLs;
	private String mPackageName;
	private String mStdPackageName;
	private String[] mArgNames;
	private String[] mWorkNames;
	private int mEndWorker;
	private ArrayList<SynchronizedQueue<TaskWrapper>> mTaskQueues;
	private ChainSpace mChainSpace;
	private LinkedBlockingQueue<TaskWrapper> mTaskQueue;
	private LinkedBlockingQueue<Object> mResultQueue;
	private Set<Integer> mChecklist;
	private ArrayList<WorkerLinkThread> mThreads;

	public GenericProcessor(String[] workers, Class<?> inputType, Class<?> outputType){
		mWorkers = workers;
		mInputType = inputType;
		mOutputType = outputType;
		mThreads = new ArrayList<WorkerLinkThread>();
		mChainSpace = new ChainSpace(outputType);
		mResultQueue = new LinkedBlockingQueue<Object>();
		mChecklist = Collections.synchronizedSet(new HashSet<Integer>());
		
	}
	
	public void initCodeTransfer(String packageName, String stdPackageName, URL[] argURLs, URL[] workURLs, String[] argNames, String[] workNames){
		mArgURLs = argURLs;
		mWorkURLs = workURLs;
		mPackageName = packageName;
		mStdPackageName = stdPackageName;
		mArgNames = argNames;
		mWorkNames = workNames;
	}

	public <T, R> void initProcessor(UnitTask task, SynchronizedQueue<T> data, List<Serializable> parameters, int unitSize) throws RemoteException, InterruptedException{
		// TODO Auto-generated method stub
		 
		 
			 int numWorkers = mWorkers.length;
			 mTaskQueue = new LinkedBlockingQueue<TaskWrapper>();
			 int currentUnit = 0;
			 
			 // Create a queue of tasks, one task for each work unit.
			 
			 while(!data.isEmpty()){
				 
				 
				if(data.size() < unitSize){ 
					mTaskQueue.put(new TaskWrapper(data, task, parameters, currentUnit, mInputType, mOutputType));
				} else{
					SynchronizedQueue<T> q = new SynchronizedQueue<T>();
					for(int i = 0; i < unitSize; i++){
						q.put(data.take());
					}
					mTaskQueue.put(new TaskWrapper(q, task, parameters, currentUnit, mInputType, mOutputType));
				}
				
				
				
			 
			 // Add the index of each task to a global checklist. As corresponding results are retrieved
			 // they will each be checked off the list. Once the list is empty, the threads and the chain
			 // space will shut down.
			 mChecklist.add(currentUnit);
			 
			 currentUnit++;
			 
			 }
			 
			 // Create a list of task queues, one for each worker.
			 
			 mTaskQueues = new ArrayList<SynchronizedQueue<TaskWrapper>>();
			 
			 for(int i = 0; i < numWorkers; i++){
				 mTaskQueues.add(new SynchronizedQueue<TaskWrapper>());
			 }
			 
			 
			 // 1. Take numWorkers tasks from the main task queue.
			 // 2. Add each of these tasks to a different worker's task queue (one per queue).
			 // 3. Repeat steps 1 and 2 until all the tasks have been expended.
			 // 4. When the main task queue is empty, take note of the last worker.
			 
			try{
				 while(!mTaskQueue.isEmpty()){
					 for(int i = 0; i < numWorkers; i++){
						 if(mTaskQueue.isEmpty()){
							 break;
						 } else{
						 	TaskWrapper taskWrapper = mTaskQueue.take();
						 	mTaskQueues.get(i).put(taskWrapper);
						 	if(mTaskQueue.isEmpty()){
						 		mEndWorker = i;
						 	}
						 }
					 }
				 }
				 
			} catch(InterruptedException e){
				e.printStackTrace();
			}
			 
			

			 

	        
	}
	
	
	public void start(){
		// TODO Auto-generated method stub
		
		int numWorkers = mWorkers.length;
		
		// Create a new thread for each worker, pass in the task queue and start the thread.
		// The thread will submit the tasks in the queue to the appropriate worker and retrieve
		// results in real-time. Every time the thread retrieves a result unit, it is placed
		// into a common chain space which sequences the arriving result units. The calling
		// thread in the master node then retrieves result units from the chain space in real
		// time and places them on the result queue which is accessible to the client.
		
		 for(int i = 0; i < numWorkers; i++){
			 mThreads.add(new WorkerLinkThread(mWorkers[i], mChainSpace, mChecklist, TaskType.GENERIC));
		 }
		 for(int i = 0; i < mThreads.size(); i++){
			 mThreads.get(i).setNumTotalTasks(mTaskQueue.size());
			 if(i == mEndWorker){
				 mThreads.get(i).setAsEndWorker();
				 System.out.println("End worker: " + i);
			 }
			 mThreads.get(i).initCodeTransfer(mPackageName, mStdPackageName, mArgURLs, mWorkURLs, mArgNames, mWorkNames);
			 mThreads.get(i).setTasks(mTaskQueues.get(i));
			 mThreads.get(i).start();
		 }
		
	}
	
	
	public boolean getResultUnit(IndexedResult output) throws InterruptedException, IllegalStateException{
		// TODO Auto-generated method stub
		
		boolean done = false;
		System.out.println("yo1");
		// If the result queue is empty, populate it with the next available result units.
		if(mResultQueue.isEmpty()){
			System.out.println("yo2");
			// First check if all the results have already been taken.
			if(mChecklist.isEmpty() && mChainSpace.getNodes().size() == 1){
				mChainSpace.close();
				for(int i = 0; i < mThreads.size(); i++){
					mThreads.get(i).interrupt();
				}
				return true;
			} else{
				System.out.println("yo2");
					ArrayList<Object> buffer = new ArrayList<Object>();
					System.out.println("yo3");
					done = mChainSpace.getObjectBuffer(buffer);
					System.out.println("yo4");
					for(int i = 0; i < buffer.size(); i++){
						mResultQueue.put(buffer.get(i));
					}
					System.out.println("yo5");
				
			}
			
		}
		System.out.println("yo6");
		if(!mResultQueue.isEmpty()){
			output.setResult(mResultQueue.take());
		}
		
		if(mResultQueue.isEmpty() && done){
			System.out.println("yo7, mChecklist is empty: " + mChecklist.isEmpty());
			System.out.println("nodes left: " + mChainSpace.getNodes().size());
			if(mChecklist.isEmpty() && mChainSpace.getNodes().size() == 1){
				mChainSpace.close();
				System.out.println("Closed chain space");
				for(int i = 0; i < mThreads.size(); i++){
					System.out.println("Interrupting thread " + i);
					mThreads.get(i).interrupt();
				}
				System.out.println("Get result unit returning true");
				return true;
			}
			System.out.println("Get result unit returning false");
			return false;
		} else{
			System.out.println("Result queue wasn't even empty");
			return false;
		}
		
	}
	
	

	
	public static enum IntegrationAlgorithm{
		TRAPEZIUM, SIMPSON, CRK
	}
	


	
//  // A sample implementation of UnitTask.	
//	public class UnitTaskImpl extends UnitTask{
//
//		/**
//		 * 
//		 */
//		private static final long serialVersionUID = 1L;
//
//		public UnitTaskImpl(Class<?> inputType, Class<?> outputType) {
//			super(inputType, outputType);
//			// TODO Auto-generated constructor stub
//		}
//
//		@Override
//		public Object execute(SynchronizedQueue<?> unitData, Serializable[] parameters) throws Exception {
//			// TODO Auto-generated method stub
//			Class<?> inputType = getInputType();
//			return null;
//		}
//		
//	}


	

}
