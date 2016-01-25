package com.ui.compute.master;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import com.ui.compute.lib.Constants;
import com.ui.compute.lib.IndexedResult;
import com.ui.compute.lib.IndexedTask;
import com.ui.compute.lib.SynchronizedQueue;
import com.ui.compute.lib.TaskType;
import com.ui.compute.lib.TaskWrapper;


public class WorkerLinkThread extends Thread {
	
	private String mWorker;
	private URL[] mArgURLs;
	private URL[] mWorkURLs;
	private String mPackageName;
	private String mStdPackageName;
	private String[] mArgNames;
	private String[] mWorkNames;
	private int mNumToProcess;
	private int mTotalToProcess;
	private boolean mIsEndWorker = false;
	private SynchronizedQueue<TaskWrapper> mTaskQueue;
	private LinkedBlockingQueue<IndexedTask> mTaskLBQ;
	private ChainSpace mChainSpace;
	private Set<Integer> mChecklist;
	private TaskType mTaskType;
	
	public WorkerLinkThread(String worker, ChainSpace chainSpace, Set<Integer> checklist, TaskType type){
		mWorker = worker;
		mChainSpace = chainSpace;
		mChecklist = checklist;
		mTaskType = type;
	}
	
	public void setAsEndWorker(){
		mIsEndWorker = true;
	}
	
	public void initCodeTransfer(String packageName, String stdPackageName, URL[] argURLs, URL[] workURLs, String[] argNames, String[] workNames){
		mArgURLs = argURLs;
		mWorkURLs = workURLs;
		mPackageName = packageName;
		mStdPackageName = stdPackageName;
		mArgNames = argNames;
		mWorkNames = workNames;
	}
	
	public void setTasks(LinkedBlockingQueue<IndexedTask> taskQueue){
		// For Integrator
		mTaskLBQ = taskQueue;
		mNumToProcess = mTaskLBQ.size();
	}
	
	public void setTasks(SynchronizedQueue<TaskWrapper> taskQueue){
		// For GenericProcessor
		mTaskQueue = taskQueue;
		mNumToProcess = mTaskQueue.size();
	}
	
	public void setNumTotalTasks(int num){
		mTotalToProcess = num;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		super.run();
		
		Socket connection = null;
		
		try{
			connection = new Socket(mWorker, Constants.WORKER_PORT);
			
			// Initialise output and input streams to and from the worker.
			ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
			ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
			
			if(mTaskType.equals(TaskType.INTEGRATION)){
				
			} else if(mTaskType.equals(TaskType.GENERIC)){
			
				Object fromWorker = null;
				
				fromWorker = in.readObject();
				if(Constants.INITIATE.equals(fromWorker)){
					out.writeObject(mTaskType);
				}
				fromWorker = in.readObject();
				if(Constants.REQUEST_PACKAGE.equals(fromWorker)){
					out.writeObject(mPackageName);
					out.writeObject(mStdPackageName);
				}
				fromWorker = in.readObject();
				if(Constants.READY_FOR_ARGS.equals(fromWorker)){
					// Input
					out.writeObject(mArgNames[0]);
					if(mArgURLs[0] == null){
						out.writeObject(Constants.ALREADY_PRESENT);
					} else{
						out.writeObject(Constants.NOT_PRESENT);
						File file = new File(mArgURLs[0].toURI());
						FileInputStream fis = new FileInputStream(file);
						int bufferSize = fis.available();
						out.writeObject(bufferSize);
						byte[] buffer = new byte[bufferSize];
						fis.read(buffer, 0, bufferSize);
						out.write(buffer, 0, bufferSize);
						fis.close();
					}
					
					// Output
					out.writeObject(mArgNames[1]);
					if(mArgURLs[1] == null){
						out.writeObject(Constants.ALREADY_PRESENT);
					} else{
						out.writeObject(Constants.NOT_PRESENT);
						File file = new File(mArgURLs[1].toURI());
						FileInputStream fis = new FileInputStream(file);
						int bufferSize = fis.available();
						out.writeObject(bufferSize);
						byte[] buffer = new byte[bufferSize];
						fis.read(buffer, 0, bufferSize);
						out.writeObject(buffer);
						fis.close();
					}
					
				}
				fromWorker = in.readObject();
				if(Constants.READY_FOR_WORK.equals(fromWorker)){
					File file = null;
					FileInputStream fis = null;
					int bufferSize = 0;
					byte[] buffer = null;
					
					for(int i = 0; i < mWorkNames.length; i++){
						out.writeObject(mWorkNames[i]);
						file = new File(mWorkURLs[i].toURI());
						fis = new FileInputStream(file);
						bufferSize = fis.available();
						out.writeObject(bufferSize);
						buffer = new byte[bufferSize];
						fis.read(buffer, 0, bufferSize);
						out.writeObject(buffer);
						fis.close();
						if(i == mWorkNames.length - 1){
							out.writeObject(true);
						} else{
							out.writeObject(false);
						}
					}
					
				}
				fromWorker = in.readObject();
				if(Constants.REQUEST_TASKS.equals(fromWorker)){
						out.writeObject(mTaskQueue);
				}
				
				
				IndexedResult resultUnit = null;
				Object temp = null;
				int count = 0;
				while(count < mNumToProcess + 1){
					out.writeObject(Constants.REQUEST_RESULT);
					temp = in.readObject();
					if(Constants.DONE.equals(temp)){
						break;
					}
					resultUnit = (IndexedResult) temp;
					if(resultUnit.getPosition() == mTotalToProcess - 1 && mIsEndWorker){
						// Tell the chain space that we're at the end of the entire dataset.
						System.out.println("end position: " + resultUnit.getPosition());
						mChainSpace.putObject(resultUnit.getPosition(), resultUnit.getResult(), true);
					} else{
						// Not at the end yet, put result into chain space.
						System.out.println("position: " + resultUnit.getPosition());
						mChainSpace.putObject(resultUnit.getPosition(), resultUnit.getResult(), false);
					}
					mChecklist.remove(resultUnit.getPosition());
					//notifyAll();
					count++;
				}
				
				System.out.println(mWorker + ": Done!");
				
				
			}
		} catch(IOException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			try {
				connection.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
			
		
		
		
		
			
		
		
	}


	
	

}
