package com;

import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;

public class FileManager {

	public void manageFiles(String aFolder) throws Exception {
		Path folderPath = Paths.get(aFolder);
		Files.walk(folderPath, FileVisitOption.FOLLOW_LINKS).forEach( (Path aPath) -> {
			try {
				manageFile(aPath, System.currentTimeMillis());
			} catch (Exception ex) {
				System.out.println("Failed to process " + folderPath.toString());
			}
		});		
	}
	
	public void manageFile(Path aFile, long aDateInMsec) throws Exception {
		BasicFileAttributeView fileAttrs = Files.getFileAttributeView(aFile, BasicFileAttributeView.class);
	    FileTime fileTime = FileTime.fromMillis(aDateInMsec);
	    fileAttrs.setTimes(fileTime, fileTime, fileTime);

	}
	
	public static void main(String[] args) throws Exception {
		FileManager t = new FileManager();
		t.manageFiles(args[0]);
	}

}
