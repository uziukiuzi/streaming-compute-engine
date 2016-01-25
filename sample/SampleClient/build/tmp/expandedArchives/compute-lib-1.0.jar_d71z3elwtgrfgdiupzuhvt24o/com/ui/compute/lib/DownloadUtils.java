package com.ui.compute.lib;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ArrayList;

public class DownloadUtils {

	
	public static Object[] downloadArgumentClasses(String packageName, ObjectOutputStream out, ObjectInputStream in) throws IOException, ClassNotFoundException{
		
		URL[] urls = new URL[2];
		String[] names = new String[2];
		
		// Notify client that we are ready to download argument classes.
		out.writeObject(Constants.READY_FOR_ARGS);
		
		String currentClass = null;
		FileOutputStream fos = null;
		int bufferSize = 0;
		byte[] buffer = null;
		
		// Get the name of the class.
		currentClass = (String) in.readObject(); // Input type
		names[0] = currentClass;
		String presentOrNot = (String) in.readObject();
		if(Constants.NOT_PRESENT.equals(presentOrNot)){
			File classFile = new File(currentClass + ".class");
			fos = new FileOutputStream(classFile);
			
			// Read the size of the buffer carrying the class file.
			bufferSize = (Integer) in.readObject();
			buffer = new byte[bufferSize];
			
			// Get the data buffer.
			buffer = (byte[]) in.readObject();
			
			// Write the buffer to the file.
			fos.write(buffer, 0, bufferSize);
			fos.close();
			
			// Add the url of the input class to the array.
			urls[0] = new URL(classFile.toURI().toString());
		}
		
		
		
		// Repeat for output type
		currentClass = (String) in.readObject(); // Output type
		names[1] = currentClass;
		presentOrNot = (String) in.readObject();
		if(Constants.NOT_PRESENT.equals(presentOrNot)){
			File classFile = new File(currentClass + ".class");
			fos = new FileOutputStream(classFile);
			
			bufferSize = (Integer) in.readObject();
			buffer = new byte[bufferSize];
			
			buffer = (byte[]) in.readObject();
			
			fos.write(buffer, 0, bufferSize);
			fos.close();
			
			urls[1] = new URL(classFile.toURI().toString());
		}
		
		// If argument URLs are not needed, the elements of urls are null.
		return new Object[]{names, urls};
	}
	
	
	public static Object[] downloadWorkClasses(String packageName, ObjectOutputStream out, ObjectInputStream in) throws IOException, ClassNotFoundException{
		
		ArrayList<URL> list = new ArrayList<URL>();
		ArrayList<String> namesList = new ArrayList<String>();
		
		
		// Notify client that we are ready to download work classes.
		out.writeObject(Constants.READY_FOR_WORK);
		
		// Download each class and write it to a file in the given package.
		boolean finished = false;
		String currentClass = null;
		FileOutputStream fos = null;
		int bufferSize = 0;
		byte[] buffer = null;
		
		while(!finished){
			
			// Get the name of the class.
			currentClass = (String) in.readObject();
			File classFile = new File(currentClass + ".class");
			fos = new FileOutputStream(classFile);
			
			// Read the size of the buffer carrying the class file.
			bufferSize = (Integer) in.readObject();
			buffer = new byte[bufferSize];
			
			// Get the data buffer.
			buffer = (byte[]) in.readObject();
			// Write the buffer to the file.
			fos.write(buffer, 0, bufferSize);
			fos.close();
			
			// Check to see if this was the last class.
			if((Boolean) in.readObject()){
				finished = true;
				// No more iterations.
			}
			
			// Add the class file URL to the list.
			list.add(classFile.toURI().toURL());
			// Add the name to the names list.
			namesList.add(currentClass);
		}
		
		String[] names = new String[namesList.size()];
		for(int i = 0; i < namesList.size(); i++){
			names[i] = namesList.get(i);
		}
		URL[] urls = new URL[list.size()];
		for(int i = 0; i < list.size(); i++){
			urls[i] = list.get(i);
		}
		
		return new Object[]{names, urls};
		
	}
	
	
}
