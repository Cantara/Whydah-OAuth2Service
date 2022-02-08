package net.whydah.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtil {
	
	public static List<String> findFiles(Path path, String fileExtension)
	        throws IOException {

	        if (!Files.isDirectory(path)) {
	            throw new IllegalArgumentException("Path must be a directory!");
	        }

	        List<String> result;

	        try (Stream<Path> walk = Files.walk(path)) {
	            result = walk
	                    .filter(p -> !Files.isDirectory(p))
	                    // this is a path, not string,
	                    // this only test if path end with a certain path
	                    //.filter(p -> p.endsWith(fileExtension))
	                    // convert path to string first
	                    .map(p -> p.toString().toLowerCase())
	                    .filter(f -> f.endsWith(fileExtension))
	                    .collect(Collectors.toList());
	        }

	        return result;
	    }
	
	public static String getBaseName(String fileName) {
	    int index = fileName.lastIndexOf('.');
	    if (index == -1) {
	        return fileName;
	    } else {
	        return fileName.substring(0, index);
	    }
	}


}
