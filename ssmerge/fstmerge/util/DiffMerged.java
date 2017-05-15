package util;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;

/**
 *  Find the origins for the merged code lines contributions
 *   * 		SAMPLE DIFF3 OUTPUT REPRESENTING TWO DISJOINT MODIFICATIONS
		  ====3
			1:3,4c
			2:3,4c


			3:3,4c
			                int x;
			                int y;
			====1
			1:7,8c
			                int c;
			                int d;
			2:7,8c
			3:7,8c

 * @author Guilherme
 */

public class DiffMerged {

	public  ArrayList<ArrayList<String>> findLinesContributions(File left, File right, File base) throws IOException {
		String mergeCmd = "C:/KDiff3/bin/diff3.exe " + "\"" + left.getPath() + "\"" + " " + "\"" + base.getPath() + "\"" + " " + "\"" + right.getPath() + "\"";
		Runtime runTime = Runtime.getRuntime();
		Process process = runTime.exec(mergeCmd);

		ArrayList<ArrayList<String>> allContribs = new ArrayList<ArrayList<String>>(); 
		ArrayList<String> contribLinesFromLeft 	= new ArrayList<String>();
		ArrayList<String> contribLinesFromRight = new ArrayList<String>();

		String hunkPattern 			= "(=+)[1-3]"; //1 - LEFT, 2 - BASE, 3 - RIGHt
		String changePattern1		= "(\\d):(\\d)+[a-zA-Z]";
		String changePattern2		= "(\\d):(\\d)+,(\\d)+[a-zA-Z]";
		String fileIndicator 		= "";
		String[] changeIndicator	= null;


		BufferedReader buffer 	= new BufferedReader(new InputStreamReader(process.getInputStream()));
		String currentLine 		= "";
		while ((currentLine=buffer.readLine())!=null) {
			//System.out.println(currentLine);
			if(currentLine.matches(hunkPattern)){
				fileIndicator 		= currentLine.substring(currentLine.length()-1);
			} else if(currentLine.matches(changePattern1) || currentLine.matches(changePattern2)) {
				changeIndicator 	= currentLine.split(":");
				if(changeIndicator[0].equals(fileIndicator)){
					String lineIndicator = (changeIndicator[1]).replaceAll("[a-z]","");
					if(lineIndicator.contains(",")){
						addMultipleNumberOfLinesContributions(contribLinesFromLeft,contribLinesFromRight, fileIndicator,lineIndicator);

					} else {
						addNumberOfLineContribution(contribLinesFromLeft,contribLinesFromRight, fileIndicator,lineIndicator);				
					}
				}
			}
		}

		allContribs.add(contribLinesFromLeft);
		allContribs.add(contribLinesFromRight);
		return allContribs;
	}
	
	public  ArrayList<ArrayList<String>> findLinesContributionsNonNumeric(File left, File right, File base) throws IOException {
		String mergeCmd = "C:/KDiff3/bin/diff3.exe " + "\"" + left.getPath() + "\"" + " " + "\"" + base.getPath() + "\"" + " " + "\"" + right.getPath() + "\"";
		if(System.getProperty("os.name").contains("Windows")){
			mergeCmd = "C:/KDiff3/bin/diff3.exe " + "\"" + left.getPath() + "\"" + " " + "\"" + base.getPath() + "\"" + " " + "\"" + right.getPath() + "\"";
		}else{
			mergeCmd = "diff3 " + "\"" + left.getPath() + "\"" + " " + "\"" + base.getPath() + "\"" + " " + "\"" + right.getPath() + "\"";
		}
		Runtime runTime = Runtime.getRuntime();
		Process process = runTime.exec(mergeCmd);

		ArrayList<ArrayList<String>> allContribs = new ArrayList<ArrayList<String>>(); 
		ArrayList<String> contribLinesFromLeft 	= new ArrayList<String>();
		ArrayList<String> contribLinesFromRight = new ArrayList<String>();

		String hunkPattern 			= "(=+)[1-3]"; //1 - LEFT, 2 - BASE, 3 - RIGHt
		String changePattern1		= "(\\d):(\\d)+[a-zA-Z]";
		String changePattern2		= "(\\d):(\\d)+,(\\d)+[a-zA-Z]";
		String fileIndicator 		= "";
		String changeIndicator	= null;


		BufferedReader buffer 	= new BufferedReader(new InputStreamReader(process.getInputStream()));
		String currentLine 		= "";
		while ((currentLine=buffer.readLine())!=null) {
			//System.out.println(currentLine);
			if(currentLine.matches(hunkPattern)){
				fileIndicator 		= currentLine.substring(currentLine.length()-1);
			} else if(currentLine.matches(changePattern1) || currentLine.matches(changePattern2)) {
				changeIndicator 	= (currentLine.split(":"))[0];
			} else {
				if(changeIndicator.equals(fileIndicator))
					addNumberOfLineContribution(contribLinesFromLeft,contribLinesFromRight, fileIndicator,currentLine);				
			}
		}

		allContribs.add(contribLinesFromLeft);
		allContribs.add(contribLinesFromRight);
		return allContribs;
	}

	private  void addMultipleNumberOfLinesContributions(
			ArrayList<String> contribLinesFromLeft,
			ArrayList<String> contribLinesFromRight, String fileIndicator,
			String lineIndicator) {
		String[] lines = lineIndicator.split(",");
		int lowline = Integer.valueOf(lines[0]);
		int highline = Integer.valueOf(lines[1]);
		while(lowline <= highline){
			addNumberOfLineContribution(contribLinesFromLeft,contribLinesFromRight, fileIndicator,String.valueOf(lowline));	
			lowline++;
		}
	}

	private  void addNumberOfLineContribution(
			ArrayList<String> contribLinesFromLeft,
			ArrayList<String> contribLinesFromRight, String fileIndicator,
			String line) {
		if(			fileIndicator.equals("1")){
			contribLinesFromLeft.add(line);
		}  else if (fileIndicator.equals("3")) {
			contribLinesFromRight.add(line);
		}
	}

	public String markLineContributions(ArrayList<ArrayList<String>> allContribs, String input) {
		ArrayList<String> contribLinesFromLeft 	= allContribs.get(0);
		ArrayList<String> contribLinesFromRight = allContribs.get(1);
		ArrayList<String> lines = new ArrayList<String>();	

		try {
			BufferedReader buffer 	= new BufferedReader(new StringReader(input));
			String currentLine 		= "";
			while ((currentLine=buffer.readLine())!=null)
				lines.add(currentLine);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if(!contribLinesFromLeft.isEmpty()){
			for(String line : contribLinesFromLeft){
				int lineIndex = (Integer.valueOf(line))-1;
				lines.set(lineIndex, "CONTRIB::LEFT::" + lines.get(lineIndex));
			}
		}

		if(!contribLinesFromRight.isEmpty()){
			for(String line : contribLinesFromRight){
				int lineIndex = (Integer.valueOf(line))-1;
				lines.set(lineIndex, "CONTRIB::RIGHT::" + lines.get(lineIndex));
			}
		}

		StringBuilder builder = new StringBuilder();
		for(String s : lines) {
			builder.append(s+"\n");
		}
		return builder.toString();
	}

	public String merge (File left, File base, File right){
		String result = "";
		try{
			String mergeCmd = "C:/KDiff3/bin/diff3.exe -m -E " + "\"" + left.getPath() + "\"" + " " + "\"" + base.getPath() + "\"" + " " + "\"" + right.getPath() + "\"";
			Runtime runTime = Runtime.getRuntime();
			Process process = runTime.exec(mergeCmd);

			BufferedReader buffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line 	= "";
			while ((line=buffer.readLine())!=null) {
				result += line + "\n";
			}
			process.getInputStream().close();
		} catch(Exception e){
			e.printStackTrace();
		}
		return result;
	}

//	//TESTE CODE
		public static void main(String[] args) {
			try {
	
				File left 	= new File("examples/left/Teste.java");
				File right 	= new File("examples/right/Teste.java");
				File base 	= new File("examples/base/Teste.java");
	
				DiffMerged diffMerged = new DiffMerged();
				ArrayList<ArrayList<String>> linesContributions = diffMerged.findLinesContributions(left, right, base);
				System.out.println(linesContributions);
				String result = diffMerged.merge(left, base, right);
				result = diffMerged.markLineContributions(linesContributions, result);
	
				System.out.println(result);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

//	public static void main(String[] args) {
//		try {
//
//			File left 	= new File("examples/left/Teste.java");
//			File right 	= new File("examples/right/Teste.java");
//			File base 	= new File("examples/base/Teste.java");
//
//			DiffMerged diffMerged = new DiffMerged();
//			ArrayList<ArrayList<String>> linesContributions = diffMerged.findLinesContributionsNonNumeric(left, base, base);
//			String result = diffMerged.merge(left, base, right);
//			result = diffMerged.markLineContributions(linesContributions, result);
//
//			System.out.println(result);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
}
