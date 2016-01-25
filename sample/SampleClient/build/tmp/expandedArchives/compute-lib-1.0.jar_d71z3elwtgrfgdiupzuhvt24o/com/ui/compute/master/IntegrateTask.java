package com.ui.compute.master;

import java.math.BigDecimal;
import java.util.function.Function;

import com.ui.compute.lib.IntegrationAlgorithm;
import com.ui.compute.lib.Task;

public class IntegrateTask implements Task<BigDecimal>{
	
	Function<BigDecimal, BigDecimal> mFunction;
	BigDecimal mStart;
	BigDecimal mEnd;
	BigDecimal mStepSize;
	BigDecimal mNumSteps;
	
	IntegrationAlgorithm mAlgorithm;

	public IntegrateTask(Function<BigDecimal, BigDecimal> f, BigDecimal start, BigDecimal end, int numSteps, IntegrationAlgorithm algorithm) {
		mFunction = f;
		mStart = start;
		mEnd = end;
		mNumSteps = new BigDecimal(numSteps);
		mStepSize = mEnd.subtract(mStart).divide(mNumSteps);
		mAlgorithm = algorithm;
	}

	@Override
	public BigDecimal execute() {
		// TODO Auto-generated method stub
		if(mAlgorithm.equals(IntegrationAlgorithm.SIMPSON)){
			
			BigDecimal result = new BigDecimal(0);
			BigDecimal currentStart = new BigDecimal(0);
			BigDecimal currentEnd = new BigDecimal(0);
			BigDecimal index = new BigDecimal(0);
			BigDecimal one = new BigDecimal(1);
			BigDecimal four = new BigDecimal(4);
			BigDecimal six = new BigDecimal(6);
			
			for(int i = 0; i < mNumSteps.intValue(); i++){
				currentStart = index.multiply(mStepSize);
				currentEnd = currentStart.add(mStepSize);
				result.add(currentEnd.subtract(currentStart).divide(six)
						.multiply(mFunction.apply(currentStart)
								.add(four.multiply(mFunction.apply(mean(currentStart, currentEnd))))
								.add(mFunction.apply(currentEnd))));
				index = index.add(one);
			}
			
			return result;
			
		} else{
			return null;
		}
	}

	private BigDecimal mean(BigDecimal a, BigDecimal b) {
		// TODO Auto-generated method stub
		return a.add(b).divide(new BigDecimal(2));
	}

}
