package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;


public class MappingContributions {

	//the mapping between the classes and developers' contributions
	private Map<String, Map<String, Set<Integer>>> mapClassContributions;

	
	public MappingContributions() {
		this.mapClassContributions = new HashMap<>();
	}


	/**
	 * Maps developers' contributions lines
	 * @param sourceDirectory
	 */
	public void executeMapping(String sourceDirectory){
		try{
			File[] files = new File(sourceDirectory).listFiles();
			List<String> srcFiles = new ArrayList<String>();
			this.getClassDirectories(srcFiles, files);

			BufferedReader bufferedReader = null;

			for (String srcFile : srcFiles) {
				String className = this.prepareClassName(srcFile);

				//the mapping between developers's contributions and its lines of code
				Map<String, Set<Integer>> mapContributionsLines = new HashMap<String, Set<Integer>>();
				Set<Integer> lineNumbersFromLeft = new HashSet<Integer>();
				Set<Integer> lineNumbersFromRight = new HashSet<Integer>();

				bufferedReader = new BufferedReader(new FileReader(srcFile));
				String line = "";
				int lineNumber = 0;

				while ((line = bufferedReader.readLine()) != null) {
					lineNumber++;
					if(line.contains("CONTRIB::LEFT::")){
						lineNumbersFromLeft.add(lineNumber);

					} else if(line.contains("CONTRIB::RIGHT::")){
						lineNumbersFromRight.add(lineNumber);
					}	
				}

				//removing contribution tag
				this.removeTags(srcFile);

				if(!lineNumbersFromLeft.isEmpty() || !lineNumbersFromRight.isEmpty() ){
					mapContributionsLines.put("LEFT", lineNumbersFromLeft);
					mapContributionsLines.put("RIGHT", lineNumbersFromRight);
					mapClassContributions.put(className, mapContributionsLines);
				}
			}

			System.out.println(mapClassContributions);
		}catch(Exception e){
			e.printStackTrace();
		}
	}


	private void getClassDirectories(List<String> srcFiles, File[] files){
		try{
			for (File file : files) {
				if (file.isDirectory()) {
					getClassDirectories(srcFiles,file.listFiles());
				} else {
					if (file.getName().endsWith(".java")) {
						srcFiles.add(file.getCanonicalPath());
					}
				}
			}
		}catch(Exception e){}
	}

	private String prepareClassName(String srcFile) {
		String pattern = Pattern.quote(System.getProperty("file.separator"));
		String[] splittedFileName = srcFile.split(pattern);
		String className = "";
		int srcPosition = -1;
		for (int i = 0; i < (splittedFileName.length - 1); i++) {
			if (splittedFileName[i].contains("rev_")) { //src
				srcPosition = i;
			}
			if (srcPosition != -1) {
				if (className.isEmpty()) {
					className = splittedFileName[i + 1];
				} else {
					className = className + "." + splittedFileName[i + 1];
				}
			}
		}
		if (className.endsWith(".java")) {
			className = className.substring(0, className.length() - 5);
		}
		return className;
	}




	public void removeTags(String directory) {
		try{
			Path path = Paths.get(directory);
			String content = new String(Files.readAllBytes(path), Charset.defaultCharset());
			content = content.replaceAll("CONTRIB::RIGHT::", "");
			content = content.replaceAll("CONTRIB::LEFT::", "");
			Files.write(path, content.getBytes(Charset.defaultCharset()));
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new MappingContributions().executeMapping("C:\\GGTS\\ggts-bundle\\workspace\\others\\git clones\\mockito\\revisions\\rev_2b80e_f97f8\\rev_2b80e-f97f8");
	}
}

