package com.ui.compute.master;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.ui.compute.lib.ClassLoaderObjectInputStream;
import com.ui.compute.lib.Constants;
import com.ui.compute.lib.CustomLoader;
import com.ui.compute.lib.DownloadUtils;
import com.ui.compute.lib.IndexedResult;
import com.ui.compute.lib.SynchronizedQueue;
import com.ui.compute.lib.TaskType;
import com.ui.compute.lib.UnitTask;

public class ComputeMaster{
	
	private String[] mWorkers;
	
	public ComputeMaster(String[] workers){
		mWorkers = workers;
	}
	
	
	public void start(){
		
		
		ServerSocket server = null;
		Socket client = null;
		
		try {
			server = new ServerSocket(Constants.MASTER_PORT);
			System.out.println("Waiting...");
			client = server.accept();
			System.out.println("Got connection");
			// Initialise output and input streams to and from the client.
			ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
			ClassLoaderObjectInputStream in = new ClassLoaderObjectInputStream(ClassLoader.getSystemClassLoader(), client.getInputStream());
			
			// Initiate a conversation - ask the client what kind of request it wants to make.
			out.writeObject(Constants.INITIATE);
			// Find out the answer.
			TaskType resp1 = (TaskType) in.readObject();
			
			// Deal with the particular type of request.
			if(resp1.equals(TaskType.INTEGRATION)){
				
			} else if(resp1.equals(TaskType.GENERIC)){
				
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
				
				GenericProcessor processor = (GenericProcessor) new GenericProcessor(mWorkers, inputType, outputType);
				processor.initCodeTransfer(packageName, stdPackageName, argURLs, workURLs, argumentClassNames, workClassNames);
//				argsLoader.close();
//				workLoader.close();
				
				
				// Obtain the data, parameters and execution unit size from the client.
				out.writeObject(Constants.REQUEST_DATA);
				SynchronizedQueue<?> data = (SynchronizedQueue<?>) in.readObject();
				out.writeObject(Constants.REQUEST_PARAMS);
				//Serializable[] params = (Serializable[]) in.readObject();
				in.setClassLoader(workLoader);
				List<Serializable> params = receiveParams(in, workLoader);
				out.writeObject(Constants.REQUEST_UNIT_SIZE);
				int unitSize = (int) in.readObject();
				
				// Initialise the processor with the above attributes.
				Constructor<?> constructor = workLoader.loadClass(stdPackageName + "." + workClassNames[0]).getConstructor(new Class<?>[]{Class.class, Class.class});
				processor.initProcessor(constructor, data, params, unitSize);
				
				// Notify the client that we are ready to begin processing.
				System.out.println("ComputeMaster about to say ready to start");
				out.writeObject(Constants.READY_TO_START);
				System.out.println("ComputeMaster said ready to start");
				
				// Given the go-ahead, start the processor.
				if(Constants.START.equals(in.readObject())){
					System.out.println("ComputeMaster about to start genproc");
					processor.start();
					System.out.println("ComputeMaster started genproc");
					
				}
				
				
				// Start listening for requests from the client to get result units.
				
				IndexedResult wrapper = new IndexedResult(0, null);
				System.out.println("Hey1");
				boolean done = processor.getResultUnit(wrapper);
				System.out.println("Hey2");
				Object result = wrapper.getResult();
				System.out.println("Hey3");
				//Object result = outputType.cast(resultObj);
				//System.out.println("Cast 2");
				while(result != null){
					System.out.println("Hey4");
					if(Constants.REQUEST_RESULT.equals(in.readObject())){
						System.out.println("Hey5");
						out.writeObject(result);
						System.out.println("Hey6");
						//result = outputType.cast(processor.getResultUnit());
						done = processor.getResultUnit(wrapper);
						System.out.println("Hey7");
						result = wrapper.getResult();
						System.out.println("Hey8");
						
						if(done){
							System.out.println("Hey9");
							out.writeObject(true);
							System.out.println("Hey10");
							break;
						} else{
							System.out.println("Hey11");
							out.writeObject(false);
							System.out.println("Hey12");
						}
					}
				}
				
				System.out.println("Master: Done!");
				
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
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			try{
				client.close();
			} catch(IOException e){
				e.printStackTrace();
			}
		}
	
	}
	
	
	public String[] getAvailableWorkers() {
		// TODO Auto-generated method stub
		return mWorkers;
	}
	
	
	private static List<Serializable> receiveParams(ObjectInputStream in, CustomLoader loader)
			throws IOException, ClassNotFoundException, InterruptedException{
		
		int numParams = (int) in.readObject();
		ArrayList<Serializable> params = new ArrayList<Serializable>();
		String name = null;
		Class<?> clazz = null;
		
		for(int i = 0; i < numParams; i++){
			name = (String) in.readObject();
			System.out.println("check 1");
			clazz = loader.loadClass(name);
			System.out.println("check 2");
			params.add(Serializable.class.cast(clazz.cast(in.readObject())));
			System.out.println("check 3");
		}
		
		return params;
		
	}
	
}