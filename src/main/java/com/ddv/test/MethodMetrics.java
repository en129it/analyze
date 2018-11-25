package com.ddv.test;

public class MethodMetrics {
	public int instructionCount = 0;
	public int branchCount = 0;
	public boolean isSuperCalled = false;
	
	public void incInstructionCount() {
		instructionCount++;
	}
	
	public void incBranchCount() {
		branchCount++;
	}
	
	public void setSuperCalled() {
		isSuperCalled = true;
	}
}
