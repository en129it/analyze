package com.ddv.test.entity;

public class ComponentMetadata {

	private int id;
	private ClassMetadata classMetadata;
	private Integer referencedComponentCount;
	
	public ComponentMetadata(int anId, ClassMetadata aClass) {
		id = anId;
		classMetadata = aClass;
	}

	public int getId() {
		return id;
	}
	
	public int resolveReferencedComponentCount() {
		if (referencedComponentCount==null) {
			referencedComponentCount = classMetadata.resolveReferencedComponentCount();
		}
		return referencedComponentCount.intValue();
	}
}
