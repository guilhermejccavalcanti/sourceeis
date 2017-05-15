package cin.ufpe.br.git;

import java.io.IOException;
import java.util.ArrayList;

import cin.ufpe.br.util.*;

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
		ArrayList<String> fileText = FileHandlerr.readFile("commits\\commits_" + mergeConflict.projectName + ".csv");
		//ArrayList<String> fileText = FileHandlerr.readFile("/home/local/CIN/gjcc/fpfnanalysis/commits/commits_" + mergeConflict.projectName + ".csv");
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

}
