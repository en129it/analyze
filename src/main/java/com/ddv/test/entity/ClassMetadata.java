package com.ddv.test.entity;

import java.util.ArrayList;

import com.ddv.test.SQLInsertBuilder;
import com.ddv.test.Tapestry;
import com.ddv.test.Utils;
import com.ddv.test.exception.UnknownObjectException;
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
	private boolean isEnum;
	private ClassMetadata outerClass;
	private ArrayList<MethodMetadata> methods;
	private ArrayList<ConstructorMetadata> constructors;
	private ArrayList<ClassMetadata> implementedInterfaces;
	private ArrayList<String> genericTypes;
	private boolean isEntityBean;
	private Integer referencedComponentCount;
	
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
	
	public void setIsEnum(boolean aFlag) {
		isEnum = aFlag;
	}
	public boolean isEnum(boolean aFlag) {
		return isEnum;
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
	
	public int getDeclaredComponentCount() {
		int rslt = 0;
		for (MethodMetadata method : methods) {
			if (method.getComponentMetadata()!=null) {
				rslt++;
			}
		}
		return rslt;
	}
	
	public int getComponentCount() {
		int rslt = getDeclaredComponentCount();
		if (extendedClass!=null) {
			rslt += extendedClass.getComponentCount();
		}
		return rslt;
	}
	
	public String getClassFullName() {
		return ((packageMetaData!=null) ? (packageMetaData.getPackageFullName() + ".") : "") + name;
	}
	
	public void postProcessComponents(Tapestry aTapestry, MetadataFactory aMetadataFactory) {
		for (MethodMetadata methodMetadata : methods) {
			methodMetadata.postProcessComponents(this, aTapestry, aMetadataFactory);
		}
	}
	
	public String resolveComponentCopyOf(String aMethodName) {
		String methodName = Utils.createGetterMethod(aMethodName);
		MethodMetadata methodMetadata = findAbstractMethodByName(methodName);
		if (methodMetadata!=null) {
			return methodMetadata.resolveComponentName(this);
		} else {
			throw new UnknownObjectException("Unable to find abstract method '" + methodName + "' on class '" + getClassFullName() + "'");
		}
	}
	
	public MethodMetadata findAbstractMethodByName(String aMethodName) {
		for (MethodMetadata methodMetadata : methods) {
			if ((methodMetadata.isAbstract()) && (aMethodName.equals(methodMetadata.getMethodName()))) {
				return methodMetadata;
			}
		}
		return null;
	}
	
	public int resolveReferencedComponentCount() {
		if (referencedComponentCount==null) {
			int rslt = 0;
			for (MethodMetadata methodMetadata : methods) {
				rslt += methodMetadata.getComponentMetadata().resolveReferencedComponentCount();
			}
			if (extendedClass!=null) {
				rslt =+ extendedClass.resolveReferencedComponentCount();
			}
			referencedComponentCount = rslt;
		}
		return referencedComponentCount.intValue();
	}
	
	@Override
	public String toString() {
		String rslt = ((isEnum) ? "Enum " : "Class ") + getClassFullName(); 
	
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
		
		new SQLInsertBuilder(rslt, "CLASS", "ID", "NAME", "FULL_PCK_NAME", "DECLARED_INSTRUCTION_COUNT", "INSTRUCTION_COUNT", "DECLARED_COMPONENT_COUNT", "COMPONENT_COUNT", "IS_STATIC", "IS_ABSTRACT", "IS_ENUM")
			.addNumber(id)
			.addString(name)
			.addString(packageMetaData.getPackageFullName())
			.addNumber(getDeclaredInstructionCount())
			.addNumber(getInstructionCount())
			.addNumber(getDeclaredComponentCount())
			.addNumber(getComponentCount())
			.addBoolean(isStatic)
			.addBoolean(isAbstract)
			.addBoolean(isEnum)
			.flush();
		
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

		for (ConstructorMetadata constructor: constructors) {
			new SQLInsertBuilder(rslt, "CONSTRUCTOR", "ID", "CLASS_ID", "SIGNATURE", "DECLARED_INSTRUCTION_COUNT")
			.addNumber(constructor.getId())
			.addNumber(id)
			.addString("")
			.addNumber(constructor.getDeclaredInstructionCount())
			.flush();
		}

		for (MethodMetadata method: methods) {
			ComponentMetadata componentMetadata = method.getComponentMetadata();
			SQLInsertBuilder builder = null;
			if (componentMetadata!=null) {
				builder = new SQLInsertBuilder(rslt, "METHOD", "ID", "CLASS_ID", "SIGNATURE", "DECLARED_INSTRUCTION_COUNT", "COMPONENT_ID");
			} else {
				builder = new SQLInsertBuilder(rslt, "METHOD", "ID", "CLASS_ID", "SIGNATURE", "DECLARED_INSTRUCTION_COUNT");
			}
			builder
			.addNumber(method.getId())
			.addNumber(id)
			.addString(method.getMethodName())
			.addNumber(method.getDeclaredInstructionCount());
			if (componentMetadata!=null) {
				builder.addNumber(componentMetadata.getId());
			}
			builder.flush();
		}
		
		return rslt.toString();
	}
}
