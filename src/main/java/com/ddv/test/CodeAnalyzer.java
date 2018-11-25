package com.ddv.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.ddv.test.entity.ClassMetadata;
import com.ddv.test.entity.MetadataFactory;
import com.ddv.test.entity.PackageMetadata;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Name;

public class CodeAnalyzer {

	private void analyzeFile(Path aFilePath, MetadataFactory aFactory) throws Exception {
        try (FileInputStream in = new FileInputStream(aFilePath.toFile())) {
            CompilationUnit cu = JavaParser.parse(in);

            // Package
            PackageMetadata packageMetadata = null;
            Optional<PackageDeclaration> packageDeclaration = cu.getPackageDeclaration();
            if (packageDeclaration.isPresent()) {
                Name packageName = packageDeclaration.get().getName();
                packageMetadata = aFactory.createPackage(packageName);
            }

            // Class + Methods
            // The only way to access the main class and all inner classes it through visitor
            cu.accept(new ClassVisitor(packageMetadata, aFactory), null);            
        }
	}
	
	private ArrayList<Path> findJavaFiles(String[] aSourceFolders) throws Exception {
		ArrayList<Path> rslt = new ArrayList<Path>();
		
		for (String sourceFolder : aSourceFolders) {
			Path sourcePath = Paths.get(sourceFolder);
			rslt.addAll(Files.walk(sourcePath, FileVisitOption.FOLLOW_LINKS)
				.filter( (Path aPath) -> {
					return Files.isRegularFile(aPath)
						&& aPath.toFile().getName().endsWith(".java");
				}).collect(Collectors.toList())
			);
		}
		
		return rslt;
	}
	
	public static void main(String[] args) throws Exception {
		CodeAnalyzer t = new CodeAnalyzer();
		ArrayList<Path> javaFiles = t.findJavaFiles(new String[]{"C:/Users/ddev/workspacemars2/CodeAnalyzer/src/main/java/com/ddv/test"});
		System.out.println(">>>> " + javaFiles.size());
		MetadataFactory factory = new MetadataFactory(javaFiles);
		for (Path javaFile : javaFiles) {
			t.analyzeFile(javaFile, factory);
		}
		factory.printOutClasses();
	}
	
	public static class MyInner implements Serializable {
		public void sayHello() {
			System.out.println("Hello");
		}
	}
}
