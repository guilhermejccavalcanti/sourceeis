package cin.ufpe.br.git;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import cin.ufpe.br.blocks.MatchedBlocks;
import cin.ufpe.br.util.FileHandlerr;
import cin.ufpe.br.util.Info;
import cin.ufpe.br.util.MergeConflict;

public final class MergeCommitsNumberFinder {

	public static String findMCNumber(Info info) throws IOException{
		//ArrayList<String> fileText = FileHandlerr.readFile("\\commits\\commits_" + info.getProjectName() + ".csv");
		ArrayList<String> fileText = FileHandlerr.readFile("/home/local/CIN/gjcc/fpfnanalysis/commits/commits_" + info.getProjectName() + ".csv");
		String possibleMineYours;
		String possibleMine;
		String possibleYours;
		String mergeCommit = "";
		for ( String line : fileText){
			possibleMineYours = line.substring(line.indexOf(',') + 1,line.length());
			possibleMine = possibleMineYours.substring(0,possibleMineYours.indexOf(','));
			possibleYours = possibleMineYours.substring(possibleMineYours.indexOf(',') + 1);
			if(possibleMine.startsWith(info.getMine()) && possibleYours.startsWith(info.getYours())){
				mergeCommit = line.substring(0,line.indexOf(','));
				break;
			}
		}
		return mergeCommit;
	}

	public static void find(MergeConflict mergeConflict) throws IOException{
		ArrayList<String> fileText = FileHandlerr.readFile("commits" + File.separator + "commits_" + mergeConflict.projectName + ".csv");
		//ArrayList<String> fileText = FileHandlerr.readFile("/home/local/CIN/gjcc/fpfnanalysis/commits/commits_" + mergeConflict.projectName + ".csv");
		//ArrayList<String> fileText = FileHandlerr.readFile("/home/paper219/Desktop/analysis/demonstration/commits/commits_" + mergeConflict.projectName + ".csv");
		
		String possibleMineYours;
		String possibleMine = "";
		String possibleYours = "";
		String mergeCommit = "";
		for ( String line : fileText){
			possibleMineYours = line.substring(line.indexOf(',') + 1,line.length());
			possibleMine = possibleMineYours.substring(0,possibleMineYours.indexOf(','));
			possibleYours = possibleMineYours.substring(possibleMineYours.indexOf(',') + 1);
			if(possibleMine.startsWith(mergeConflict.leftCommit) && possibleYours.startsWith(mergeConflict.rightCommit)){
				mergeCommit = line.substring(0,line.indexOf(','));
				break;
			}
		}
		mergeConflict.mergeCommit = mergeCommit;
		mergeConflict.leftCommit = possibleMine;
		mergeConflict.rightCommit = possibleYours;
	}

	public static void find(MatchedBlocks block) throws IOException {
		ArrayList<String> fileText = FileHandlerr.readFile("commits\\commits_" + block.projectname + ".csv");
		//ArrayList<String> fileText = FileHandlerr.readFile("/home/local/CIN/gjcc/fpfnanalysis/commits/commits_" + mergeConflict.projectName + ".csv");
		String possibleMineYours;
		String possibleMine = "";
		String possibleYours = "";
		String mergeCommit = "";
		for ( String line : fileText){
			possibleMineYours = line.substring(line.indexOf(',') + 1,line.length());
			possibleMine = possibleMineYours.substring(0,possibleMineYours.indexOf(','));
			possibleYours = possibleMineYours.substring(possibleMineYours.indexOf(',') + 1);
			if(possibleMine.startsWith(block.leftCommit) && possibleYours.startsWith(block.rightCommit)){
				mergeCommit = line.substring(0,line.indexOf(','));
				break;
			}
		}
		block.mergeCommit = mergeCommit;
		block.leftCommit  = possibleMine;
		block.rightCommit = possibleYours;		
	}

	public static void findCommonAncestor(String workingDirectory, MatchedBlocks block) throws IOException {
		File cmdpath = new File(workingDirectory + block.projectname + "\\git\\");
		ProcessBuilder builder =  new ProcessBuilder("git", "merge-base", block.leftCommit, block.rightCommit);
		builder.directory(cmdpath);
		Process process = builder.start();

		BufferedReader buf = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line = "";
		while ((line=buf.readLine())!=null) {
			if(line.length() == 40){ //size of SHA
				block.baseCommit = line;
				break;
			}
		}
		process.getInputStream().close();
	}

}
