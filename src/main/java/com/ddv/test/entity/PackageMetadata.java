package com.ddv.test.entity;

import java.util.ArrayList;
import java.util.function.Consumer;

import com.ddv.test.SQLInsertBuilder;
import com.github.javaparser.ast.expr.Name;

public class PackageMetadata implements IMetadata {

	private int id;
	private String name;
	private PackageMetadata parent;
	private ArrayList<PackageMetadata> children;
	
	public PackageMetadata(int anId, String aName, PackageMetadata aParent) {
		id = anId;
		name = aName;
		parent = aParent;
		children = new ArrayList<PackageMetadata>();
		
		if (parent!=null) {
			parent.addChild(this);
		}
	}
	
	public String getName() {
		return name;
	}
	
	public void addChild(PackageMetadata aPackage) {
		children.add(aPackage);
	}
	
	public ArrayList<PackageMetadata> getChildren() {
		return children;
	}
	
	public String getPackageFullName() {
		return ((parent!=null) ? (parent.getPackageFullName() + ".") : "") + name;
	}
	
	public String getTopParentPackage() {
		return (parent!=null) ? parent.getTopParentPackage() : name;
	}
	
	public boolean isJavaPackage() {
		return getTopParentPackage().startsWith("java");
	}
	
	public ArrayList<PackageMetadata> getAllPackagesToRoot() {
		ArrayList<PackageMetadata> rslt = new ArrayList<PackageMetadata>();
		getAllPackagesToRootHelper(rslt);
		return rslt;
	}
	
	private void getAllPackagesToRootHelper(ArrayList<PackageMetadata> aRslt) {
		aRslt.add(0, this);
		if (parent!=null) {
			parent.getAllPackagesToRootHelper(aRslt);
		}
	}
	
	public void walkTree(Consumer<PackageMetadata> aConsumer) {
		aConsumer.accept(this);
		for (PackageMetadata child : children) {
			child.walkTree(aConsumer);
		}
	}
	
	public void findLongestMatch(ArrayList<String> aPackageNameParts, ArrayList<PackageMetadata> aRslt) {
		if (!aPackageNameParts.isEmpty()) {
			String name = aPackageNameParts.get(0);
			if (this.name.equals(name)) {
				aRslt.add(this);
				aPackageNameParts.remove(0);
				for (PackageMetadata child : children) {
					child.findLongestMatch(aPackageNameParts, aRslt);
				}
			}
		}
	}
	
	public Name toName() {
		if (parent!=null) {
			Name rslt = parent.toName();
			return new Name(rslt, name);
		}
		return new Name(name);
	}
	
	@Override
	public String toString() {
		return "Package " + getPackageFullName();
	}

	//**** IMetadata implementation ********************************************
	
	@Override
	public int getId() {
		return id;
	}

	@Override
	public String generateSQLInsert() {
		StringBuilder rslt = new StringBuilder();
		new SQLInsertBuilder(rslt, "PACKAGE", "ID", "NAME", "FULL_NAME")
		.addNumber(id)
		.addString(name)
		.addString(getPackageFullName())
		.flush();
		return rslt.toString();
	}
	
}
