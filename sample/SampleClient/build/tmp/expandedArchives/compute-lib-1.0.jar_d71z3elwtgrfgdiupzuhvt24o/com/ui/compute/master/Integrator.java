package com.ui.compute.master;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

import com.ui.compute.lib.IndexedTask;
import com.ui.compute.lib.IntegrationAlgorithm;
import com.ui.compute.lib.Task;
import com.ui.compute.lib.TaskType;


public class Integrator{
	
	private String[] mWorkers;
	private ArrayList<LinkedBlockingQueue<IndexedTask>> mTaskQueues;
	private ChainSpace mChainSpace;
	private LinkedBlockingQueue<BigDecimal> mResultQueue;
	private Set<Integer> mChecklist;
	private ArrayList<WorkerLinkThread> mThreads;

	public Integrator(String[] workers){
		mWorkers = workers;
		mThreads = new ArrayList<WorkerLinkThread>();
		mChainSpace = new ChainSpace(BigDecimal.class);
		mResultQueue = new LinkedBlockingQueue<BigDecimal>();
		mChecklist = Collections.synchronizedSet(new HashSet<Integer>());
		
	}
	

	
	
	
	public void init(Function<BigDecimal, BigDecimal> f, BigDecimal start, BigDecimal end, int stepsPerUnit, int responsiveness){
		// TODO Auto-generated method stub
		
		 if (System.getSecurityManager() == null) {
	            System.setSecurityManager(new SecurityManager());
	        }
		 
		 BigDecimal range = end.subtract(start);
		 
			 
			 int numWorkers = mWorkers.length;
			 LinkedBlockingQueue<IndexedTask> taskQueue = new LinkedBlockingQueue<IndexedTask>();
			 BigDecimal workUnitLength = range.divide(new BigDecimal(numWorkers)).divide(new BigDecimal(responsiveness));
			 BigDecimal one = new BigDecimal(1);
			 
			 // Create a queue of tasks, one task for each work unit.
			 
			 for(BigDecimal currentUnit = new BigDecimal(0);
					 (currentUnit.multiply(workUnitLength)).compareTo(end) < 0;
					 currentUnit = currentUnit.add(one)){
				 
			 IntegrateTask integrateTask = new IntegrateTask(
					 f, currentUnit.multiply(workUnitLength), currentUnit.multiply(workUnitLength).add(workUnitLength),
					 stepsPerUnit, IntegrationAlgorithm.SIMPSON);
			 
			 IndexedTask indexedTask = new IndexedTask(currentUnit.intValue(), (Task<? extends Serializable>) integrateTask);
			 taskQueue.add(indexedTask);
			 
			 // Add the index of each task to a global checklist. As corresponding results are retrieved
			 // they will each be checked off the list. Once the list is empty, the threads and the chain
			 // space will shut down.
			 mChecklist.add(currentUnit.intValue());
			 
			 }
			 
			 // Create a list of task queues, one for each worker.
			 
			 mTaskQueues = new ArrayList<LinkedBlockingQueue<IndexedTask>>();
			 
			 for(int i = 0; i < numWorkers; i++){
				 mTaskQueues.add(new LinkedBlockingQueue<IndexedTask>());
			 }
			 
			 
			 // 1. Take numWorkers tasks from the task queue.
			 // 2. Add each of these tasks to a different worker's task queue (one per queue).
			 // 3. Repeat steps 1 and 2 until all the tasks have been expended.
			 
			try{
				 while(!taskQueue.isEmpty()){
					 for(int i = 0; i < numWorkers; i++){
						 if(taskQueue.isEmpty()){
							 break;
						 } else{
						 	IndexedTask task = taskQueue.take();
						 	mTaskQueues.get(i).add(task);
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
			 mThreads.add(new WorkerLinkThread(mWorkers[i], mChainSpace, mChecklist, TaskType.INTEGRATION));
		 }
		 for(int i = 0; i < mThreads.size(); i++){
			 mThreads.get(i).setTasks(mTaskQueues.get(i));
			 mThreads.get(i).start();
		 }
		
	}
	
	
	public BigDecimal getResultUnit() throws InterruptedException, IllegalStateException{
		// TODO Auto-generated method stub
		
		// If the result queue is empty, populate it with the next available result units.
		if(mResultQueue.isEmpty()){
			// First check if all the results have already been taken.
			if(mChecklist.isEmpty() && mChainSpace.getNodes().size() == 0){
				mChainSpace.close();
				for(int i = 0; i < mThreads.size(); i++){
					mThreads.get(i).interrupt();
				}
				return null;
			} else{
					ArrayList<Object> buffer = new ArrayList<Object>();
					boolean done = mChainSpace.getObjectBuffer(buffer);
					for(int i = 0; i < buffer.size(); i++){
						mResultQueue.put((BigDecimal) buffer.get(i));
					}
				
			}
			
		}
		
		return mResultQueue.take();
		
	}


}
