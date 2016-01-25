package com.ui.compute.lib;

import java.io.Serializable;

public interface Task<T extends Serializable>{
	public T execute();
}