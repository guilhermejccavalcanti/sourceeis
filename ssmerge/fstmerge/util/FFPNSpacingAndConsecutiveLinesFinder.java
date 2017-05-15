package util;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import de.ovgu.cide.fstgen.ast.FSTTerminal;

enum PatternSameSignatureCM {
	smallMethod, renamedMethod, copiedMethod, copiedFile, noPattern
}

public  class FFPNSpacingAndConsecutiveLinesFinder {

	public static final String SSMERGE_SEPARATOR = "##FSTMerge##";

	public static final String DIFF3MERGE_SEPARATOR = "|||||||";

	private String body;

	private String filePath;

	private String nodeType;

	private int differentSpacing;

	private int consecutiveLines;

	private int numberOfConflicts;

	private int falsePositivesIntersection;

	//private boolean possibleRenaming;

	private ArrayList<String> conflicts;

	private String mergetracking;


	public FFPNSpacingAndConsecutiveLinesFinder(FSTTerminal node){
		this.body = node.getBody();
		this.nodeType = node.getType();
		this.conflicts = splitConflictsInsideMethods();
	}

	public FFPNSpacingAndConsecutiveLinesFinder(FSTTerminal node,String mergetracking) {
		this.body = node.getBody();
		this.nodeType = node.getType();
		this.mergetracking = mergetracking;
		//this.possibleRenaming = possibleRenaming;
		this.conflicts = splitConflictsInsideMethods();

	}

	public FFPNSpacingAndConsecutiveLinesFinder(){
	}

	public int getSpacingAndConsecutiveLinesIntersection() {
		return falsePositivesIntersection;
	}

	public void setFalsePositivesIntersection(int falsePositivesIntersection) {
		this.falsePositivesIntersection = falsePositivesIntersection;
	}

	public void checkFalsePositives(){
		if(conflicts.size() > 1){	
			for(String s : conflicts){
				this.auxCheckFalsePositives(s);
			}
		} else{
			this.auxCheckFalsePositives(conflicts.get(0));
		}	
	}

	public boolean checkDifferentSpacing(String [] splitConflictBody){
		boolean falsePositive = false;
		String[] temp = splitConflictBody.clone();
		String[] threeWay = this.removeInvisibleChars(temp);
		if(!threeWay[1].equals("")){
			if(threeWay[0].equals(threeWay[1]) || threeWay[2].equals(threeWay[1])){
				this.differentSpacing++;
				falsePositive = true;
			}
		}else{
			if(threeWay[0].equals("") || threeWay[0].equals(threeWay[2])){
				this.differentSpacing++;
				falsePositive = true;
			}

		}

		if(falsePositive)
			logging(splitConflictBody[0],splitConflictBody[1],splitConflictBody[2],"spacing");

		return falsePositive;
	}

	public String[] removeInvisibleChars(String[] input){
		input[0] = input[0].replaceAll("\\s+","");
		input[1] = input[1].replaceAll("\\s+","");
		input[2] = input[2].replaceAll("\\s+","");
		return input;
	}

	public boolean checkConsecutiveLines(String[] splitConflictBody){
		boolean falsePositive = false;
		if(!splitConflictBody[0].equals("") && (!splitConflictBody[1].equals("") && !splitConflictBody[2].equals(""))) {
			String [] leftLines  = splitConflictBody[0].split("\n");
			String [] baseLines  = splitConflictBody[1].split("\n");
			String [] rightLines = splitConflictBody[2].split("\n");
			if(!baseLines[0].equals("")){
				String fixedElement =  baseLines[0];
				boolean foundOnLeft = this.searchFixedElement(fixedElement, leftLines);
				if(foundOnLeft){
					falsePositive = true;
					this.consecutiveLines++;
				}else{
					boolean foundOnRight = this.searchFixedElement(fixedElement, rightLines);
					if(foundOnRight){
						falsePositive = true;
						this.consecutiveLines++;
					}
				}
			}

		}

		if(falsePositive)
			logging(splitConflictBody[0],splitConflictBody[1],splitConflictBody[2],"consecutivelines");

		return falsePositive;
	}

	public String [] splitConflictBody(String s){
		String [] splitBody = {"", "", ""};
		if(s.contains("|||||||")){
			String[] temp = s.split("\\|\\|\\|\\|\\|\\|\\|");

			String[] temp2 = temp[0].split("\n");
			splitBody[0] = extractLines(temp2);

			String [] baseRight = temp[1].split("=======");	
			temp2 = baseRight[0].split("\n");
			splitBody[1] = extractLines(temp2);
			temp2 = baseRight[1].split("\n");
			splitBody[2] = extractLines(temp2);
		}else{
			splitBody[1] = "";
			splitBody[0] = extractLines(s.split("=======")[0].split("\n"));
			splitBody[2] = extractLines(s.split("=======")[1].split("\n"));
			//			splitBody[0] = s.split("=======")[0];
			//			splitBody[2] = s.split("=======")[1];
		}

		return splitBody;
	}

	public int getNumberOfTruePositives(){
		int truePositives = this.numberOfConflicts - this.differentSpacing -
				this.consecutiveLines + this.falsePositivesIntersection;

		return truePositives;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public int getDifferentSpacing() {
		return differentSpacing;
	}

	public void setDifferentSpacing(int differentSpacing) {
		this.differentSpacing = differentSpacing;
	}

	public int getConsecutiveLines() {
		return consecutiveLines;
	}

	public void setConsecutiveLines(int consecutiveLines) {
		this.consecutiveLines = consecutiveLines;
	}

	public static void main(String[] args) {
		/*String example = "public void m(){\n" +
				"<<<<<<< /Users/paolaaccioly/Desktop/Teste/jdimeTests/left/Example.java\n" +
				"        int a1;\n" +
				"||||||| /Users/paolaaccioly/Desktop/Teste/jdimeTests/base/Example.java\n" +
				"        int a;\n" +
				"=======\n" +
				"            int a;\n" +
				">>>>>>> /Users/paolaaccioly/Desktop/Teste/jdimeTests/right/Example.java\n" +
				"        int b;\n" +
				"        int c;\n" +
				"<<<<<<< /Users/paolaaccioly/Desktop/Teste/jdimeTests/left/Example.java\n" +
				"        int d1;\n" +
				"||||||| /Users/paolaaccioly/Desktop/Teste/jdimeTests/base/Example.java\n" +
				"        int d;\n" +
				"=======\n" +
				"        int d2;\n" +
				">>>>>>> /Users/paolaaccioly/Desktop/Teste/jdimeTests/right/Example.java\n" +
				"    }";
		String example2 = "hello world";
		System.out.println(example2.split("mamae")[0]);*/
		/*String s = "<<<<<<< /Users/paolaaccioly/Documents/testeConflictsAnalyzer/conflictsAnalyzer/fstmerge_tmp1437435093749/fstmerge_var1_6882939852718786152\n" +
				"		int x;" +
				"||||||| /Users/paolaaccioly/Documents/testeConflictsAnalyzer/conflictsAnalyzer/fstmerge_tmp1437435093749/fstmerge_base_7436445259957106246\n" +
				"=======\n" +
				"		int y;\n"+
				">>>>>>> /Users/paolaaccioly/Documents/testeConflictsAnalyzer/conflictsAnalyzer/fstmerge_tmp1437435093749/fstmerge_var2_5667963733764531246\n";
		 */
	}

	private boolean searchFixedElement(String fixedElement, String[] variant){
		boolean foundFixedElement = false;
		int i = 0;
		while(!foundFixedElement && i < variant.length){
			if(variant[i].equals(fixedElement)){
				foundFixedElement = true;
			}
			i++;
		}
		return foundFixedElement;
	}

	private void logging(String left, String base, String right, String loggingoption){
		String line 	= this.mergetracking + ";" + left + ";" + base + ";" + right;
		String filename = "";
		switch (loggingoption) {
		case "spacing":
			filename = "log_ssmerge_spacing.csv";
			break;
		case "consecutivelines":
			filename = "log_ssmerge_consecutiveLines.csv";
			break;
		case "spacingandconsecutivelines":
			filename = "log_ssmerge_consecutiveLinesAndSpacingIntersection.csv";
			break;
		}

		try {
			String header = "";
			File file = new File( "results/"+filename);
			if(!file.exists()){
				file.createNewFile();
				header = "revision;file;methodsignature;leftbody;basebody;rightbody\n";
			}
			FileWriter fw = new FileWriter(file, true);
			BufferedWriter bw = new BufferedWriter( fw );
			try{
				if(!header.isEmpty()){
					bw.write(header);
				}
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

	private void auxCheckFalsePositives(String s) {
		String [] splitConflictBody = this.splitConflictBody(s);
		boolean diffSpacing = this.checkDifferentSpacing(splitConflictBody);
		boolean consecLines = false;
		//if(this.possibleRenaming == 0){
		consecLines = this.checkConsecutiveLines(splitConflictBody);
		//}
		if(diffSpacing && consecLines){
			this.falsePositivesIntersection++;
			logging(splitConflictBody[0], splitConflictBody[1], splitConflictBody[2], "spacingandconsecutivelines");
		}
	}

	private ArrayList<String> splitConflictsInsideMethods(){
		ArrayList<String> conflicts = new ArrayList<String>();
		if(this.body.contains("<<<<<<<") && this.body.contains(">>>>>>>")){
			String [] temp = this.body.split("<<<<<<<");
			for(int i = 1; i < temp.length; i++){
				String temp2 = temp[i].split(">>>>>>>")[0];
				conflicts.add(temp2);
			}
		}else{
			conflicts.add(this.body);
		}


		return conflicts;
	}

	private String extractLines(String[] conflict) {
		String lines = "";
		if(conflict.length > 1){
			for(int i = 1; i < conflict.length; i++){
				if(i != conflict.length-1){
					lines = lines + conflict[i] + "\n";
				}else{
					lines = lines + conflict[i];
				}
			}
		}
		return lines;
	}
}
