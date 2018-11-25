package com.ddv.test;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Tapestry {

	private HashMap<String, ArrayList<String>> componentNameToTypeMap = new HashMap<String, ArrayList<String>>();
	
	public Tapestry() throws Exception {
	}

	public void addContribLibrary(String aPrefix, String aFileFullPath) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(false);
		factory.setValidating(false);
		
		DocumentBuilder builder = factory.newDocumentBuilder();
		FileInputStream in = new FileInputStream(aFileFullPath);
		Document document = builder.parse(in);
		Element librarySpecificationElem = document.getDocumentElement();
		
		NodeList componentTypeElems = librarySpecificationElem.getElementsByTagName("component-type");
		int size = componentTypeElems.getLength();
		for (int i=0; i<size; i++) {
			Element componentTypeElem = (Element)componentTypeElems.item(i);
			
			String specificationPath = componentTypeElem.getAttribute("specification-path");
			componentNameToTypeMap.put(componentTypeElem.getAttribute("type"), splitSpecificationPath(specificationPath));
		}
	}
	
	
	
	private ArrayList<String> splitSpecificationPath(String aPath) {
		ArrayList<String> rslt = new ArrayList<String>();
		
		Pattern p = Pattern.compile("(\\w+)*/*(\\w+).jwc");
		Matcher m = p.matcher(aPath);
		if (m.find()) {
			for (int i = 1; i <= m.groupCount(); i++) {
				rslt.add(m.group(i));
			}
		}
		return rslt;
	}
	
}
