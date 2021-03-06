package util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import merger.FSTGenMerger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Multimap;

import de.ovgu.cide.fstgen.ast.FSTNode;
import de.ovgu.cide.fstgen.ast.FSTTerminal;

public class Util {

	public static int JDIME_CONFS 	= 0;
	public static int JDIME_LOCS 	= 0;
	public static int JDIME_FILES 	= 0;

	public static String generateMethodStub(String methodSourceCode) {
		String methodSignature = getMethodSignature(methodSourceCode);
		String returnType = getMethodReturnType(methodSignature);
		String returnStmt = "";

		if (returnType.equalsIgnoreCase("void")) {
		} else {
			returnStmt = "return " + getPrimitiveTypeDefaultValue(returnType)
					+ ";";
		}
		String methodStub = methodSignature + "\n" + returnStmt + "\n}";
		return methodStub;
	}

	public static String generateMultipleMethodBody(String left, String base, String right) {
		String newBody = "";
		String tempVar = "var"+System.currentTimeMillis();
		newBody += getMethodSignature(base);
		newBody += "\n";
		newBody += "int "+ tempVar + " = 0;\nif("+tempVar+"=="+tempVar+"){\n";
		newBody += getMethodBody(left);
		newBody += "}else{\n";
		newBody += getMethodBody(right);
		newBody += "}\n}";
		return newBody;
	}

	public static void generateJarFile(String srcDirectory){
		try{
			Manifest manifest = new Manifest();
			manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION,"1.0");
			JarOutputStream target = new JarOutputStream(new FileOutputStream("output.jar"), manifest);
			addFilesToJar(new File(srcDirectory), target);
			target.close();
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	public static String simplifyMethodSignature(String signature){
		String simplifiedMethodSignature = "";
		String firstPart = "";
		String parameters= "";
		String lastPart  = "";
		String aux 		 = "";

		signature = signature.replaceAll("\\s+","");

		for (int i = 0, n = signature.length(); i < n; i++) {
			char chr = signature.charAt(i);
			if (chr == '('){
				aux = signature.substring(i+1,signature.length());
				firstPart+="(";
				break;
			}else
				firstPart += chr;
		}
		for (int i = 0, n = aux.length(); i < n; i++) {
			char chr = aux.charAt(i);
			if (chr == ')'){
				lastPart = aux.substring(i,aux.length());
				break;
			}else
				parameters += chr;
		}

		simplifiedMethodSignature = firstPart + normalizeParameters(parameters) + lastPart;
		simplifiedMethodSignature = simplifiedMethodSignature.replace("{FormalParametersInternal}", "");
		return simplifiedMethodSignature;
	}

	public static String getMethodBody(String methodSource){
		String methodBody = "";
		String aux 		 = "";
		for (int i = 0, n = methodSource.length(); i < n; i++) {
			char chr = methodSource.charAt(i);
			if (chr == '{'){
				aux = methodSource.substring(i+1,methodSource.length());
				break;
			}
		}
		int ind = aux.lastIndexOf("}");
		methodBody = new StringBuilder(aux).replace(ind, ind+1,"").toString();
		return methodBody;
	}

	public static boolean isFilesContentEqual(String filePathLeft, String filePathBase, String filePathRight ){
		String leftContent 	= getFileContents(filePathLeft);
		String baseContent 	= getFileContents(filePathBase);
		String rightContent = getFileContents(filePathRight);

		leftContent  = getSingleLineContentNoSpacing(leftContent);
		baseContent  = getSingleLineContentNoSpacing(baseContent); 
		rightContent = getSingleLineContentNoSpacing(rightContent); 

		return (leftContent.equals(baseContent) && rightContent.equals(baseContent));
	}

	public static boolean isFilesContentEqual(String firstFilePath, String secondFilePath){
		String firstContent  = getFileContents(firstFilePath);
		String secondContent = getFileContents(secondFilePath);

		firstContent  = getSingleLineContentNoSpacing(firstContent);
		secondContent = getSingleLineContentNoSpacing(secondContent);

		return (firstContent.equals(secondContent));
	}

	public static boolean isStringsContentEqual(String firstString, String secondString){
		firstString  = getSingleLineContentNoSpacing(firstString);
		secondString = getSingleLineContentNoSpacing(secondString);

		return (firstString.equals(secondString));
	}

	public static MergeResult countConflicts(String revision) {
		File revFile 			= new File(revision);
		MergeResult mergeResult = new MergeResult();
		if(revFile.exists()){
			String mergedFolder 	= revFile.getParentFile() + File.separator + (revFile.getName().split("\\.")[0]);
			File root 	 			= new File(mergedFolder);
			countConflicts(root,mergeResult);

			countEqualConflicts(root,mergeResult);

			printConflictsReport(revision,mergeResult);
		}
		return mergeResult;
	}

	public static List<MergeConflict> countConflicts(MergeResult mergeResult) {
		File revFile = new File(mergeResult.revision);
		List<MergeConflict> unmergediffconfs = new ArrayList<MergeConflict>();
		if(revFile.exists()){
			String mergedFolder = revFile.getParentFile() + File.separator + (revFile.getName().split("\\.")[0]);
			File root 	 		= new File(mergedFolder);
			countConflicts(root,mergeResult);

			unmergediffconfs = countEqualConflicts(root,mergeResult);

			printConflictsReport(mergeResult.revision,mergeResult);
		}
		return unmergediffconfs;
	}

	public static void countJdimeConflicts(MergeResult currentMergeResult) {
		try {
			File tempFile  = new File("results/temp_jdime_conflicts_report.csv");
			if(tempFile.exists() && (null!=currentMergeResult)){
				String line    = (FileUtils.readLines(tempFile)).get(0);
				String[] stats = line.split(";");
				currentMergeResult.jdimeConfs += Integer.valueOf(stats[0]);
				currentMergeResult.jdimeLOC   += Integer.valueOf(stats[1]);
				currentMergeResult.jdimeFiles += Integer.valueOf(stats[2]);		

				//FileUtils.forceDelete(tempFile);
				tempFile.setWritable(true);
				tempFile.delete();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static boolean contains(LinkedList<LinkedList<String>> list, LinkedList<String> newentry){
		for(LinkedList<String> entry : list){
			String file 		= entry.get(0);
			String newentryfile = newentry.get(0);
			String methodDeclaration 	= entry.get(1);
			String newmethodDeclaration = newentry.get(1);
			if(file.equals(newentryfile) && methodDeclaration.equals(newmethodDeclaration)){
				return true;
			} else {
				continue;
			}
		}
		return false;
	}

	public static ArrayList<MergeConflict> getConflicts(String fileName, FSTTerminal node){
		ArrayList<MergeConflict> mergeConflicts = new ArrayList<MergeConflict>();

		String CONFLICT_HEADER_BEGIN= "<<<<<<<";
		String CONFLICT_HEADER_END 	= ">>>>>>>";

		boolean isConflictOpen		= false;
		String nodeContent 	= node.getBody();
		Scanner scanner 	= new Scanner(nodeContent);

		String textualConflict = "";
		FFPNSpacingAndConsecutiveLinesFinder aux = new FFPNSpacingAndConsecutiveLinesFinder();

		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			//if(!line.contains("//") && !line.contains("/*") && line.contains(CONFLICT_HEADER_END)) {
			if(line.contains(CONFLICT_HEADER_END)) {
				try{

					textualConflict += "\n";

					String[] splittedConflictBody = aux.splitConflictBody(textualConflict);

					MergeConflict mergeConflict = new MergeConflict(fileName, splittedConflictBody[0], null,splittedConflictBody[2],textualConflict);
					mergeConflicts.add(mergeConflict);

					//reseting
					isConflictOpen	= false;
					textualConflict = "";

				}catch(Exception e){
					e.printStackTrace();
					break; //in case of having errors on the file, ignore the rest of the file
				}
			}
			if(isConflictOpen){
				textualConflict += line + "\n";
			}
			//if(!line.contains("//") && !line.contains("/*") && line.contains(CONFLICT_HEADER_BEGIN)){
			if(line.contains(CONFLICT_HEADER_BEGIN)){

				isConflictOpen = true;
				textualConflict += "\n";
			}
		}
		scanner.close();

		return mergeConflicts;
	}

	public static String getMethodSignature(String methodSourceCode) {
		String methodSignature = "";
		for (int i = 0, n = methodSourceCode.length(); i < n; i++) {
			char chr = methodSourceCode.charAt(i);
			methodSignature += chr;
			if (chr == '{')
				break;
		}
		return methodSignature;
	}

	public static String unMergeMethodSignature(String [] methodBodySplited) {
		String leftPart  = methodBodySplited[0];
		String rightPart = methodBodySplited[2];
		String body = ((leftPart.startsWith("<<<<<<<"))?rightPart:leftPart).replaceAll("\\r\\n|\\r|\\n","");
		if(!body.isEmpty()){
			body = getMethodSignature(body);
		}
		return body;
	}

	public static void unMergeNonJavaFiles(String dir){
		try {
			File revFile = new File(dir);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(revFile.getAbsolutePath()))); 
			String leftRevName 	= bufferedReader.readLine();
			String baseRevName 	= bufferedReader.readLine();
			String rightRevName = bufferedReader.readLine();
			String baseFolder 	= revFile.getParentFile() + File.separator + "_nonJavaFiles" + File.separator + baseRevName;
			String leftFolder 	= revFile.getParentFile() + File.separator + "_nonJavaFiles" + File.separator + leftRevName;
			String rightFolder 	= revFile.getParentFile() + File.separator + "_nonJavaFiles" + File.separator + rightRevName;
			bufferedReader.close();

			unMergeNonJavaFilesAux(leftRevName, leftFolder, baseRevName, baseFolder, rightRevName, rightFolder, true);
			unMergeNonJavaFilesAux(leftRevName, leftFolder, baseRevName, baseFolder, rightRevName, rightFolder, false);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String getSingleLineContent(String content) {
		return (content.replaceAll("\\r\\n|\\r|\\n",""));
	}

	public static String getSingleLineContentNoSpacing(String content) {
		return (content.replaceAll("\\r\\n|\\r|\\n","")).replaceAll("\\s+","");
	}

	public static boolean multimapContains(	Multimap<String, FSTNode> entries, String filename, FSTNode node) {
		for(FSTNode n : entries.get(filename)){
			if(n.getType().equals(node.getType()) && n.getName().equals(node.getName())){
				return true;
			}
		}
		return false;
	}

	public static String removeReservedKeywords(String str) {
		//		String[] keywords = {
		//				"assert",
		//				"abstract", "boolean", "break", "byte",
		//				"case", "catch", "char", "class",
		//				"const", "continue", "default", "do",
		//				"double", "else", "extends", "final",
		//				"finally", "float", "for", "goto",
		//				"if", "implements", "import",
		//				"instanceof", "int", "interface",
		//				"long", "native", "new", "package",
		//				"private", "protected", "public",
		//				"return", "short", "static", "super",
		//				"switch", "synchronized", "this",
		//				"throw", "throws", "transient",
		//				"try", "void", "volatile", "while"
		//		};

		String[] keywords = {
				"assert",
				"abstract", 
				"class",
				"const",
				"extends", 
				"final",
				"implements",
				"interface",
				"native",
				"private", "protected", "public",
				"static", "synchronized", "this",
				"throw", "throws", "transient",
				"volatile"
		};

		for (int i = 0; i < keywords.length; ++i) {
			str  = str.replaceAll(keywords[i], "");
		}
		return str;
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

	public static String readFileContent(File file){
		//StringBuilder content = new StringBuilder();
		String content = "";
		try{
			BufferedReader reader = Files.newBufferedReader(Paths.get(file.getAbsolutePath()), StandardCharsets.ISO_8859_1);
			content = reader.lines().collect(Collectors.joining("\n"));
		}catch(Exception e){
			//System.err.println(e.getMessage());
		}
		return content;
	}

	public static List<MergeConflict> extractMergeConflicts(String mergedCode){
		//FPFN uncomment String CONFLICT_HEADER_BEGIN= "<<<<<<< MINE";
		String CONFLICT_HEADER_BEGIN= "<<<<<<<";
		String CONFLICT_MID			= "=======";
		//String CONFLICT_HEADER_END 	= ">>>>>>> YOURS";
		String CONFLICT_HEADER_END 	= ">>>>>>>";
		String leftConflictingContent = "";
		String rightConflictingContent= "";
		boolean isConflictOpen		  = false;
		boolean isLeftContent		  = false;

		List<MergeConflict> mergeConflicts = new ArrayList<MergeConflict>();
		List<String> lines = new ArrayList<>();
		BufferedReader reader = new BufferedReader(new StringReader(mergedCode));
		lines = reader.lines().collect(Collectors.toList());
		Iterator<String> itlines = lines.iterator();
		while(itlines.hasNext()){
			String line = itlines.next();
			if(line.contains(CONFLICT_HEADER_BEGIN)){
				isConflictOpen = true;
				isLeftContent  = true;
			}
			else if(line.contains(CONFLICT_MID)){
				leftConflictingContent+=line + "\n";
				isLeftContent = false;
			}
			else if(line.contains(CONFLICT_HEADER_END)) {
				rightConflictingContent+=line + "\n";
				MergeConflict mergeConflict = new MergeConflict(leftConflictingContent,rightConflictingContent);
				mergeConflicts.add(mergeConflict);
				//reseting the flags
				isConflictOpen	= false;
				isLeftContent   = false;
				leftConflictingContent = "";
				rightConflictingContent= "";
			} else {
				if(isConflictOpen){
					if(isLeftContent){leftConflictingContent+=line + "\n";
					}else{rightConflictingContent+=line + "\n";}
				}
			}
		}
		return mergeConflicts;
	}

	public static List<String> listJavaSsmergedFilesPath(String directory){
		List<String> allFiles = new ArrayList<String>();
		File[] fList = (new File(directory)).listFiles();
		if(fList != null){
			for (File file : fList){
				if (file.isFile() && FilenameUtils.getExtension(file.getAbsolutePath()).equalsIgnoreCase("java")){
					String unmergedFilePath = file.getAbsolutePath() +".merge";
					if(new File(unmergedFilePath).exists()){ //only pairs of .java and .java.merge files interest
						allFiles.add(file.getAbsolutePath());
					}

				} else if (file.isDirectory()){
					allFiles.addAll(listJavaSsmergedFilesPath(file.getAbsolutePath()));
				}
			}
		}
		return allFiles;
	}

	private static String getFileContents(String filePath){
		String content = "";
		try {
			StringBuilder fileData = new StringBuilder(1000);
			BufferedReader reader = new BufferedReader(new FileReader(filePath));
			char[] buf = new char[10];
			int numRead = 0;
			while ((numRead = reader.read(buf)) != -1) {
				String readData = String.valueOf(buf, 0, numRead);
				fileData.append(readData);
				buf = new char[1024];
			}
			reader.close();
			content = fileData.toString();	
		} catch (Exception e) {return content;} 
		return content;
	}

	private static String normalizeParameters(String parameters){
		String normalizedParameters = "";
		String[] strs = parameters.split("-");
		for(int i = 0; i < strs.length; i++){
			if(i % 2 == 0){
				normalizedParameters+=(strs[i]+",");
			}
		}
		normalizedParameters = (normalizedParameters.substring(0,normalizedParameters.length()-1)) + "";
		return normalizedParameters;
	}

	private static void addFilesToJar(File source, JarOutputStream target) throws IOException {
		BufferedInputStream in = null;
		try {
			if (source.isDirectory()) {
				String name = source.getPath().replace("\\", "/");
				if (!name.isEmpty()) {
					if (!name.endsWith("/"))
						name += "/";
					JarEntry entry = new JarEntry(name);
					entry.setTime(source.lastModified());
					target.putNextEntry(entry);
					target.closeEntry();
				}
				for (File nestedFile : source.listFiles())
					addFilesToJar(nestedFile, target);
				return;
			}

			JarEntry entry = new JarEntry(source.getPath().replace("\\", "/"));
			entry.setTime(source.lastModified());
			target.putNextEntry(entry);
			in = new BufferedInputStream(new FileInputStream(source));

			byte[] buffer = new byte[1024];
			while (true) {
				int count = in.read(buffer);
				if (count == -1)
					break;
				target.write(buffer, 0, count);
			}
			target.closeEntry();
		} finally {
			if (in != null)
				in.close();
		}
	}

	private static String getMethodReturnType(String methodSignature) {
		String[] strs = methodSignature.split("['\\s]");
		String returnType = "";
		if (hasAccessModifier(strs[0])) {
			if (isStatic(strs[1])) {
				if (!isGeneric(strs[2])) {
					returnType = strs[2];
				} else {
					returnType = getMethodReturnType(removeGenerics(methodSignature));
				}
			} else {
				if (!isGeneric(strs[1])) {
					returnType = strs[1];
				} else {
					returnType = getMethodReturnType(removeGenerics(methodSignature));
				}
			}
		} else {
			if (isStatic(strs[0])) {
				if (!isGeneric(strs[1])) {
					returnType = strs[1];
				} else {
					returnType = getMethodReturnType(removeGenerics(methodSignature));
				}
			} else {
				if (!isGeneric(strs[0])) {
					returnType = strs[0];
				} else {
					returnType = getMethodReturnType(removeGenerics(methodSignature));
				}
			}
		}
		return returnType;
	}

	private static String getPrimitiveTypeDefaultValue(String returnType) {
		String defaultValue = "";
		if (returnType.equalsIgnoreCase("int")
				|| returnType.equalsIgnoreCase("byte")
				|| returnType.equalsIgnoreCase("short")
				|| returnType.equalsIgnoreCase("long")
				|| returnType.equalsIgnoreCase("float")
				|| returnType.equalsIgnoreCase("double")) {
			defaultValue = "0";
		} else if (returnType.equalsIgnoreCase("char")) {
			defaultValue = "'\u0000'";
		} else if (returnType.equalsIgnoreCase("boolean")) {
			defaultValue = "false";
		} else {
			defaultValue = "null";
		}
		return defaultValue;
	}

	private static String removeGenerics(String methodSignature) {
		String res = "";
		int count = -1;
		boolean first = true;
		boolean canTake = false;
		for (int i = 0, n = methodSignature.length(); i < n; i++) {
			char chr = methodSignature.charAt(i);
			if (chr == '<') {
				if (first) {
					first = false;
					count = 1;
				} else {
					count++;
				}
			} else if (chr == '>') {
				count--;
			}
			if (canTake) {
				res += chr;
			}
			if (count == 0) {
				canTake = true;
			}
		}
		return res.replaceFirst(" ", "");
	}

	private static boolean isGeneric(String str) {
		return str.startsWith("<");
	}

	private static boolean isStatic(String str) {
		return str.equalsIgnoreCase("static");
	}

	private static boolean hasAccessModifier(String str) {
		return str.equalsIgnoreCase("private")
				|| str.equalsIgnoreCase("public")
				|| str.equalsIgnoreCase("protected");
	}

	private static void countConflicts(File folder, MergeResult mergeResult){
		try{
			File[] fList = folder.listFiles();
			for (File f : fList){
				if (f.isDirectory()){
					countConflicts(f, mergeResult);
				} else {
					if(	FilenameUtils.getExtension(f.getAbsolutePath()).equalsIgnoreCase("java") ||
							FilenameUtils.getExtension(f.getAbsolutePath()).equalsIgnoreCase("merge")){

						countHeaders(f,mergeResult);
					} 
				}
			}

			mergeResult.jdimeConfs	+= Util.JDIME_CONFS;
			mergeResult.jdimeLOC	+= Util.JDIME_LOCS;
			mergeResult.jdimeFiles	+= Util.JDIME_FILES;

			//reseting
			Util.JDIME_CONFS 	= 0;
			Util.JDIME_LOCS 	= 0;
			Util.JDIME_FILES	= 0;
		}catch(Exception e){
			return;
		}
	}

	private static void countHeaders(File f, MergeResult mergeResult) {
		try {
			String CONFLICT_HEADER_BEGIN= "<<<<<<<";
			String CONFLICT_HEADER_END 	= ">>>>>>>";
			String SEMANTIC_HEADER 		= "~~FSTMerge~~";

			boolean isConflictOpen		= false;
			boolean isConflictingFile	= false;

			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(f.getAbsolutePath())));   
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				//if(!line.contains("//") && !line.contains("/*") && line.contains(CONFLICT_HEADER_END)) {
				if(line.contains(CONFLICT_HEADER_END)) {
					isConflictOpen	= false;
				}

				if(isConflictOpen){
					if(FilenameUtils.getExtension(f.getAbsolutePath()).equalsIgnoreCase("java")){
						mergeResult.ssmergeLOC++;
					} else if(FilenameUtils.getExtension(f.getAbsolutePath()).equalsIgnoreCase("merge")) {
						mergeResult.linebasedLOC++;
					}
				}

				//if(!line.contains("//") && !line.contains("/*") && line.contains(CONFLICT_HEADER_BEGIN)){
				if(line.contains(CONFLICT_HEADER_BEGIN)){

					isConflictingFile = true;
					isConflictOpen = true;
					if(FilenameUtils.getExtension(f.getAbsolutePath()).equalsIgnoreCase("java")){
						mergeResult.ssmergeConfs++;
						FSTGenMerger.javaFilesConfsSS++;
					} else if(FilenameUtils.getExtension(f.getAbsolutePath()).equalsIgnoreCase("merge")) {
						mergeResult.linedbasedConfs++;
						FSTGenMerger.javaFilesConfsUN++;

					}
				} else if(line.contains(SEMANTIC_HEADER)) {
					mergeResult.semanticConfs++;
				}
			}
			if(isConflictingFile){
				if(FilenameUtils.getExtension(f.getAbsolutePath()).equalsIgnoreCase("java")){
					mergeResult.ssmergeFiles++;
				} else if(FilenameUtils.getExtension(f.getAbsolutePath()).equalsIgnoreCase("merge")) {
					mergeResult.linebasedFiles++;
				}
			}

			bufferedReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void printConflictsReport(String revision, MergeResult mergeResult) {
		try {
			String header = "";
			File file = new File("results/ssmerge_conflicts_report.csv" );
			if(!file.exists()){
				file.createNewFile();
				header = "revision;ssmergeConfs;linedbasedConfs;ssmergeLOC;linebasedLOC;ssmergeFiles;linebasedFiles;semanticConfs;jdimeConfs;jdimeLOC;jdimeFiles;equalConfs\n";
			}	
			PrintWriter pw = new PrintWriter(new FileOutputStream(file, true), true);
			if(!header.isEmpty()){pw.append(header);}
			pw.append(revision+";"+mergeResult.ssmergeConfs+";"+mergeResult.linedbasedConfs+";" +mergeResult.ssmergeLOC+";"
					+mergeResult.linebasedLOC+";"+mergeResult.ssmergeFiles+";"+mergeResult.linebasedFiles+";"
					+mergeResult.semanticConfs+";"
					+mergeResult.jdimeConfs+";"+mergeResult.jdimeLOC+";"+mergeResult.jdimeFiles+";"+mergeResult.equalConfs+"\n");
			pw.flush();pw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void unMergeNonJavaFilesAux(String leftRevName, String leftFolder,
			String baseRevName, String baseFolder, String rightRevName,
			String rightFolder, boolean includeBase) {

		try{
			File directory;
			if(includeBase){
				directory = new File(baseFolder);
			} else{
				directory = new File(leftFolder);
			}

			if(directory.exists()){
				File[] fList = directory.listFiles();
				for (File file : fList){

					if (file.isDirectory()){
						if(includeBase){
							unMergeNonJavaFilesAux(leftRevName, leftFolder, baseRevName,  file.getAbsolutePath(), rightRevName, rightFolder, includeBase);
						} else{
							unMergeNonJavaFilesAux(leftRevName, file.getAbsolutePath(), baseRevName, baseFolder, rightRevName, rightFolder, includeBase);
						}

					} else {

						String ref = "";
						if(includeBase){
							ref = baseRevName;
						} else {
							ref = leftRevName;
						}

						String leftFilePath   = file.getAbsolutePath().replaceFirst(ref, leftRevName);
						String rightFilePath  = file.getAbsolutePath().replaceFirst(ref, rightRevName);
						String baseFilePath	  = file.getAbsolutePath().replaceFirst(ref, baseRevName);

						File leftfile 	= new File(leftFilePath);
						File rightfile 	= new File(rightFilePath);
						File basefile 	= new File(baseFilePath);


						if(!basefile.exists()){
							basefile.mkdirs();
							basefile.createNewFile();
						}
						if(!rightfile.exists()){
							rightfile.mkdirs();
							rightfile.createNewFile();
						}
						if(!leftfile.exists()){
							leftfile.mkdirs();
							leftfile.createNewFile();
						}


						String mergeCmdOriginal 	= ""; 
						if(System.getProperty("os.name").contains("Windows")){
							mergeCmdOriginal = "C:/KDiff3/bin/diff3.exe -m -E " + "\"" + leftfile.getPath() + "\"" + " " + "\"" + basefile.getPath() + "\"" + " " + "\"" + rightfile.getPath() + "\"";
						}else{
							mergeCmdOriginal = "diff3 -m -E " + leftfile.getPath() + " " + basefile.getPath() + " " + rightfile.getPath();
						}
						Runtime run = Runtime.getRuntime();
						Process pr 	= run.exec(mergeCmdOriginal);
						BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
						String line = "";
						String mergedContent = "";
						while ((line=buf.readLine())!=null) {
							mergedContent += line + "\n";
						}

						countConflictsUnMergeNonJavaFiles(mergedContent);


						leftfile .setWritable(true);
						basefile .setWritable(true);
						rightfile.setWritable(true);
						leftfile .delete();
						basefile .delete();
						rightfile.delete();
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			System.exit(0);
		}

	}

	private static void countConflictsUnMergeNonJavaFiles(String mergedContent ) {
		String CONFLICT_HEADER_BEGIN= "<<<<<<<";
		String CONFLICT_HEADER_END 	= ">>>>>>>";

		boolean isConflictOpen		= false;
		boolean isConflictingFile	= false;

		Scanner scanner = new Scanner(mergedContent);
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if(!line.contains("//") && !line.contains("/*") && line.contains(CONFLICT_HEADER_END)) {
				isConflictOpen	= false;
			}
			if(isConflictOpen){
			}
			if(!line.contains("//") && !line.contains("/*") && line.contains(CONFLICT_HEADER_BEGIN)){
				isConflictingFile = true;
				isConflictOpen = true;

				FSTGenMerger.nonJavaFilesConfs++;
			} 
		}
		scanner.close();
		if(isConflictingFile){
		}
	}

	private static boolean areSimilarConflicts(MergeConflict confa,	MergeConflict confb) {
		if (null == confa || null == confb)
			return false;
		else {
			String ljfst = getSingleLineContentNoSpacing(confa.left);
			String rjfst = getSingleLineContentNoSpacing(confa.right);

			String ljdm = getSingleLineContentNoSpacing(confb.left);
			String rjdm = getSingleLineContentNoSpacing(confb.right);

			return ljfst.equals(ljdm) && rjfst.equals(rjdm);
		}
	}

	private static List<MergeConflict> countEqualConflicts(File mergedFolder, MergeResult mergeResult) {
		List<String> logEqualConfs 	= new ArrayList<String>();
		List<String> logDiffeConfsUN= new ArrayList<String>();
		List<String> logDiffeConfsSS= new ArrayList<String>();

		List<String> logDiffeConfsUNInclBase= new ArrayList<String>();

		List<String> mergedFilesPath = listJavaSsmergedFilesPath(mergedFolder.getAbsolutePath());

		//reseting to avoid bias
		FSTGenMerger.javaFilesConfsSS=0;FSTGenMerger.javaFilesConfsUN=0;mergeResult.ssmergeConfs=0; mergeResult.linedbasedConfs=0; 

		//list of different textual confs for futher analysis
		List<MergeConflict> unmergediffconfs = new ArrayList<MergeConflict>();

		for(String ssmergePath : mergedFilesPath){
			File ssmerge = new File(ssmergePath);
			File unmerge = new File(ssmergePath + ".merge");
			if(ssmerge.exists() && unmerge.exists()){
				String ssmergecontent = readFileContent(ssmerge);
				String unmergeconent  = readFileContent(unmerge);

				List<MergeConflict> ssmergeconfs = extractMergeConflicts(ssmergecontent);
				List<MergeConflict> unmergeconfs = extractMergeConflicts(unmergeconent);

				FSTGenMerger.javaFilesConfsSS 	+= ssmergeconfs.size();
				FSTGenMerger.javaFilesConfsUN 	+= unmergeconfs.size();
				mergeResult.ssmergeConfs		+= ssmergeconfs.size();
				mergeResult.linedbasedConfs 	+= unmergeconfs.size();

				for(MergeConflict unmergeconf : unmergeconfs){
					boolean foundEqual = false;
					for(MergeConflict ssmergeconf : ssmergeconfs){
						if(areSimilarConflicts(ssmergeconf, unmergeconf)){
							foundEqual = true;

							FSTGenMerger.javaEqualConfs++;
							mergeResult.equalConfs++;

							//logging
							String entry = ssmergePath+";"+ssmergeconf.toString()+";"+unmergeconf.toString();
							logEqualConfs.add(entry);

							break;
						} 
					}
					if(!foundEqual){
						String entry = ssmergePath+";"+unmergeconf.toString();
						logDiffeConfsUN.add(entry);

						unmergeconf.originpath = ssmergePath;
						unmergediffconfs.add(unmergeconf);

						//detailed log for manual analysis
						if(unmergeconf.bodyInclBase != null){
							entry =makeDetailedEntryUnmergeConfInclBase(ssmergePath,ssmerge, unmerge, unmergeconf, mergeResult);
							logDiffeConfsUNInclBase.add(entry);
						}
					}
				}

				for(MergeConflict ssmergeconf : ssmergeconfs){
					boolean foundEqual = false;
					for(MergeConflict unmergeconf : unmergeconfs){
						if(areSimilarConflicts(ssmergeconf, unmergeconf)){
							foundEqual = true;
							break;
						} 
					}
					if(!foundEqual){
						String entry = ssmergePath+";"+ssmergeconf.toString();
						logDiffeConfsSS.add(entry);
					}
				}
			}
		}
		printEqualConflictsLog(logEqualConfs);
		printDifferentConflictsLog(logDiffeConfsUN,false,false);
		printDifferentConflictsLog(logDiffeConfsUNInclBase,false,true);
		printDifferentConflictsLog(logDiffeConfsSS,true,false);

		return unmergediffconfs;
	}

	private static String makeDetailedEntryUnmergeConfInclBase(
			String ssmergePath, File ssmerge, File unmerge,
			MergeConflict unmergeconf, MergeResult mergeResult) {
		File revisionsFile = new File( mergeResult.revision );
		String[] revisions = (readFileContent(revisionsFile)).split("\\r?\\n");
		String left = revisions[0]; 
		String base = revisions[1];
		String right= revisions[2];

		String leftContent = readFileContent(new File((revisionsFile.getParent()+"/"+left+"/"+ssmerge.getName())));
		String baseContent = readFileContent(new File((revisionsFile.getParent()+"/"+base+"/"+ssmerge.getName())));
		String rightContent= readFileContent(new File((revisionsFile.getParent()+"/"+right+"/"+ssmerge.getName())));
		String ssmeContent = readFileContent(ssmerge);
		String unmeContent = readFileContent(unmerge);

		StringBuilder builder = new StringBuilder();
		builder.append("########################################################\n");
		builder.append("File: " + ssmergePath + "\n");
		builder.append("Conflict:\n" + unmergeconf.toStringInclBase() + "\n");
		builder.append("Unstructered Merge Output:\n" + unmeContent   + "\n");
		builder.append("Semistructured Merge Output:\n" + ssmeContent + "\n");
		builder.append("Left Content:\n" + ((leftContent.isEmpty()) ?"<empty>":leftContent) + "\n");
		builder.append("Base Content:\n" + ((baseContent.isEmpty()) ?"<empty>":baseContent) + "\n");
		builder.append("Right Content:\n"+ ((rightContent.isEmpty())?"<empty>":rightContent)+ "\n");
		return builder.toString();
	}

	private static void printEqualConflictsLog(List<String> logentries) {
		try {
			String header = "";
			File file = new File("results/log_ssmerge_equal_conflicts.csv" );
			if(!file.exists()){file.createNewFile();header = "file;ssmergeConf;linedbasedConf\n";}
			PrintWriter pw = new PrintWriter(new FileOutputStream(file, true), true);
			try{
				if(!header.isEmpty()){pw.append(header);}
				for(String entry : logentries){pw.append(entry + "\n");}
				pw.flush();pw.close();
			}finally{
				try{pw.close();}
				catch(Exception e){e.printStackTrace();}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void printDifferentConflictsLog(List<String> logentries, boolean isssmerge, boolean inclbase) {
		try {
			String header = "";
			File file = (!isssmerge)?
					((!inclbase)?new File("results/log_ssmerge_different_conflicts_textual.csv" ):new File("results/log_ssmerge_different_conflicts_textual_inclbase.csv" )) 
					: new File("results/log_ssmerge_different_conflicts_ssmerge.csv") ;
					if(!file.exists()){file.createNewFile();header = (!isssmerge)? "file;linedbasedConf\n" : "file;ssmergeConf\n" ;}
					PrintWriter pw = new PrintWriter(new FileOutputStream(file, true), true);
					try{
						if(!header.isEmpty()){pw.append(header);}
						for(String entry : logentries){pw.append(entry + "\n");}
						pw.flush();pw.close();
					}finally{
						try{pw.close();}
						catch(Exception e){e.printStackTrace();}
					}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
