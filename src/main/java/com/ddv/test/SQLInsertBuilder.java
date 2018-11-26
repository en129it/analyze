package com.ddv.test;

public class SQLInsertBuilder {

	private StringBuilder sql;
	private boolean isFirst = true;
	private boolean isClosed = false;
	
	public SQLInsertBuilder(StringBuilder anOut, String aTableName, String... aColumns) {
		sql = anOut;
		
		sql.append("INSERT INTO ").append(aTableName).append("(");
		boolean isFirst = true;
		for (String column : aColumns) {
			if (isFirst) {
				isFirst = false;
			} else {
				sql.append(",");
			}
			sql.append(column);
		}
		sql.append(") VALUES (");
	}
	
	public SQLInsertBuilder addString(String aValue) {
		return addHelper("'" + aValue + "'");
	}
	
	public SQLInsertBuilder addNumber(int aValue) {
		return addHelper(Integer.toString(aValue));
	}
	
	public SQLInsertBuilder addBoolean(boolean aFlag) {
		return addHelper("'" + (aFlag ? "Y" : "N") + "'");
	}
	
	private SQLInsertBuilder addHelper(String aValue) {
		if (isFirst) {
			isFirst = false;
		} else {
			sql.append(",");
		}
		sql.append(aValue);
		return this;
	}
	
	public void flush() {
		if (!isClosed) {
			sql.append(");\n");
			isClosed = true;
		}
	}
}
