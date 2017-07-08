package cin.ufpe.br.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.merge.MergeAlgorithm;
import org.eclipse.jgit.merge.MergeFormatter;
import org.eclipse.jgit.merge.MergeResult;

public final class FileHandlerr {

	//read into a sigle string representation
	public static String readFile(File file){
		String content = "";
		try{
			BufferedReader reader = Files.newBufferedReader(Paths.get(file.getAbsolutePath()), StandardCharsets.ISO_8859_1);
			content = reader.lines().collect(Collectors.joining("\n"));
			reader.close();
		}catch(Exception e){}
		return content;
	}

	//read into lines
	public static ArrayList<String> readFile(String filename) throws IOException{
		ArrayList<String> fileText = new ArrayList<String>();
		// Open the file
		FileInputStream fstream = new FileInputStream(filename);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

		String strLine;
		int index = 0;
		//Read File Line By Line
		while ((strLine = br.readLine()) != null)   {
			// Print the content on the console
			fileText.add(index,strLine);
			index++;
		}

		//Close the input stream
		br.close();
		return fileText;
	}

	public static List<String> findMethodSignatures(String sourceCode){
		String methodSignatureRegex = "((public|private|protected|static|final|native|synchronized|abstract|transient)+\\s)+[\\$_\\w\\<\\>\\w\\s\\[\\]]*\\s+[\\$_\\w]+\\([^\\)]*\\)?\\s*";
		Pattern p = Pattern.compile(methodSignatureRegex); 
		Matcher m = p.matcher(sourceCode);
		List<String> matches = new ArrayList<String>();
		while(m.find()){
			matches.add(m.group());
		}
		return matches;
	}

	public static String getStringContentIntoSingleLineNoSpacing(String content) {
		return (content.replaceAll("\\r\\n|\\r|\\n","")).replaceAll("\\s+","");
	}

	public static double computeStringSimilarity(String first,String second) {
		@SuppressWarnings("unused")
		String longer = first, shorter = second;
		if (first.length() < second.length()) { // longer should always have greater length
			longer = second; 
			shorter= first;
		}
		int longerLength = longer.length();
		if (longerLength == 0) {
			return 1.0; /* both strings are zero length */ 
		}

		int levenshteinDistance = StringUtils.getLevenshteinDistance(first, second);
		return ((longerLength - levenshteinDistance)/(double) longerLength);
	}

	public static List<String> listAllFilesPath(String directory){
		List<String> allFiles = new ArrayList<String>();
		File[] fList = (new File(directory)).listFiles();
		if(fList != null){
			for (File file : fList){
				if (file.isFile()){
					allFiles.add(file.getAbsolutePath());
				} else if (file.isDirectory()){
					allFiles.addAll(listAllFilesPath(file.getAbsolutePath()));
				}
			}
		}
		return allFiles;
	}

	public static String merge(String left, String base, String right){
		String textualMergeResult = null;
		try{
			RawTextComparator textComparator = RawTextComparator.DEFAULT;
			@SuppressWarnings("rawtypes") MergeResult mergeCommand = new MergeAlgorithm().merge(textComparator,
					new RawText(Constants.encode(base)), 
					new RawText(Constants.encode(left)), 
					new RawText(Constants.encode(right))
					);		
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			(new MergeFormatter()).formatMerge(output, mergeCommand, "BASE", "MINE", "YOURS", Constants.CHARACTER_ENCODING);
			textualMergeResult = new String(output.toByteArray(), Constants.CHARACTER_ENCODING);
		}catch(Exception e){
			textualMergeResult= "";
		}
		return textualMergeResult;
	}

	public static void main(String[] args) {
		try{
			ArrayList<String> lines = readFile("C:\\Users\\Guilherme\\.jfstmerge\\evaljfstmerge\\handlersoff\\jfstmerge.statistics");
			for(String l : lines){
				if(l.contains("java_lucenesolr\\rev_2ede7_249fd")){
					System.out.println(l);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
