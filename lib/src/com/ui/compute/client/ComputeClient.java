package com.ui.compute.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.ui.compute.lib.Constants;
import com.ui.compute.lib.SynchronizedQueue;
import com.ui.compute.lib.TaskType;


/**
 * Class ComputeClient
 * This is the client class for the distributed processor. To connect to
 * a cluster, instantiate this class with the appropriate IP and port of
 * the master node, call init(...) with all the required parameters and
 * then call start(). To acquire a result unit, call getResultUnit().
 */
public class ComputeClient{
	
	private String mPackageName;
	private String mStdPackageName;
	private String[] mArgNames;
	private String[] mWorkNames;
	private URL[] mArgURLs;
	private URL[] mWorkURLs;
	private SynchronizedQueue<?> mData;
	private int mUnitSize;
	private boolean mDone = false;
	private String mMasterIP;
	private int mMasterPort;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	private Socket mConnection;

	
	/**
	 * Constructs a new client for the given IP address and port of the master node.
	 * 
	 * @param masterIP the IP address of the master node
	 * @param masterPort the port on which the master node is listening
	 */
	public ComputeClient(String masterIP, int masterPort){
		mMasterIP = masterIP;
		mMasterPort = masterPort;
	}
	
	/**
	 * Initialises the client with all the necessary preliminary information.
	 * 
	 * @param task the URL of the class file of the custom UnitTask subclass
	 * @param args if input/output types are not in the system library, this is a 2-element array of the URLs of their class files, otherwise {null, null}
	 * @param dependencies an array of the URLs to the class files of any custom classes used
	 * @param taskName the name of the custom UnitTask subclass to execute on the cluster
	 * @param argNames a 2-element array comprised of the simple names of the input and output classes
	 * @param dependencyNames an array comprised of the simple names of any custom classes used (in the same order as in dependencies)
	 * @param packageName the name of the package in which all custom classes are (task, args and dependencies)
	 * @param data the data to be processed
	 * @param unitSize the number of data points to be given to each instance of the custom UnitTask subclass
	 * @param params a list of any additional parameters required by the task
	 * @throws UnknownHostException if the host cannot be found
	 * @throws IOException if there is an I/O problem
	 * @throws URISyntaxException if there is a problem with the URLs
	 * @throws ClassNotFoundException if the correct custom classes haven't been specified
	 * @throws InterruptedException if a blocking queue is interrupted while waiting for a result
	 */
	public void init(URL task, URL[] args, URL[] dependencies,
			String taskName, String[] argNames, String[] dependencyNames,
			String packageName, SynchronizedQueue<?> data, int unitSize,
			ArrayList<Serializable> params) 
			throws
			UnknownHostException, IOException, URISyntaxException,
			ClassNotFoundException, InterruptedException{
		
		mData = data;
		mStdPackageName = packageName;
		mPackageName = mStdPackageName.replace(".", "\\");
		mUnitSize = unitSize;
		
		
		
	        	
	        	mWorkNames = new String[dependencyNames.length + 1];
	        	mWorkURLs = new URL[dependencies.length + 1];
	        	
	        	mArgNames = argNames;
	        	mWorkNames[0] = taskName;
	        	for(int i = 0; i < dependencyNames.length; i++){
	        		mWorkNames[i + 1] = dependencyNames[i];
	        	}
	        	mArgURLs = args;
	        	mWorkURLs[0] = task;
	        	for(int i = 0; i < dependencies.length; i++){
	        		mWorkURLs[i + 1] = dependencies[i];
	        	}
	        	
	        	mConnection = new Socket(mMasterIP, mMasterPort);
	            
				out = new ObjectOutputStream(mConnection.getOutputStream());
				in = new ObjectInputStream(mConnection.getInputStream());
				if(Constants.INITIATE.equals(in.readObject())){
					out.writeObject(TaskType.GENERIC);
				}
				if(Constants.REQUEST_PACKAGE.equals(in.readObject())){
					out.writeObject(mPackageName);
					out.writeObject(mStdPackageName);
				}
				if(Constants.READY_FOR_ARGS.equals(in.readObject())){
					// Input
					out.writeObject(mArgNames[0]);
					if(mArgURLs[0] == null){
						out.writeObject(Constants.ALREADY_PRESENT);
					} else{
						out.writeObject(Constants.NOT_PRESENT);
						File file = new File(mArgURLs[0].toURI());
						FileInputStream fis = new FileInputStream(file);
						int bufferSize = fis.available();
						out.writeObject((Integer) bufferSize);
						byte[] buffer = new byte[bufferSize];
						fis.read(buffer, 0, bufferSize);
						out.writeObject(buffer);
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
				if(Constants.READY_FOR_WORK.equals(in.readObject())){
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
				
				if(Constants.REQUEST_DATA.equals(in.readObject())){
					out.writeObject(mData);
				}
				
				if(Constants.REQUEST_PARAMS.equals(in.readObject())){
					sendParams(params, out);
				}
				
				if(Constants.REQUEST_UNIT_SIZE.equals(in.readObject())){
					out.writeObject(mUnitSize);
				}
				
	}
		
	        
	/**
	 * Starts the processor.
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public void start() throws ClassNotFoundException, IOException{
		
		if(Constants.READY_TO_START.equals(in.readObject())){
			out.writeObject(Constants.START);
			System.out.println("ComputeClient just started proc");
			
		}
		
	}
	
	/**
	 * Returns true if all the result units have been returned by getResultUnit().
	 * This should be used as the condition in a while loop which calls getResultUnit().
	 */
	public boolean done(){
		return mDone;
	}
	
	/**
	 * Returns the next available result unit, blocking until one is available.
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public Object getResultUnit() throws IOException, ClassNotFoundException{
		if(mDone){
			return null;
		}
		out.writeObject(Constants.REQUEST_RESULT);
		Object resultUnit = in.readObject();
		mDone = (boolean) in.readObject();
		return resultUnit;
	}
	
	/**
	 * Closes the connection with the master. Must be called in a finally block
	 * after all processing is done.
	 */
	public void close(){
		try {
			mConnection.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Utility method to send the parameters to the master
	 */
	private static void sendParams(List<Serializable> params, ObjectOutputStream out) throws IOException {
		// TODO Auto-generated method stub
		out.writeObject(params.size());
		
		for(int i = 0; i < params.size(); i++){
			out.writeObject(params.get(i).getClass().getName());
			out.writeObject(params.get(i));
		}
	}
	
}