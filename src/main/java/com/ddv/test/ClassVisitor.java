package com.ddv.test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Optional;

import com.ddv.test.entity.ClassMetadata;
import com.ddv.test.entity.ConstructorMetadata;
import com.ddv.test.entity.MetadataFactory;
import com.ddv.test.entity.MethodMetadata;
import com.ddv.test.entity.PackageMetadata;
import com.ddv.test.exception.ParsingException;
import com.ddv.test.exception.UnknownObjectException;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithBody;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.common.base.Objects;

public class ClassVisitor extends VoidVisitorAdapter<Void> {

	public static final String TAPESTRY_COMPONENT_ANNOTATION = "Component";
	
	private ArrayList<ImportDeclaration> imports = new ArrayList<ImportDeclaration>();
	private ClassMetadata outerClass = null;
	private PackageMetadata packageMetadata;
	private MetadataFactory factory;
	private ClassMetadata currentParsedClass = null;
	
	public ClassVisitor(PackageMetadata aPackage, MetadataFactory aFactory) {
		packageMetadata = aPackage;
		factory = aFactory;
	}

	private Name getOuterClassName(Name aClassName) {
		Name candidate = aClassName.getQualifier().orElse(null);
		if (candidate!=null) {
			char firstChar = candidate.getIdentifier().charAt(0);
			if (Character.toUpperCase(firstChar) == firstChar) {
				// Case : Java class
				return getOuterClassName(candidate);
			} else {
				// Case : package name
				return aClassName;
			}
		} else {
			return aClassName;
		}
	}
	
	private Name resolveClassName(Name aClassName) {
		Name outerClassName = getOuterClassName(aClassName);
		
		Optional<Name> qualifier = outerClassName.getQualifier();
		if (qualifier.isPresent()) {
			return (factory.existsJavaFile(outerClassName)) ? aClassName : null;
		} else {
			ArrayList<Name> candidates = factory.findJavaFiles(outerClassName.getIdentifier());
			Optional<Name> rslt = candidates.stream().filter( (Name candidate) -> {
				return isJavaFileInImports(candidate, imports, packageMetadata.toName());
			}).findFirst();
			
			Name adjRslt = rslt.orElse(null);
			
			if ((adjRslt!=null) && (!outerClassName.equals(aClassName))) {
				ArrayList<String> path = new ArrayList<String>(); 
				Name ref = aClassName;
				do {
					path.add(0, ref.getIdentifier());
					ref = ref.getQualifier().get();
				} while (!ref.equals(outerClassName));

				if (!path.isEmpty()) {
					for (String p : path) {
						adjRslt = new Name(adjRslt, p);
					}
				}
			}
			
			return adjRslt;
		}
	}
	
	private boolean isJavaFileInImports(Name aJavaFile, ArrayList<ImportDeclaration> anImports, Name aPackageAsName) {
		Name javaFilePackage = aJavaFile.getQualifier().orElse(null);
		
		if (aPackageAsName!=null) {
			if (Objects.equal(javaFilePackage, aPackageAsName)) {
				return true;
			}
		}
		
		for (ImportDeclaration importDecl : anImports) {
			if (importDecl.isAsterisk()) {
				if (Objects.equal(javaFilePackage, importDecl.getName())) {
					return true;
				}
			} else {
				if (Objects.equal(aJavaFile, importDecl.getName())) {
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public void visit(ImportDeclaration n, Void arg) {
		imports.add(n);
		super.visit(n, arg);
	}
	
	@Override
	public void visit(EnumDeclaration n, Void arg) {
		ClassMetadata classMetadata = null;
		if (n.isTopLevelType()) {
			classMetadata = factory.createClass(n.getNameAsString(), packageMetadata, null);
			if (outerClass==null) {
				outerClass = classMetadata;
			} else {
				throw new ParsingException("Error during parsing of class '" + n.getNameAsString() + "' : class is supposed to be the top level class in its file but class '" + outerClass.getClassFullName() + "' is already considered as the top level class");
			}
		} else {
			if (outerClass!=null) {
				classMetadata = factory.createClass(n.getNameAsString(), packageMetadata, outerClass);
			} else {
				throw new ParsingException("Error during parsing of class '" + n.getNameAsString() + "' : class is supposed to be an inner class but the outer class cannot be found");
			}
		}
		classMetadata.setIsEnum(true);
		classMetadata.setIsStatic(n.isStatic());
	}
	
	@Override
	public void visit(ClassOrInterfaceDeclaration n, Void arg) {
		// Create class itself
		ClassMetadata classMetadata = null;
		if (n.isTopLevelType()) {
			classMetadata = factory.createClass(n.getNameAsString(), packageMetadata, null);
			if (outerClass==null) {
				outerClass = classMetadata;
			} else {
				throw new ParsingException("Error during parsing of class '" + n.getNameAsString() + "' : class is supposed to be the top level class in its file but class '" + outerClass.getClassFullName() + "' is already considered as the top level class");
			}
		} else {
			if (outerClass!=null) {
				classMetadata = factory.createClass(n.getNameAsString(), packageMetadata, outerClass);
			} else {
				throw new ParsingException("Error during parsing of class '" + n.getNameAsString() + "' : class is supposed to be an inner class but the outer class cannot be found");
			}
		}
		classMetadata.setIsAbstract(n.isAbstract());
		classMetadata.setIsStatic(n.isStatic());
		
		NodeList<TypeParameter> genericTypes = n.getTypeParameters();
		for (int i=0; i<genericTypes.size(); i++) {
			classMetadata.addGenericType(genericTypes.get(i).getNameAsString());
		}
		currentParsedClass = classMetadata;
		
		// Process the class environment
		NodeList<ClassOrInterfaceType> interfaces = n.getImplementedTypes();
		for (int i=0; i<interfaces.size(); i++) {
			ClassOrInterfaceType interfaceType = interfaces.get(i);

			Name interfaceName = resolveClassName(Utils.convertFullScope(interfaceType));
			if (interfaceName != null) {
				classMetadata.addImplementedInterfaces(factory.createClass(interfaceName));
			}
		}
		
		NodeList<ClassOrInterfaceType> extendeds = n.getExtendedTypes();
		for (int i=0; i<extendeds.size(); i++) {
			ClassOrInterfaceType extended = extendeds.get(i);
			
			Name extendName = resolveClassName(Utils.convertFullScope(extended));
			if (extendName != null) {
				classMetadata.setExtendedClass(factory.createClass(extendName));
			}
		}
		
		super.visit(n, arg);
	}
	
	@Override
	public void visit(MethodDeclaration aMethodDeclaration, Void anArg) {
		ArrayList<String> genericTypeNames = new ArrayList<String>();
		NodeList<TypeParameter> genericTypes = aMethodDeclaration.getTypeParameters();
		for (int i=0; i<genericTypes.size(); i++) {
			genericTypeNames.add(genericTypes.get(i).getNameAsString());
		}
		
		NodeList<Parameter> args = aMethodDeclaration.getParameters();
		int argSize = args.size();
		ArrayList<String> argTypeNames = new ArrayList<String>(argSize); 
		for (int i=0; i<argSize; i++) {
			Parameter arg = args.get(i);
			argTypeNames.add(Utils.getClassSimpleName(arg.getType()));
		}		

		String returnTypeName = Utils.getClassSimpleName(aMethodDeclaration.getType());

		MethodMetrics metrics = new MethodMetrics();
		aMethodDeclaration.accept(new MethodVisitor(metrics), null);
		MethodMetadata methodMetadata = factory.createMethod(currentParsedClass, aMethodDeclaration.getNameAsString(), argTypeNames, returnTypeName, metrics);
		
		processComponentAnnotation(aMethodDeclaration, methodMetadata);
	}
	
	@Override
	public void visit(ConstructorDeclaration aConstructorDeclaration, Void anArg) {
		NodeList<Parameter> args = aConstructorDeclaration.getParameters();
		int argSize = args.size();
		ArrayList<String> argTypeNames = new ArrayList<String>(argSize); 
		for (int i=0; i<argSize; i++) {
			Parameter arg = args.get(i);
			argTypeNames.add(Utils.getClassSimpleName(arg.getType()));
		}
		
		MethodMetrics metrics = new MethodMetrics();
		aConstructorDeclaration.accept(new MethodVisitor(metrics), null);
		ConstructorMetadata constuctorMetadata = factory.createConstructor(currentParsedClass, argTypeNames, metrics);
	}
	
	private void processComponentAnnotation(MethodDeclaration aMethodDeclaration, MethodMetadata aMethodMetadata) {
		Optional<AnnotationExpr> componentAnnotation = aMethodDeclaration.getAnnotationByName(TAPESTRY_COMPONENT_ANNOTATION);
		if (componentAnnotation.isPresent()) {
			AnnotationExpr componentAnnotationExpr = componentAnnotation.get();
			
			String annotationValue = null;
			if (componentAnnotationExpr instanceof SingleMemberAnnotationExpr) {
				annotationValue = ((SingleMemberAnnotationExpr)componentAnnotationExpr).getMemberValue().toString();
			} else if (componentAnnotationExpr instanceof NormalAnnotationExpr) {
				NodeList<MemberValuePair> members = ((NormalAnnotationExpr)componentAnnotationExpr).getPairs();
				for (int i=0; i<members.size(); i++) {
					if ("type".equals(members.get(i).getNameAsString())) {
						annotationValue = members.get(i).getValue().toString();
						break;
					}
				}
			}
			
			if (annotationValue!=null) {
				if (annotationValue.startsWith("\"") && annotationValue.endsWith("\"")) {
					annotationValue = annotationValue.substring(1,  annotationValue.length()-1);
				}
				aMethodMetadata.setComponentAnnotationValue(annotationValue);
			} else {
				throw new UnknownObjectException("Unable to process @Component annotation on method '" + aMethodDeclaration.getNameAsString() + "' of class '" + currentParsedClass.getClassFullName() + "'");
			}
		}
		
	}
	
	private static class MethodStats {
		public int instructionCount = 0;
		public int branchCount = 0;
		
		public void incInstructionCount() {
			instructionCount++;
		}
	}
}
