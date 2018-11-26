package com.ddv.test.entity;

import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

import com.ddv.test.MethodMetrics;
import com.ddv.test.Tapestry;

public class MethodMetadata implements IMetadata {

	private int id;
	private String name;
	private ArrayList<String> methodArgs;
	private String methodReturnType;
	private MethodMetrics metrics;
	private String componentAnnotationValue;
	private ComponentMetadata componentMetadata;
	
	public MethodMetadata(int anId, String aName, ArrayList<String> aMethodArgs, String aMethodReturnType, MethodMetrics aMetrics) {
		id = anId;
		name = aName;
		methodArgs = aMethodArgs;
		methodReturnType = aMethodReturnType;
		metrics = aMetrics;
	}
	
	public void setComponentAnnotationValue(String aValue) {
		componentAnnotationValue = aValue;
	}
	public String getComponentAnnotationValue() {
		return componentAnnotationValue;
	}
	
	public ComponentMetadata getComponentMetadata() {
		return componentMetadata;
	}
	
	public boolean hasSameSignature(MethodMetadata aMethod) {
		return Objects.equals(aMethod.name, name)
			&& Objects.equals(aMethod.methodArgs, methodArgs)
			&& Objects.equals(aMethod.methodReturnType, methodReturnType);
	}
	
	public int getDeclaredInstructionCount() {
		return metrics.instructionCount;
	}
	
	public int getDeclaredBranchCount() {
		return metrics.branchCount;
	}

	public void postProcessComponents(ClassMetadata aContext, Tapestry aTapestry, MetadataFactory aMetadataFactory) {
		componentMetadata = aTapestry.resolveComponentName(aContext, componentAnnotationValue);
	}
	
	@Override
	public String toString() {
		return "Method " + ((componentAnnotationValue!=null) ? "@Component(\"" + componentAnnotationValue + "\") " : "") + methodReturnType + " " + name + "(" + ((methodArgs!=null) ? methodArgs.stream().collect(Collectors.joining(", ")) : "") + ") [lines=" + metrics.instructionCount + "][is super called? " + metrics.isSuperCalled + "]";
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
