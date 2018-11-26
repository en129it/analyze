package com.ddv.test.entity;

public class ComponentMetadata {

	private ClassMetadata classMetadata;
	private Integer referencedComponentCount;
	
	public ComponentMetadata(ClassMetadata aClass) {
		classMetadata = aClass;
	}

	public int resolveReferencedComponentCount() {
		if (referencedComponentCount==null) {
			referencedComponentCount = classMetadata.resolveReferencedComponentCount();
		}
		return referencedComponentCount.intValue();
	}
}
