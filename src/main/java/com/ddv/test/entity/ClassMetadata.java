package com.ddv.test.entity;

import java.util.ArrayList;

import com.ddv.test.SQLInsertBuilder;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration.Signature;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.google.common.base.Objects;

public class ClassMetadata implements IMetadata {

	private int id;
	private String name;
	private PackageMetadata packageMetaData;
	private ClassMetadata extendedClass;
	private boolean isAbstract;
	private boolean isStatic;
	private ClassMetadata outerClass;
	private ArrayList<MethodMetadata> methods;
	private ArrayList<ConstructorMetadata> constructors;
	private ArrayList<ClassMetadata> implementedInterfaces;
	private ArrayList<String> genericTypes;
	private boolean isEntityBean;
	
	public ClassMetadata(int anId, String aName, PackageMetadata aPackage) {
		id = anId;
		name = aName;
		packageMetaData = aPackage;
		
		methods = new ArrayList<MethodMetadata>();
		constructors = new ArrayList<ConstructorMetadata>();
		implementedInterfaces = new ArrayList<ClassMetadata>();
		genericTypes = new ArrayList<String>();
	}
	
	public ClassMetadata(int anId, String aName, PackageMetadata aPackage, ClassMetadata anOuterClass) {
		this(anId, aName, aPackage);
		outerClass = anOuterClass;
	}
	
	public String getName() {
		return name;
	}
	
	public PackageMetadata getPackage() {
		return packageMetaData;
	}
	
	public void addGenericType(String aType) {
		genericTypes.add(aType);
	}
	
	public void setIsEntityBean(boolean aFlag) {
		isEntityBean = aFlag;
	}
	
	public boolean equals(String aName, PackageMetadata aPackage) {
		return (Objects.equal(aName, name))
			&& (Objects.equal(aPackage, packageMetaData));
	}
	
	public void setExtendedClass(ClassMetadata aClass) {
		extendedClass = aClass;
	}
	public ClassMetadata getExtendedClass() {
		return extendedClass;
	}
	
	public boolean isStatic() {
		return isStatic;
	}
	public void setIsStatic(boolean aFlag) {
		isStatic = aFlag;
	}
	
	public void setIsAbstract(boolean aFlag) {
		isAbstract = aFlag;
	}
	public boolean isAbstract() {
		return isAbstract;
	}
	
	public void addImplementedInterfaces(ClassMetadata anInterface) {
		implementedInterfaces.add(anInterface);
	}
	
	public void addConstructor(ConstructorMetadata aConstructor) {
		constructors.add(aConstructor);
	}
	
	public void addMethod(MethodMetadata aMethod) {
		methods.add(aMethod);
	}
	
	public MethodMetadata findDeclaredMethod(MethodMetadata aMethodSignature) {
		for (MethodMetadata method : methods) {
			if (method.hasSameSignature(aMethodSignature)) {
				return method;
			}
		}
		return null;
	}
	
	public MethodMetadata findMethod(MethodMetadata aMethodSignature) {
		MethodMetadata rslt = findDeclaredMethod(aMethodSignature);
		if ((rslt==null) && (extendedClass!=null)) {
			extendedClass.findMethod(aMethodSignature);
		}
		return rslt;
	}
	
	public int getDeclaredInstructionCount() {
		int rslt = 0;
		for (MethodMetadata method : methods) {
			rslt += method.getDeclaredInstructionCount();
		}
		return rslt;
	}
	
	public int getInstructionCount() {
		int rslt = getDeclaredInstructionCount();
		if (extendedClass!=null) {
			rslt += extendedClass.getInstructionCount();
		}
		return rslt;
	}
	
	public String getClassFullName() {
		return ((packageMetaData!=null) ? (packageMetaData.getPackageFullName() + ".") : "") + name;
	}
	
	@Override
	public String toString() {
		String rslt =  "Class " + getClassFullName(); 
	
		if ((genericTypes!=null) && (!genericTypes.isEmpty())) {
			for (String genericType : genericTypes) {
				rslt += " [" + genericType + "]";
			}
		}		
		
		if (extendedClass!=null) {
			rslt += " extends " + extendedClass.getClassFullName();
		}
		
		if ((implementedInterfaces!=null) && (!implementedInterfaces.isEmpty())) {
			rslt += " implements";
			for (ClassMetadata interfaceMetadata : implementedInterfaces) {
				rslt += " " + interfaceMetadata;
			}
		}
		
		if (isEntityBean) {
			rslt += "[entity bean]";
		}
		
		for (ConstructorMetadata constructor : constructors) {
			rslt += ("\n   " + constructor.toString());
		}
		
		for (MethodMetadata method : methods) {
			rslt += ("\n   " + method.toString());
		}
		
		return rslt;
	}

	//**** IMetadata implementation ********************************************
	
	@Override
	public int getId() {
		return id;
	}

	@Override
	public String generateSQLInsert() {
		StringBuilder rslt = new StringBuilder();
		
		ArrayList<PackageMetadata> packages = packageMetaData.getAllPackagesToRoot();
		for (PackageMetadata packageMetadata : packages) {
			new SQLInsertBuilder(rslt, "CLASS_PACKAGE", "CLASS_ID", "PACKAGE_ID")
				.addNumber(id)
				.addNumber(packageMetadata.getId())
				.flush();
		}
		
		if (extendedClass!=null) {
			new SQLInsertBuilder(rslt, "EXTEND_CLASS", "SUB_CLASS_ID", "CLASS_ID")
				.addNumber(id)
				.addNumber(extendedClass.getId())
				.flush();
		}

		if ((implementedInterfaces!=null) && (!implementedInterfaces.isEmpty())) {
			for (ClassMetadata interfaceMetadata : implementedInterfaces) {
				new SQLInsertBuilder(rslt, "IMPLEMENT_INTERFACE", "CLASS_ID", "INTERFACE_ID")
					.addNumber(id)
					.addNumber(interfaceMetadata.getId())
					.flush();
			}
		}
		
		new SQLInsertBuilder(rslt, "CLASS", "ID", "NAME", "FULL_PCK_NAME", "DECLARED_INSTRUCTION_COUNT", "INSTRUCTION_COUNT")
			.addNumber(id)
			.addString(name)
			.addString(packageMetaData.getPackageFullName())
			.addNumber(getDeclaredInstructionCount())
			.addNumber(getInstructionCount())
			.flush();

		for (ConstructorMetadata constructor: constructors) {
			new SQLInsertBuilder(rslt, "CONSTRUCTOR", "ID", "CLASS_ID", "SIGNATURE", "DECLARED_INSTRUCTION_COUNT")
			.addNumber(constructor.getId())
			.addNumber(id)
			.addString("")
			.addNumber(constructor.getDeclaredInstructionCount())
			.flush();
		}

		for (MethodMetadata method: methods) {
			new SQLInsertBuilder(rslt, "METHOD", "ID", "CLASS_ID", "SIGNATURE", "DECLARED_INSTRUCTION_COUNT", "COMPONENT_ANNOTATION_VALUE")
			.addNumber(method.getId())
			.addNumber(id)
			.addString("")
			.addNumber(method.getDeclaredInstructionCount())
			.addString(method.getComponentAnnotationValue()!=null ? method.getComponentAnnotationValue() : "")
			.flush();
		}
		
		return rslt.toString();
	}
}
