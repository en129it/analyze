package com.ddv.test.entity;

import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

import com.ddv.test.MethodMetrics;

public class ConstructorMetadata implements IMetadata {

	private int id;
	private ArrayList<String> methodArgs;
	private MethodMetrics metrics;
	
	public ConstructorMetadata(int anId, ArrayList<String> aMethodArgs, MethodMetrics aMetrics) {
		id = anId;
		methodArgs = aMethodArgs;
		metrics = aMetrics;
	}
	
	public boolean hasSameSignature(ConstructorMetadata aMethod) {
		return Objects.equals(aMethod.methodArgs, methodArgs);
	}
	
	public int getDeclaredInstructionCount() {
		return metrics.instructionCount;
	}
	
	public int getDeclaredBranchCount() {
		return metrics.branchCount;
	}
	
	@Override
	public String toString() {
		return "Constructor " + "(" + ((methodArgs!=null) ? methodArgs.stream().collect(Collectors.joining(", ")) : "") + ") [lines=" + metrics.instructionCount + "][is super called? " + metrics.isSuperCalled + "]";
	}

	//**** IMetadata implementation ********************************************
	
	@Override
	public int getId() {
		return id;
	}

	@Override
	public String generateSQLInsert() {
		return null;
	}
	
}
