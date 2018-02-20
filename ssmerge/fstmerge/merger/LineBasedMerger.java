package merger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import modification.traversalLanguageParser.addressManagement.DuplicateFreeLinkedList;
import util.FFPNSpacingAndConsecutiveLinesFinder;
import util.MergeConflict;
import util.Util;
//import de.fosd.jdime.strategy.StructuredStrategy;
import de.ovgu.cide.fstgen.ast.FSTNode;
import de.ovgu.cide.fstgen.ast.FSTNonTerminal;
import de.ovgu.cide.fstgen.ast.FSTTerminal;


public class LineBasedMerger implements MergerInterface {
	String encoding = "UTF-8";

	//FPFN RENAMING ISSUE
	private int countOfPossibleRenames 			= 0;
	public static DuplicateFreeLinkedList<File> errorFiles 	= null;
	public static String currentFile			= "";
	String currentRevision						= "";
	Map<String, String> mapRenamingConflicts	= new HashMap<String, String>();
	private int countOfRenamesDueToIdentation 	= 0;

	//FPFN DUPLICATED METHOD ISSUE
	List<String> listDuplicatedMethods			= new ArrayList<String>();
	private int countOfPossibleDuplications		= 0;
	List<FSTTerminal> possibleDuplications		= new ArrayList<FSTTerminal>();

	//FPFN
	int countOfConsecutiveLinesConfs 			  = 0;
	int countOfSpacingConfs 					  = 0;
	int countOfConsecutiveLinesAndSpacingConfs 	  = 0;
	int countOfEditionsToDifferentPartsOfSameStmt = 0;
	boolean hadRenaming							  = false;

	//FPFN ENUMS
	public int countOfEnumsPossibleRenames = 0;
	public Map<String, String> mapEnumRenamingConflicts	= new HashMap<String, String>();

	static final String CONFLICT_DELIMITER 	= "<<<<<";
	static final int LEFT_CONTENT 	= 0;
	static final int BASE_CONTENT 	= 1;
	static final int RIGHT_CONTENT 	= 2;

	public void merge(FSTTerminal node) throws ContentMergeException {

		String body = node.getBody() + " ";
		String[] tokens = body.split(FSTGenMerger.MERGE_SEPARATOR);

		try {
			tokens[0] = tokens[0].replace(FSTGenMerger.SEMANTIC_MERGE_MARKER, "").trim();
			tokens[1] = tokens[1].trim();
			tokens[2] = tokens[2].trim();
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("|"+body+"|");
			e.printStackTrace();
		}

		//System.out.println("|" + tokens[0] + "|");
		//System.out.println("|" + tokens[1] + "|");
		//System.out.println("|" + tokens[2] + "|");
		//System.out.println("--------------------");

		// SPECIAL CONFLICT HANDLER
		if(!(node.getType().contains("-Content") ||	node.getMergingMechanism().equals("LineBased"))) {

			//FPFN NEW ARTEFACT REFERENCING EDITED ONE
			identifyEditedNodes(tokens,node);

			if(tokens[0].length() == 0 && tokens[1].length() == 0 && tokens[2].length() == 0) {
				node.setBody("");
			} else if(tokens[0].equals(tokens[2])) {
				node.setBody(tokens[0]);
			} else if(tokens[0].equals(tokens[1]) && tokens[2].length() > 0) {
				node.setBody(tokens[2]);
			} else if(tokens[2].equals(tokens[1]) && tokens[0].length() > 0) {
				node.setBody(tokens[0]);
			} else if(tokens[0].equals(tokens[1]) && tokens[2].length() == 0) {
				node.setBody("");
			} else if(tokens[2].equals(tokens[1]) && tokens[0].length() == 0) {
				node.setBody("");
			}

			//System.out.println(node.getMergingMechanism());
			//System.out.println("|" + tokens[1] + "|");
			//System.out.println("|" + tokens[2] + "|");
			//System.out.println("--------------------");

			return;
		}

		//streams
		Process pr = null;
		BufferedReader buf = null;

		try {
			long time = System.currentTimeMillis();
			File tmpDir = new File(System.getProperty("user.dir") + File.separator + "fstmerge_tmp"+time);
			//File tmpDir = new File(System.getProperty("user.home") + File.separator +"ssmerge" + File.separator + "fstmerge_tmp"+time);
			//File tmpDir = new File("E:" + File.separator +"ssmerge" + File.separator + "fstmerge_tmp"+time);
			tmpDir.mkdir();

			//			File fileVar1 = File.createTempFile("fstmerge_var1_", "", tmpDir);
			//			File fileBase = File.createTempFile("fstmerge_base_", "", tmpDir);
			//			File fileVar2 = File.createTempFile("fstmerge_var2_", "", tmpDir);

			//FPFN
			File fileVar1 = File.createTempFile("fstmerge_var1_", ".java", tmpDir);
			File fileBase = File.createTempFile("fstmerge_base_", ".java", tmpDir);
			File fileVar2 = File.createTempFile("fstmerge_var2_", ".java", tmpDir);

			BufferedWriter writerVar1 = new BufferedWriter(new FileWriter(fileVar1));
			if(node.getType().contains("-Content") || tokens[0].length() == 0)
				writerVar1.write(tokens[0]);
			else 
				writerVar1.write(tokens[0] + "\n");
			writerVar1.close();

			BufferedWriter writerBase = new BufferedWriter(new FileWriter(fileBase));
			if(node.getType().contains("-Content") || tokens[1].length() == 0)
				writerBase.write(tokens[1]);
			else 
				writerBase.write(tokens[1] + "\n");
			writerBase.close();

			BufferedWriter writerVar2 = new BufferedWriter(new FileWriter(fileVar2));
			if(node.getType().contains("-Content") || tokens[2].length() == 0)
				writerVar2.write(tokens[2]);
			else 
				writerVar2.write(tokens[2] + "\n");
			writerVar2.close();

			String mergeCmdInclBase 	= ""; 
			String mergeCmdOriginal 	= ""; 
			if(System.getProperty("os.name").contains("Windows")){
				mergeCmdInclBase = "C:/KDiff3/bin/diff3.exe -m " + "\"" + fileVar1.getPath() + "\"" + " " + "\"" + fileBase.getPath() + "\"" + " " + "\"" + fileVar2.getPath() + "\"";// + " > " + fileVar1.getName() + "_output";
				mergeCmdOriginal = "C:/KDiff3/bin/diff3.exe -m -E " + "\"" + fileVar1.getPath() + "\"" + " " + "\"" + fileBase.getPath() + "\"" + " " + "\"" + fileVar2.getPath() + "\"";// + " > " + fileVar1.getName() + "_output";
			}else{
				mergeCmdInclBase = "diff3 -m " + fileVar1.getPath() + " " + fileBase.getPath() + " " + fileVar2.getPath();// + " > " + fileVar1.getName() + "_output";
				mergeCmdOriginal = "diff3 -m -E " + fileVar1.getPath() + " " + fileBase.getPath() + " " + fileVar2.getPath();// + " > " + fileVar1.getName() + "_output";
			}

			//merge showing base
			Runtime run = Runtime.getRuntime();
			pr 	= run.exec(mergeCmdInclBase);
			buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			String line 			= "";
			String resultInclBase 	= "";
			while ((line=buf.readLine())!=null) {
				resultInclBase += line + "\n";
			}
			pr.getInputStream().close();
			pr.getOutputStream().close();
			buf.close();

			//merge default
			pr 	 = run.exec(mergeCmdOriginal);
			buf  = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			line = "";
			String resultOriginal 	= "";
			while ((line=buf.readLine())!=null) {
				resultOriginal += line + "\n";
			}
			pr.getInputStream().close();
			pr.getOutputStream().close();
			buf.close();


			//DIFFMERGED
			//			if(!res.contains(this.CONFLICT_DELIMITER)){
			//				DiffMerged diffMerged = new DiffMerged();
			//				ArrayList<ArrayList<String>> linesContributions = diffMerged.findLinesContributions(fileVar1, fileVar2, fileBase);
			//				res = diffMerged.markLineContributions(linesContributions, res);
			//			}

			//node.setBody(resultOriginal);

			node.setBody(resultInclBase);


			//FPFN RENAMING ISSUE
			if(hadConflict(node)){
				identifyAndAccountRenamingAndDuplications(node, tokens,false);
			}

			//FPFN SPACING AND CONSECUTIVE LINES
			if(hadConflict(node) && !hadRenaming && isMethodOrConstructor(node) && isNotErrorFile()){
				node.setBody(resultInclBase);

				//logging info
				String signature  	  = this.getMethodSignature(node);
				String mergetracking  = ((this.getMergedFolder()).replaceAll("/", Matcher.quoteReplacement(File.separator))) + ";"+ LineBasedMerger.getFileAbsolutePath(node)+";"+signature;

				FFPNSpacingAndConsecutiveLinesFinder finder = new FFPNSpacingAndConsecutiveLinesFinder(node,mergetracking);
				finder.checkFalsePositives();
				this.countOfConsecutiveLinesConfs 			+= finder.getConsecutiveLines();
				this.countOfSpacingConfs 					+= finder.getDifferentSpacing();
				this.countOfConsecutiveLinesAndSpacingConfs += finder.getSpacingAndConsecutiveLinesIntersection();

				this.hadRenaming = false;
			}

			node.setBody(resultOriginal);

			//FPFN NEW ARTEFACT REFERENCING EDITED ONE
			if((!hadConflict(node)) && isMethodOrConstructor(node) && isNotErrorFile()){
				identifyEditedMethodWithoutConflict(node, tokens);
			}

			//FPFN NEW ARTEFACT REFERENCING EDITED ONE
			if(node.getType().equals(".java-Content") && isNotErrorFile()){
				++FSTGenMerger.javaMergedFiles;

				System.out.println("Extracting Conflicts of " + node.getName() + "...");
				String file = LineBasedMerger.getFileAbsolutePath(node) + ".java";
				ArrayList<MergeConflict> mergeConflicts = Util.getConflicts(file,node);
				FSTGenMerger.mapMergeConflicts.put(file, mergeConflicts);
				System.out.println("Extracting Conflicts of " + node.getName() + " done!");

				//to counting purposes
				node.setBody(resultInclBase);
			}


			buf = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
			while ((line=buf.readLine())!=null) {
				System.err.println(line);
			}
			pr.getErrorStream().close();
			pr.getOutputStream().close();

			fileVar1.delete();
			fileBase.delete();
			fileVar2.delete();
			tmpDir.delete();

		} catch (Exception e) {
			e.printStackTrace();
			if (buf != null) {
				try {
					buf.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			if (pr != null) {
				try {
					pr.getInputStream().close();
					pr.getOutputStream().close();
					pr.getErrorStream().close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			System.exit(0);
		}  
	}

	//FPFN NEW ARTEFACT REFERENCING EDITED ONE
	private void identifyEditedNodes(String[] tokens, FSTTerminal node) {
		String left  = tokens[0];
		String base  = tokens[1];
		String right = tokens[2];
		String mergedfile = LineBasedMerger.getFileAbsolutePath(node) + ".java";

		String lefttrim 	= Util.getSingleLineContentNoSpacing(left);
		String basetrim 	= Util.getSingleLineContentNoSpacing(base);
		String righttrim 	= Util.getSingleLineContentNoSpacing(right);

		if(!lefttrim.isEmpty() && !basetrim.isEmpty() && !righttrim.isEmpty()){
			if(lefttrim.equals(basetrim) && !righttrim.equals(lefttrim)){ 		//left == base != right
				if(isValidEditedNode(right)){
					node.setBody(right);
					FSTGenMerger.editedNodesFromRight.put(mergedfile,node);
				}
			}
			else if(righttrim.equals(basetrim) && !lefttrim.equals(righttrim)){//left != base == right
				if(isValidEditedNode(left)){
					node.setBody(left);
					FSTGenMerger.editedNodesFromLeft.put(mergedfile,node);
				}
			}
		}
	}

	//FPFN NEW ARTEFACT REFERENCING EDITED ONE
	private boolean isValidEditedNode(String nodeContent) {
		String oneLineNodeContent = nodeContent.replaceAll("\\r\\n|\\r|\\n","");
		int numberOfWordsOfNodeContent = (oneLineNodeContent.split("\\s+")).length;
		return (numberOfWordsOfNodeContent > 1);
	}

	//FPFN NEW ARTEFACT REFERENCING EDITED ONE
	private void identifyEditedMethodWithoutConflict(FSTTerminal node,	String[] tokens) {
		String leftMethodDeclaration 	= tokens[LineBasedMerger.LEFT_CONTENT];
		String baseMethodDeclaration 	= tokens[LineBasedMerger.BASE_CONTENT];
		String rightMethodDeclaration 	= tokens[LineBasedMerger.RIGHT_CONTENT];

		leftMethodDeclaration = Util.getSingleLineContentNoSpacing(leftMethodDeclaration);
		baseMethodDeclaration = Util.getSingleLineContentNoSpacing(baseMethodDeclaration);
		rightMethodDeclaration= Util.getSingleLineContentNoSpacing(rightMethodDeclaration);

		LinkedList<String> entry = new LinkedList<String>();
		entry.add(LineBasedMerger.getFileAbsolutePath(node)+".java");

		if(!(leftMethodDeclaration.equals(baseMethodDeclaration) && leftMethodDeclaration.equals(rightMethodDeclaration))){
			if(!leftMethodDeclaration.equals(baseMethodDeclaration)){
				entry.add(leftMethodDeclaration);
				entry.add(this.getMethodSignature(node));
				FSTGenMerger.editedMethodsFromLeft.add(entry);
			} else if(!rightMethodDeclaration.equals(baseMethodDeclaration)){
				entry.add(rightMethodDeclaration);
				entry.add(this.getMethodSignature(node));
				FSTGenMerger.editedMethodsFromRight.add(entry);
			}
		}
	}

	//FPFN RENAMING ISSUE && DUPLICATED METHOD ISSUE
	private void identifyAndAccountRenamingAndDuplications(FSTTerminal node, String[] tokens, boolean checkDuplications) {
		if( 	node.getType().contains("MethodDecl")|| 
				node.getType().contains("FunctionDefinition") 	||
				node.getType().contains("classsmall_stmt1") 	||
				//node.getType().contains("ConstructorDecl") 	||
				node.getType().contains("class_member_declarationEnd6")){
			if(isNotErrorFile()){
				if(!tokens[LineBasedMerger.LEFT_CONTENT].isEmpty() && tokens[LineBasedMerger.BASE_CONTENT].isEmpty() && !tokens[LineBasedMerger.RIGHT_CONTENT].isEmpty()){
					String methodSignature = this.getMethodSignature(node);
					if(!methodSignature.equals("")){
						this.countOfPossibleDuplications++;
						String mergedFolder  = (this.getMergedFolder()).replaceAll("/", Matcher.quoteReplacement(File.separator));
						String candidateFile = mergedFolder + LineBasedMerger.getFileAbsolutePath(node) + ".java.merge";
						this.listDuplicatedMethods.add(mergedFolder+";"+candidateFile+";"+methodSignature);
					}			
				} 
			}
		} else if(node.getType().contains("EnumConstant")){
			if((  tokens[LineBasedMerger.LEFT_CONTENT].isEmpty() && !tokens[LineBasedMerger.BASE_CONTENT].isEmpty() && !tokens[LineBasedMerger.RIGHT_CONTENT].isEmpty()) ||
					(!tokens[LineBasedMerger.LEFT_CONTENT].isEmpty() && !tokens[LineBasedMerger.BASE_CONTENT].isEmpty() &&  tokens[LineBasedMerger.RIGHT_CONTENT].isEmpty())	){

				countOfEnumsPossibleRenames++;

				String mergedFolder    		  = (this.getMergedFolder()).replaceAll("/", Matcher.quoteReplacement(File.separator));
				String file					  = LineBasedMerger.getFileAbsolutePath(node);
				String enumRenamingEntry 	  = mergedFolder+";"+ file +";"+node.getName();
				this.mapEnumRenamingConflicts.put(file,enumRenamingEntry);
			}
		}
	}

	//FPFN IDENTATION RENAMING 
	private void logSpacingRenaming(String mergetracking, String method1, String method2) {
		String line 	= mergetracking + ";" + method1 + ";" + method2;
		try {
			File file = new File("results/log_ssmerge_identationrenaming.csv");
			if(!file.exists()){file.createNewFile();}
			FileWriter fw = new FileWriter(file, true);
			BufferedWriter bw = new BufferedWriter( fw );
			try{
				bw.write(line);
				bw.newLine();
				bw.close();
				fw.close();
			}catch(Exception e){
				bw.close();
				fw.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	//FPFN RENAMING ISSUE
	private boolean belongsToInterface(FSTTerminal node) {
		try{
			FSTNonTerminal parent = node.getParent();
			FSTTerminal son = (FSTTerminal) parent.getChildren().get(1);
			return son.getBody().contains("interface");
		} catch(NullPointerException E){
			return false;
		}
	}

	//FPFN RENAMING ISSUE
	private boolean isNotErrorFile(){
		String fileName = LineBasedMerger.currentFile.replaceAll(".merge","");
		for(File f: LineBasedMerger.errorFiles){
			if(f.getName().equals(fileName))
				return false;
		}
		return true;
	}

	//FPFN RENAMING ISSUE
	private String getMethodSignature(FSTTerminal node){
		String methodSignarute = "";
		methodSignarute = node.getName();
		return Util.simplifyMethodSignature(methodSignarute);
	}

	private String getUnMergeMethodSignature(FSTTerminal node){
		String methodBody = "";
		methodBody = node.getBody();
		return Util.unMergeMethodSignature((new FFPNSpacingAndConsecutiveLinesFinder()).splitConflictBody(methodBody));
	}

	//FPFN RENAMING ISSUE
	private static String getFilePath(FSTNode node){
		String dir = "";
		if(node == null){
		} else 	if(node.getType().equals("Folder")){
			dir = getFilePath(node.getParent()) + File.separator + node.getName();
		} else {
			dir = getFilePath(node.getParent()) + dir;
		}
		return dir;
	}

	//FPFN RENAMING ISSUE
	private String getMergedFolder() {
		//return ((currentRevision.split("\\."))[0]).replace("\\", "/");
		return ((currentRevision.split("\\."))[0]);
	}

	//FPFN 
	private boolean isMethodOrConstructor(FSTTerminal node){
		String nodeType = node.getType();
		boolean result = nodeType.equals("MethodDecl") || nodeType.equals("ConstructorDecl");	
		return result;
	}

	//FPFN
	private boolean hadConflict(FSTTerminal node) {
		return node.getBody().contains(LineBasedMerger.CONFLICT_DELIMITER);
	}

	//FPFN RENAMING ISSUE
	public int getCountOfPossibleRenames() {
		return countOfPossibleRenames;
	}

	//FPFN RENAMING ISSUE
	public void setCountOfPossibleRenames(int countOfPossibleRenames) {
		this.countOfPossibleRenames = countOfPossibleRenames;
	}

	//FPFN RENAMING ISSUE
	public int getCountOfRenamesDueToIdentation() {
		return countOfRenamesDueToIdentation;
	}

	//FPFN RENAMING ISSUE
	public void setCountOfRenamesDueToIdentation(int countOfRenamesDueToIdentation) {
		this.countOfRenamesDueToIdentation = countOfRenamesDueToIdentation;
	}

	//FPFN RENAMING ISSUE
	public static String getFileAbsolutePath(FSTTerminal node) {
		//return (this.getFilePath(node)+File.separator+(currentFile.split("\\.")[0])).replaceFirst((File.separator), "");
		return (getFilePath(node)+File.separator+(currentFile.split("\\.")[0]));
	}

	//FPFN DUPLICATED METHOD ISSUE
	public int getCountOfPossibleDuplications() {
		return countOfPossibleDuplications;
	}

	//FPFN DUPLICATED METHOD ISSUE
	public void setCountOfPossibleDuplications(int countOfPossibleDuplications) {
		this.countOfPossibleDuplications = countOfPossibleDuplications;
	}

	//FPFN CONSECUTIVE LINES
	public int getCountOfConsecutiveLinesConfs() {
		return countOfConsecutiveLinesConfs;
	}

	//FPFN CONSECUTIVE LINES
	public void setCountOfConsecutiveLinesConfs(int countOfConsecutiveLinesConfs) {
		this.countOfConsecutiveLinesConfs = countOfConsecutiveLinesConfs;
	}

	//FPFN SPACING
	public int getCountOfSpacingConfs() {
		return countOfSpacingConfs;
	}

	//FPFN SPACING
	public void setCountOfSpacingConfs(int countOfSpacingConfs) {
		this.countOfSpacingConfs = countOfSpacingConfs;
	}

	//FPFN CONSECUTIVE LINES AND SPACING
	public int getCountOfConsecutiveLinesAndSpacingConfs() {
		return countOfConsecutiveLinesAndSpacingConfs;
	}

	//FPFN CONSECUTIVE LINES AND SPACING
	public void setCountOfConsecutiveLinesAndSpacingConfs(
			int countOfConsecutiveLinesAndSpacingConfs) {
		this.countOfConsecutiveLinesAndSpacingConfs = countOfConsecutiveLinesAndSpacingConfs;
	}

	//FPFN EDITIONS
	public int getCountOfEditionsToDifferentPartsOfSameStmt() {
		return countOfEditionsToDifferentPartsOfSameStmt;
	}

	//FPFN EDITIONS
	public void setCountOfEditionsToDifferentPartsOfSameStmt(
			int countOfEditionsToDifferentPartsOfSameStmt) {
		this.countOfEditionsToDifferentPartsOfSameStmt = countOfEditionsToDifferentPartsOfSameStmt;
	}

}
