package com.ui.compute.sample.client;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import com.ui.compute.lib.IntegrationAlgorithm;
import com.ui.compute.lib.SynchronizedQueue;
import com.ui.compute.lib.UnitTask;

public class ClientTask extends UnitTask{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ClientTask(Class<?> inputType, Class<?> outputType) {
		super(inputType, outputType);
		// TODO Auto-generated constructor stub
	}
	

	@Override
	public void setSupplementaries(List<SynchronizedQueue<Serializable>> chunks, List<Serializable> supplementaries){
		
		int index = getIndex();
		
		try{
			if(index == 0){
				SynchronizedQueue<Serializable> nextQ = new SynchronizedQueue<Serializable>(chunks.get(index + 1));
				ArrayList<BigDecimal> next = new ArrayList<BigDecimal>();
				int qSize = nextQ.size();
				for(int i = 0; i < qSize; i++){
					next.add((BigDecimal) nextQ.take());
				}
				supplementaries.add(next.get(0));
			} else if(index == getTotalTasks() - 1){
				SynchronizedQueue<Serializable> previousQ = new SynchronizedQueue<Serializable>(chunks.get(index - 1));
				ArrayList<BigDecimal> previous = new ArrayList<BigDecimal>();
				int qSize = previousQ.size();
				for(int i = 0; i < qSize; i++){
					previous.add((BigDecimal) previousQ.take());
				}
				supplementaries.add(previous.get(previous.size() - 1));
			} else{
				
				SynchronizedQueue<Serializable> nextQ = new SynchronizedQueue<Serializable>(chunks.get(index + 1));
				SynchronizedQueue<Serializable> previousQ = new SynchronizedQueue<Serializable>(chunks.get(index - 1));
				ArrayList<BigDecimal> next = new ArrayList<BigDecimal>();
				ArrayList<BigDecimal> previous = new ArrayList<BigDecimal>();
				
				int qSize = nextQ.size();
				for(int i = 0; i < qSize; i++){
					next.add((BigDecimal) nextQ.take());
				}
				qSize = previousQ.size();
				for(int i = 0; i < qSize; i++){
					previous.add((BigDecimal) previousQ.take());
				}
				supplementaries.add(previous.get(previous.size() - 1));
				supplementaries.add(next.get(0));
				
			}
			
		} catch(InterruptedException e){
			e.printStackTrace();
		}
		
	}
	
	@Override
	public <T> Object execute(SynchronizedQueue<T> unitData, List<Serializable> parameters) throws Exception {
		// TODO Auto-generated method stub
		
		InputFunction function = (InputFunction) parameters.get(0);
		IntegrationAlgorithm algorithm = (IntegrationAlgorithm) parameters.get(1);
		
		int index = getIndex();
		BigDecimal endBoundary = null;
		
		if(algorithm.equals(IntegrationAlgorithm.SIMPSON)){
			
			BigDecimal result = new BigDecimal(0);
			BigDecimal currentStart = new BigDecimal(0);
			BigDecimal currentEnd = new BigDecimal(0);
			BigDecimal four = new BigDecimal(4);
			BigDecimal six = new BigDecimal(6);
			ArrayList<T> adjustedData = new ArrayList<T>();
			
			if(index == 0){
				
				endBoundary = (BigDecimal) getSupplementaries().get(0);
				
				int qSize = unitData.size();
				for(int i = 0; i < qSize; i++){
					adjustedData.add(unitData.take());
				}
				adjustedData.add((T) endBoundary);

			} else if(index == getTotalTasks() - 1){
				
				int qSize = unitData.size();
				for(int i = 0; i < qSize; i++){
					adjustedData.add(unitData.take());
				}
				
			} else{
				
				endBoundary = (BigDecimal) getSupplementaries().get(1);
				
				int qSize = unitData.size();
				for(int i = 0; i < qSize; i++){
					adjustedData.add(unitData.take());
				}
				adjustedData.add((T) endBoundary);
				
			}
			
			currentStart = (BigDecimal) adjustedData.remove(0);
			if(adjustedData.size() == 1){
				return result;
			}
			for(int count = 1; count < adjustedData.size(); count++){
				currentEnd = (BigDecimal) adjustedData.remove(count);
				result = result.add(currentEnd.subtract(currentStart, MathContext.DECIMAL64).divide(six, MathContext.DECIMAL64)
						.multiply(function.apply(currentStart)
								.add(four.multiply(function.apply(mean(currentStart, currentEnd))), MathContext.DECIMAL64)
								.add(function.apply(currentEnd), MathContext.DECIMAL64), MathContext.DECIMAL64), MathContext.DECIMAL64);
				currentStart = currentEnd;
			}
			
			return result;
			
		} else{
			return null;
		}
	}

	private BigDecimal mean(BigDecimal a, BigDecimal b) {
		// TODO Auto-generated method stub
		return a.add(b, MathContext.DECIMAL64).divide(new BigDecimal(2), MathContext.DECIMAL64);
	}

}
