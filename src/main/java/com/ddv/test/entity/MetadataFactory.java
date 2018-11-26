package com.ddv.test.entity;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;

import com.ddv.test.MethodMetrics;
import com.ddv.test.Tapestry;
import com.ddv.test.Utils;
import com.ddv.test.exception.UnknownObjectException;
import com.github.javaparser.ast.expr.Name;
import com.google.common.base.Objects;
import com.google.common.io.Files;

public class MetadataFactory {

	private ArrayList<Name> javaFiles = new ArrayList<Name>();
	private Tapestry tapestry;
	private ArrayList<ClassMetadata> classes = new ArrayList<ClassMetadata>();
	private ArrayList<PackageMetadata> packages = new ArrayList<PackageMetadata>();
	private int idGenerator;
	

	public MetadataFactory(ArrayList<Path> aJavaFiles, Tapestry aTapestry) {
		for (Path javaFile : aJavaFiles) {
			boolean isInPackage = false;
			boolean mustSkip = false;
			Name name = null;
			int count = javaFile.getNameCount();
			for (int i=0; i<count; i++) {
				String pathName = javaFile.getName(i).toString();
				if (isInPackage) {
					name = (name==null) ? new Name(pathName) : new Name(name, pathName);
				} else if ("test".equals(pathName)) {
					mustSkip = true;
					break;
				} else if ("java".equals(pathName)) {
					isInPackage = true;
				}
			}

			if (!mustSkip) {
				String identifier = name.getIdentifier();
				if (identifier.endsWith(".java")) {
					name.setIdentifier(identifier.substring(0, identifier.length()-5));
				}
				javaFiles.add(name);
			}
		}
		
		tapestry = aTapestry;
	}
	
	public void postProcess() {
		tapestry.postProcess(this);
		
		for (ClassMetadata classMetadata : classes) {
			classMetadata.postProcessComponents(tapestry,  this);
		}
		
		for (ClassMetadata classMetadata : classes) {
			classMetadata.resolveReferencedComponentCount();
		}
	}
	
	public boolean existsJavaFile(Name aQualifiedJavaClass) {
		return javaFiles.contains(aQualifiedJavaClass);
	}
	
	public ArrayList<Name> findJavaFiles(String aJavaClassName) {
		ArrayList<Name> rslt = new ArrayList<Name>(1);
		
		for (Name javaFile : javaFiles) {
			if (Objects.equal(aJavaClassName, javaFile.getIdentifier())) {
				rslt.add(javaFile);
			}
		}
		
		return rslt;
	}
	
	public ArrayList<Name> findJavaFilesEndingWith(Name aPartialQualifiedJavaClass) {
		ArrayList<String> reversePathParts = new ArrayList<String>();
		Name ref = aPartialQualifiedJavaClass;
		while (ref!=null) {
			reversePathParts.add(ref.getIdentifier());
			if (ref.getQualifier().isPresent()) {
				ref = ref.getQualifier().get();
			}
		}
		
		ArrayList<Name> rslt = new ArrayList<Name>(1);
		
		boolean match;
		for (Name javaFile : javaFiles) {
			ref = javaFile;
			match = true;
			for (String pathPart : reversePathParts) {
				if ((ref!=null) && (pathPart.equals(ref.getIdentifier()))) {
					if (ref.getQualifier().isPresent()) {
						ref = ref.getQualifier().get();
					}
				} else {
					match = false;
					break;
				}
			}
			
			if (match) {
				rslt.add(javaFile);
			}
		}
		
		return rslt;
	}
	
	public ClassMetadata createClass(Name aClassName) {
		PackageMetadata packageMetadata = null;
		Optional<Name> optionalQualifier = aClassName.getQualifier();
		if (optionalQualifier.isPresent()) {
			packageMetadata = createPackage(optionalQualifier.get());
		}
		return createClass(aClassName.getIdentifier(), packageMetadata, null);
	}
	
	public ClassMetadata createClass(String aClassName, PackageMetadata aPackage, ClassMetadata anOuterClass) {
		try {
			return getClass(aClassName, aPackage);
		} catch (UnknownObjectException ex) {
		}
		
		ClassMetadata rslt = (anOuterClass==null) ? new ClassMetadata(idGenerator++, aClassName, aPackage) : new ClassMetadata(idGenerator++, aClassName, aPackage, anOuterClass);
		classes.add(rslt);
//		System.out.println("#### Create class (name=" + rslt.getClassFullName() + ")");
		return rslt;
	}

	public ClassMetadata getClass(String aQualifiedName) {
		Name className = Utils.convertToName(aQualifiedName);
		
		PackageMetadata packageMetadata = null;
		Optional<Name> optionalQualifier = className.getQualifier();
		if (optionalQualifier.isPresent()) {
			packageMetadata = createPackage(optionalQualifier.get());
		}
		return getClass(className.getIdentifier(), packageMetadata);
	}
	
	public ClassMetadata getClass(String aClassName, PackageMetadata aPackage) {
		for (ClassMetadata classMetadata : classes) {
			if (classMetadata.equals(aClassName, aPackage)) {
				return classMetadata;
			}
		}
		throw new UnknownObjectException("Unable to find class '" + aPackage.getPackageFullName() + "." + aClassName + "'");
	}
	
	public PackageMetadata createPackage(Name aName) {
		ArrayList<String> packageNameParts = nameToArray(aName);

		if (!packageNameParts.isEmpty()) {
			ArrayList<PackageMetadata> candidate = null;
			for (PackageMetadata packageMetadata : packages) {
				ArrayList<PackageMetadata> rslt = new ArrayList<PackageMetadata>(); 
				packageMetadata.findLongestMatch((ArrayList<String>)packageNameParts.clone(), rslt);
				
				if ((candidate==null) || (candidate.size() < rslt.size())) {
					candidate = rslt;
				}
			}

			PackageMetadata ref = null; int startIndex = 0;
			if ((candidate==null) || candidate.isEmpty()) {
				ref = new PackageMetadata(idGenerator++, packageNameParts.get(0), null);
//				System.out.println("#### Create package (name=" + ref.getPackageFullName() + ")");
				startIndex = 1;
				packages.add(ref);
			} else {
				startIndex = candidate.size();
				ref = candidate.get(startIndex-1);
			}
			
			for (int i=startIndex; i<packageNameParts.size(); i++) {
				ref = new PackageMetadata(idGenerator++, packageNameParts.get(i), ref);
//				System.out.println("#### Create package (name=" + ref.getPackageFullName() + ")");
			}
			return ref;
		} else {
			return null;
		}
	}

	public MethodMetadata createMethod(ClassMetadata aClass, String aMethodName, ArrayList<String> anArgs, String aReturnType, MethodMetrics aMetrics) {
		MethodMetadata method = new MethodMetadata(idGenerator++, aMethodName, anArgs, aReturnType, aMetrics);
		aClass.addMethod(method);
		return method;
	}

	public ConstructorMetadata createConstructor(ClassMetadata aClass, ArrayList<String> anArgs, MethodMetrics aMetrics) {
		ConstructorMetadata constructor = new ConstructorMetadata(idGenerator++, anArgs, aMetrics);
		aClass.addConstructor(constructor);
		return constructor;
	}
	
	private ArrayList<String> nameToArray(Name aName) {
		ArrayList<String> rslt = new ArrayList<String>();
		nameToArrayHelper(aName, rslt);
		return rslt;
	}

	private void nameToArrayHelper(Name aName, ArrayList<String> aRslt) {
		aRslt.add(0, aName.getIdentifier());
		
		Optional<Name> optionalQualifier = aName.getQualifier();
		if (optionalQualifier.isPresent()) {
			nameToArrayHelper(optionalQualifier.get(), aRslt);
		}
	}

	public void generateSQL(String anOutFileName) throws IOException {
		try (BufferedWriter out = new BufferedWriter(new FileWriter(new File(anOutFileName)))) {
			for (PackageMetadata packageMetadata : packages) {
				packageMetadata.walkTree( (PackageMetadata metadata) -> {
					try { 
						out.write(metadata.generateSQLInsert());
					} catch (IOException ex) {
					}
				});
			}
			for (ClassMetadata classMetadata : classes) {
				out.write(classMetadata.generateSQLInsert());
			}
		}
	}
	
	public void printOutClasses() {
		for (ClassMetadata classMetadata : classes) {
			System.out.println(classMetadata.toString());
		}
	}
}
