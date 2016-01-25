package com.ui.compute.sample.client;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;

import com.ui.compute.client.ComputeClient;
import com.ui.compute.lib.IntegrationAlgorithm;
import com.ui.compute.lib.PathUtils;
import com.ui.compute.lib.SynchronizedQueue;

public class Main {

	private static String mPackageName = "com.ui.compute.sample.client";
	private static SynchronizedQueue<BigDecimal> mData;
	private static SynchronizedQueue<BigDecimal> mResult;
	private static int mNumSteps = 10000;
	private static BigDecimal mStart = new BigDecimal(0);
	private static BigDecimal mEnd = new BigDecimal(1);
	private static BigDecimal mRange = mEnd.subtract(mStart);
	private static int mUnitSize = 10;
	private static ComputeClient client;
	
	public static void main(String[] args){
		
		mData = new SynchronizedQueue<BigDecimal>();
		mResult = new SynchronizedQueue<BigDecimal>();
		
		
		BigDecimal stepSize = mRange.divide(new BigDecimal(mNumSteps), MathContext.DECIMAL64);
		try {
			BigDecimal currentPoint = new BigDecimal(0);
			mData.put(currentPoint);
			for(int i = 1; i < mNumSteps + 1; i++){
				currentPoint = currentPoint.add(stepSize);
				mData.put(currentPoint);
				System.out.println("Generating input data, " + i + ": " + currentPoint);
			}
			
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		try{
		
			// Set up input parameters
			String homeDir = PathUtils.getParent(PathUtils.getParent(System.getProperty("user.dir")));
			String[] argNames = {"BigDecimal", "BigDecimal"};
			String taskName = "ClientTask";
			String[] dependencyNames = {"InputFunction", "FunctionName"};
			URL[] argURLs = {null, null}; // We don't need to send the BigDecimal class as it will be recognised anyway
			URL taskURL = new URL("file:\\" + homeDir + "\\bin\\" + mPackageName.replace(".", "\\") + "\\" + taskName + ".class");
			URL[] dependencyURLs = new URL[]{new URL("file:\\" + homeDir + "\\bin\\" + mPackageName.replace(".", "\\") + "\\" + dependencyNames[0] + ".class"),
										new URL("file:\\" + homeDir + "\\bin\\" + mPackageName.replace(".", "\\") + "\\" + dependencyNames[1] + ".class")};
			BigDecimal[] coeffs = new BigDecimal[2];
			coeffs[0] = new BigDecimal(0);
			coeffs[1] = new BigDecimal(1);
			ArrayList<Serializable> params = new ArrayList<Serializable>();
			params.add(new InputFunction(FunctionName.LINEAR, coeffs));
			params.add(IntegrationAlgorithm.SIMPSON);
			
			
			// Instantiate, initialise and start the client
			client = new ComputeClient(args[0], Integer.parseInt(args[1]));
			
			client.init(taskURL, argURLs, dependencyURLs,
						taskName, argNames, dependencyNames,
						mPackageName, mData, mUnitSize,
						params);
			client.start();
			
			
			// Get results
			BigDecimal unit = new BigDecimal(0);
			while(!client.done()){
				unit = unit.add((BigDecimal) client.getResultUnit());
				mResult.put(unit);
				System.out.println(unit);
			}
		
		} catch(InterruptedException e){
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			client.close();
		}
		
	}
	
}
