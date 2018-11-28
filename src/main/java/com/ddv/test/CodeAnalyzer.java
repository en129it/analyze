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
	
	private void findJavaAndJwcFiles(String[] aSourceFolders, ArrayList<Path> aJavaFiles, ArrayList<Path> aJwcFiles) throws Exception {
		for (String sourceFolder : aSourceFolders) {
			Path sourcePath = Paths.get(sourceFolder);
			Files.walk(sourcePath, FileVisitOption.FOLLOW_LINKS).forEach( (Path aPath) -> {
				if (Files.isRegularFile(aPath) && (!isTestFile(aPath))) {
					String fileName = aPath.toFile().getName();
					if (fileName.endsWith(".java")) {
						aJavaFiles.add(aPath);
					} else if (fileName.endsWith(".jwc")) {
						aJwcFiles.add(aPath);
					}
				}
			});
		}
	}
	
	private static boolean isTestFile(Path aPath) {
		int count = aPath.getNameCount();
		for (int i=0; i<count; i++) {
			if (aPath.getName(i).toString().equals("test")) {
				return true;
			}
		}
		return false;
	}
	
	public static void main(String[] args) throws Exception {
		if (args.length<3) {
			System.out.println("Usage: CodeAnalyzer <contrib file> <out file> <out methods (Y,N)> [<folders to analyze>]");
		} else {
			String contribFile = args[0];
			String outFile = args[1];
			boolean mustOutputClassMethods = "Y".equals(args[2]);
			
			String[] foldersToAnalyze = new String[args.length-3];
			System.arraycopy(args, 3, foldersToAnalyze, 0, foldersToAnalyze.length);

			CodeAnalyzer t = new CodeAnalyzer();
			ArrayList<Path> javaFiles = new ArrayList<Path>();
			ArrayList<Path> jwcFiles = new ArrayList<Path>();
			t.findJavaAndJwcFiles(foldersToAnalyze, javaFiles, jwcFiles);
			
			System.out.println("Start parsing contrib library file...");
			Tapestry tapestry = new Tapestry();
			tapestry.addContribLibrary("RDIS", contribFile, jwcFiles);
			jwcFiles.clear();
			System.out.println("Contrib library file parsing done");
			
			int javaFileCount = javaFiles.size();
			System.out.println(String.format("Start analyzing %d Java files...", javaFileCount));
			MetadataFactory factory = new MetadataFactory(javaFiles, tapestry);
			
			int tickSize = Math.max(1, (javaFileCount * 10) / 100);
			int processedFileCount = 0;

			System.out.print("Progress : ");
			for (Path javaFile : javaFiles) {
				t.analyzeFile(javaFile, factory);
				processedFileCount++;
				if ((processedFileCount % tickSize)==0) {
					System.out.print(".");
				}
			}
			System.out.println();
			System.out.println("Analysis done. Start post processing...");
			factory.postProcess();
			System.out.println("Post processing done. Saving results to " + outFile);
			factory.generateSQL(outFile, mustOutputClassMethods);
			//factory.printOutClasses();
		}
	}
}
