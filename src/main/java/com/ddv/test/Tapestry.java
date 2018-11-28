package com.ddv.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.ddv.test.entity.ClassMetadata;
import com.ddv.test.entity.ComponentMetadata;
import com.ddv.test.entity.MetadataFactory;
import com.ddv.test.exception.UnknownObjectException;
import com.github.javaparser.ast.expr.Name;

public class Tapestry {

	private static final ComponentMetadata DEFAULT_TAPESTRY_COMPONENT = new ComponentMetadata(-1, null);
	private static final String DEFAULT_TAPESTRY_CLASS_NAME = "DEFAULT_TAPESTRY_CLASS_NAME";
	
	private DocumentBuilderFactory factory;
	private HashMap<String, HashMap<String, String>> prefixToComponentNameToQualifiedClassNameMap = new HashMap<String, HashMap<String, String>>();
	private HashMap<String ,ComponentMetadata> qualifiedClassNameToComponentMetadataMap = new HashMap<String ,ComponentMetadata>(); 
	
	public Tapestry() throws Exception {
		factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(false);
		factory.setValidating(false);
	}

	public void addContribLibrary(String aPrefix, String aFileFullPath, ArrayList<Path> aJwcFiles) throws Exception {
		Path contribLibContainerFolder = Paths.get(aFileFullPath).getParent();
		
		HashMap<String, String> componentNameToQualifiedClassNameMap = new HashMap<String, String>();
		prefixToComponentNameToQualifiedClassNameMap.put(aPrefix, componentNameToQualifiedClassNameMap);

		DocumentBuilder builder = createDocumentBuilder();
		try (FileInputStream in = new FileInputStream(aFileFullPath)) {
			Document document = builder.parse(in);
			Element librarySpecificationElem = document.getDocumentElement();
			
			NodeList componentTypeElems = librarySpecificationElem.getElementsByTagName("component-type");
			int size = componentTypeElems.getLength();
			for (int i=0; i<size; i++) {
				Element componentTypeElem = (Element)componentTypeElems.item(i);
				
				String type = componentTypeElem.getAttribute("type");
				String specificationPath = componentTypeElem.getAttribute("specification-path");
				Path filePath = contribLibContainerFolder.resolve(specificationPath);
				
				aJwcFiles.remove(filePath);
				
				try {
					String componentQualifiedClassName = parseJwcFile(filePath);
					componentNameToQualifiedClassNameMap.put(type, componentQualifiedClassName);
				} catch (FileNotFoundException ex) {
					String fileName = filePath.toFile().getAbsolutePath();
					if (fileName.contains("org\\apache\\tapestry") || fileName.contains("org/apache/tapestry")) {
						componentNameToQualifiedClassNameMap.put(type, DEFAULT_TAPESTRY_CLASS_NAME);
					} else {
						System.err.println("Failed to find JWC file associated with component '" + aPrefix + ":" + type +"'");
					}
				}
			}
		}
		
		// Process the remaining JWC files that were not referenced in the aFileFullPath file
		for (Path jwcFile : aJwcFiles) {
			String fileName = jwcFile.getFileName().toString();
			String componentName = fileName.substring(0,  fileName.indexOf('.'));
			
			String componentQualifiedClassName = parseJwcFile(jwcFile);
			componentNameToQualifiedClassNameMap.put(componentName, componentQualifiedClassName);
		}
	}
	
	public void postProcess(MetadataFactory aMetadataFactory) {
		for (Map.Entry<String, HashMap<String, String>> rootEntry : prefixToComponentNameToQualifiedClassNameMap.entrySet()) {
			for (Map.Entry<String, String> entry : rootEntry.getValue().entrySet()) {
				String qualifiedClassName = entry.getValue();
				ComponentMetadata metadata = aMetadataFactory.createComponent(qualifiedClassName);
				qualifiedClassNameToComponentMetadataMap.put(qualifiedClassName, metadata);
			}
		}
	}
	
	private DocumentBuilder createDocumentBuilder() throws Exception {
		DocumentBuilder builder = factory.newDocumentBuilder();
		builder.setEntityResolver(new EntityResolver() {
			@Override
			public InputSource resolveEntity(String aPublicId, String aSystemId) throws SAXException, IOException {
				if (aSystemId.contains("Tapestry_4_0.dtd")) {
					return new InputSource(CodeAnalyzer.class.getResourceAsStream("Tapestry_4_0.dtd"));
				} else {
					return null;
				}
			}
		});
		return builder;
	}
	
	private String parseJwcFile(Path aJwcFile) throws Exception {
		DocumentBuilder builder = createDocumentBuilder();
		try (FileInputStream in = new FileInputStream(aJwcFile.toFile())) {
			Document document = builder.parse(in);
			Element componentSpecificationElem = document.getDocumentElement();
			return componentSpecificationElem.getAttribute("class");
		}
	}

	private String findComponentPrefix(String aQualifiedClassName) {
		for (Map.Entry<String, HashMap<String, String>> rootEntry : prefixToComponentNameToQualifiedClassNameMap.entrySet()) {
			if (rootEntry.getValue().values().contains(aQualifiedClassName)) {
				return rootEntry.getKey();
			}
		}
		return null;
	}
	
	public ComponentMetadata resolveComponentName(ClassMetadata aContext, String aComponentName) {
		if (aComponentName!=null) {
			String[] parts = aComponentName.split(":");
			String prefix = null; String componentName = null;
			if (parts.length==2) {
				// Case : component name has prefix
				prefix = parts[0];
				componentName = parts[1];
			} else {
				// Case : component name has no prefix
				prefix = findComponentPrefix(aContext.getClassFullName());
				componentName = aComponentName;
			}
			
			if (prefix==null) {
				// Case : must be a Tapestry component
				return DEFAULT_TAPESTRY_COMPONENT;
			} else {
				// Case : custom Tapestry component
				HashMap<String, String> componentNameToQualifiedClassNameMap = prefixToComponentNameToQualifiedClassNameMap.get(prefix);
				if (componentNameToQualifiedClassNameMap!=null) {
					String qualifiedClassName = componentNameToQualifiedClassNameMap.get(componentName);
					if (qualifiedClassName!=null) {
						ComponentMetadata rslt = qualifiedClassNameToComponentMetadataMap.get(qualifiedClassName);
						if (rslt!=null) {
							return rslt;
						}
					}
					throw new UnknownObjectException("Class '" + aContext.getClassFullName() + "' references an unknown Tapestry library component (prefix=" + prefix + ", name=" + componentName + ")");
				} else {
					throw new UnknownObjectException("Tapestry library prefix '" + prefix + "' used in class '" + aContext.getClassFullName() + "' is unknown");
				}
			}
		} else {
			return null;
		}
	}
	
	public String generateSQLInsert() {
		StringBuilder builder = new StringBuilder();
		for (ComponentMetadata componentMetadata : qualifiedClassNameToComponentMetadataMap.values()) {
			builder.append(componentMetadata.generateSQLInsert());
		}
		return builder.toString();
	}
	
	private Name splitSpecificationPath(String aPath) {
		Name rslt = null;
		
		Pattern p = Pattern.compile("(\\w+)*/*(\\w+).jwc");
		Matcher m = p.matcher(aPath);
		if (m.find()) {
			for (int i = 1; i <= m.groupCount(); i++) {
				if (rslt==null) {
					rslt = new Name(m.group(i));
				} else {
					rslt = new Name(rslt, m.group(i));
				}
			}
		}
		return rslt;
	}
	
}
