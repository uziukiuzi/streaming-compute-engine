package com.ui.compute.worker;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;

import com.ui.compute.lib.ClassLoaderObjectInputStream;
import com.ui.compute.lib.Constants;
import com.ui.compute.lib.CustomLoader;
import com.ui.compute.lib.DownloadUtils;
import com.ui.compute.lib.IndexedResult;
import com.ui.compute.lib.SynchronizedQueue;
import com.ui.compute.lib.TaskType;
import com.ui.compute.lib.TaskWrapper;

public class ComputeWorker{
	
	public ComputeWorker(){
		
	}
	
	public void start(){
		ServerSocket workerSoc = null;
		Socket masterSoc = null;
		
		try {
			workerSoc = new ServerSocket(Constants.WORKER_PORT);
			System.out.println("Waiting for master...");
			masterSoc = workerSoc.accept();
			System.out.println("Got connection with master");
			
			ObjectOutputStream out = new ObjectOutputStream(masterSoc.getOutputStream());
			ClassLoaderObjectInputStream in = new ClassLoaderObjectInputStream(ClassLoader.getSystemClassLoader(), masterSoc.getInputStream());
			
			out.writeObject(Constants.INITIATE);
			
			TaskType taskType = (TaskType) in.readObject();
			
			// Request the package name of the client classes to be downloaded.
			out.writeObject(Constants.REQUEST_PACKAGE);
			
			// Format: com\\example\\packagename
			String packageName = (String) in.readObject();
			// Format: com.example.packagename
			String stdPackageName = (String) in.readObject();
			
			// These will be returned by the download methods.
			String[] argumentClassNames = null;
			String[] workClassNames = null;
			
			// First download the classes which define what the inputs/outputs to the task will be.
			Object[] arguments = DownloadUtils.downloadArgumentClasses(packageName, out, in);
			
			URL[] argURLs = (URL[]) arguments[1];
			
			CustomLoader argsLoader = new CustomLoader(ClassLoader.getSystemClassLoader());
			
			// Now download the classes which are used in the task.
			Object[] workClasses = DownloadUtils.downloadWorkClasses(packageName, out, in);
			URL[] workURLs = (URL[]) workClasses[1];
			CustomLoader workLoader = new CustomLoader(ClassLoader.getSystemClassLoader());
			// The first element of workURLs is the task implementation.
			
			argumentClassNames = (String[]) arguments[0];
			workClassNames = (String[]) workClasses[0];
			
			// Initialise the processor object with the input and output types.
			Class<?> inputType = null;
			Class<?> outputType = null;
			if(argURLs[0] != null && argumentClassNames.length > 0){
				inputType = argsLoader.loadClass(stdPackageName + "." + argumentClassNames[0]);
			}
			if(argURLs[1] != null && argumentClassNames.length > 1){
				outputType = argsLoader.loadClass(stdPackageName + "." + argumentClassNames[1]);
			}
			
			// Request the task queue.
			out.writeObject(Constants.REQUEST_TASKS);
			
			// Initialise a new worker object to wait for submitted tasks.
			WorkerObject workerObj = new WorkerObject(inputType, outputType);
			workerObj.init(taskType);
			
			if(taskType.equals(TaskType.INTEGRATION)){
				
			} else if(taskType.equals(TaskType.GENERIC)){
				
				in.setClassLoader(workLoader);
				// Get the task queue and send it to the worker object for processing.
				SynchronizedQueue<TaskWrapper> taskQueue = (SynchronizedQueue<TaskWrapper>) in.readObject();
				workerObj.submitGeneric(taskQueue);
				
				IndexedResult resultUnit = new IndexedResult(-30, new SerializableObj());
				String request = null;
				while(resultUnit.getResult() != null){
					request = (String) in.readObject();
					if(Constants.REQUEST_RESULT.equals(request)){
						// Get a result unit, blocking if necessary until one is available.
						resultUnit = workerObj.getIndexedResult();
						if(resultUnit.getResult() != null){
						} else{
						}
						// Send the result unit to the master.
						if(resultUnit.getResult() != null){
							out.writeObject(resultUnit);
						}
					} else{
						break;
					}
				}
				
				out.writeObject(Constants.DONE);
				System.out.println("Work done!");
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			try {
				masterSoc.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
	}
	
}