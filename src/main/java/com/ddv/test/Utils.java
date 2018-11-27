package com.ddv.test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;

import com.ddv.test.exception.UnknownObjectException;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.VoidType;

public class Utils {

	public static Name convertToName(String aQualifiedName) {
		return JavaParser.parseName(aQualifiedName);
	}
	
	public static String getClassSimpleName(Type aType) {
		if (aType instanceof PrimitiveType) {
			return ((PrimitiveType)aType).getType().name();
		} else if (aType instanceof ArrayType) {
			return getClassSimpleName( ((ArrayType)aType).getComponentType() ) +"[]";
		} else if (aType instanceof ClassOrInterfaceType) {
			return ((ClassOrInterfaceType)aType).getNameAsString();
		} else if (aType instanceof TypeParameter) {
			return "";
		} else if (aType instanceof VoidType) {
			return "";
		} else {
			throw new UnknownObjectException("Unable to handle a type of type '" + aType.getClass().getSimpleName() + "'");
		}
	}
	
	public static Name convertFullScope(ClassOrInterfaceType aType) {
		Name scopeRslt = null;
		if (aType.getScope().isPresent()) {
			scopeRslt = convertFullScope(aType.getScope().get());
		}
		return (scopeRslt==null) ? new Name(aType.getNameAsString()) : new Name(scopeRslt, aType.getNameAsString());
	}
	
	public static String createGetterMethod(String aPropertyName) {
		return "get" + ((aPropertyName.length()>1)
			? (Character.toUpperCase(aPropertyName.charAt(0)) + aPropertyName.substring(1))
			: ("get" + aPropertyName.toUpperCase()));
	}
}
