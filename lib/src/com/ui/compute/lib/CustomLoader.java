package com.ui.compute.lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class CustomLoader extends ClassLoader{
	
	public CustomLoader(ClassLoader parent){
		super(parent);
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		// TODO Auto-generated method stub
		File file = new File(getSimpleName(name) + ".class");
        FileInputStream fis = null;
        Class<?> clazz = null;
        try {
                fis = new FileInputStream(file);
                int content = 0;
                int i = 0;
                byte[] data = new byte[fis.available()];
                while ((content = fis.read()) != -1) {
                        // convert to char and display it
                        data[i] = (byte) content;
                        i++;
                }
                clazz = defineClass(name, data, 0, data.length);
        } catch (IOException e) {
                        e.printStackTrace();
        } finally {
            try {
                if (fis != null)
                        fis.close();
            } catch (IOException ex) {
        		ex.printStackTrace();
        	}
        }
        return clazz;
	}

	private String getSimpleName(String name) {
		// TODO Auto-generated method stub
		int i = name.lastIndexOf(".");
		return name.substring(i + 1);
	}

	
	
}
