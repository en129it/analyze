package com.ddv.test.entity;

import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

import com.ddv.test.MethodMetrics;
import com.ddv.test.Tapestry;

public class MethodMetadata implements IMetadata {

	private int id;
	private String name;
	private boolean isAbstract;
	private ArrayList<String> methodArgs;
	private String methodReturnType;
	private MethodMetrics metrics;
	private String componentAnnotationValue;
	private boolean isComponentAnnotationValueType;
	private ComponentMetadata componentMetadata;
	
	public MethodMetadata(int anId, String aName, ArrayList<String> aMethodArgs, String aMethodReturnType, boolean aIsAbstract, MethodMetrics aMetrics) {
		id = anId;
		name = aName;
		methodArgs = aMethodArgs;
		methodReturnType = aMethodReturnType;
		metrics = aMetrics;
		isAbstract = aIsAbstract;
	}
	
	public void setComponentAnnotationValue(String aValue, boolean isType) {
		componentAnnotationValue = aValue;
		isComponentAnnotationValueType = isType;
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

	public String getMethodName() {
		return name;
	}
	
	public boolean isAbstract() {
		return isAbstract;
	}
	
	public void postProcessComponents(ClassMetadata aContext, Tapestry aTapestry, MetadataFactory aMetadataFactory) {
		String componentName = this.resolveComponentName(aContext);
		componentMetadata = aTapestry.resolveComponentName(aContext, componentName);
	}
	
	public String resolveComponentName(ClassMetadata aContext) {
		if (componentAnnotationValue!=null) {
			return (isComponentAnnotationValueType) ? componentAnnotationValue : aContext.resolveComponentCopyOf(componentAnnotationValue);
		}
		return null;
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
	
}
