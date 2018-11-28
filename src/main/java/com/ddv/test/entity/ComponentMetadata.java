package com.ddv.test.entity;

import com.ddv.test.SQLInsertBuilder;

public class ComponentMetadata implements IMetadata {

	private int id;
	private ClassMetadata classMetadata;
	private Integer referencedComponentCount;
	
	public ComponentMetadata(int anId, ClassMetadata aClass) {
		id = anId;
		classMetadata = aClass;
		if (classMetadata==null) {
			referencedComponentCount = 0;
		}
	}
	
	public int resolveReferencedComponentCount() {
		if (referencedComponentCount==null) {
			referencedComponentCount = classMetadata.resolveReferencedComponentCount();
		}
		return referencedComponentCount.intValue();
	}

	public String generateSQLInsert() {
		StringBuilder rslt = new StringBuilder();

		SQLInsertBuilder builder = (classMetadata!=null) ? new SQLInsertBuilder(rslt, "COMPONENT", "ID", "CLASS_ID") : new SQLInsertBuilder(rslt, "COMPONENT", "ID");
		builder.addNumber(id);
		if (classMetadata!=null) {
			builder.addNumber(classMetadata.getId());
		}
		builder.flush();
		
		return rslt.toString();
	}
	
	@Override
	public int getId() {
		return id;
	}
}
