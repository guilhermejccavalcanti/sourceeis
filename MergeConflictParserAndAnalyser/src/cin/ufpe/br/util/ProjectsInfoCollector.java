package cin.ufpe.br.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;

import cin.ufpe.br.blocks.MatchedBlocks;

public final class ProjectsInfoCollector {
	
	public static ArrayList<Info> collectInfo(String filepath) throws IOException{
		ArrayList<String> fileText = FileHandlerr.readFile(filepath);
		ArrayList<String> infoText = findInfo(fileText);
		ArrayList<Info> extractedInfos = new ArrayList<Info>();
		
		for(String line : infoText){
			extractedInfos.add(extractInfo(line));
		}
		return extractedInfos;
	}
	
	private static ArrayList<String> findInfo(ArrayList<String> fileText) throws IOException{
		ArrayList<String> allInfos = new ArrayList<String>();
		
		for(String line : fileText){
			if(line.length() != 0){
				if(line.charAt(0) == '/' && !(line.charAt(1) == '/' || line.charAt(1) == '*')){
					allInfos.add(line);
				}				
			}
		}
		return allInfos;
	}
	
	private static Info extractInfo(String rawInfo){
		rawInfo = rawInfo.substring(rawInfo.indexOf("projects") + 9 , rawInfo.length());
		String projectName = rawInfo.substring(0,rawInfo.indexOf('/'));
		rawInfo = rawInfo.substring(rawInfo.indexOf('/') + 1,rawInfo.length());
		rawInfo = rawInfo.substring(rawInfo.indexOf('/') + 1,rawInfo.length());
		
		String mineYours = rawInfo.substring(0,rawInfo.indexOf('/'));
		rawInfo = rawInfo.substring(rawInfo.indexOf('/') + 1,rawInfo.length());
		rawInfo = rawInfo.substring(rawInfo.indexOf('/') + 1,rawInfo.length());
		mineYours = mineYours.substring(mineYours.indexOf('_') + 1,mineYours.length());
		String mine = mineYours.substring(0,mineYours.indexOf('_'));
		String yours = mineYours.substring(mineYours.indexOf('_') + 1,mineYours.length());
		String filepath = rawInfo.substring(0,rawInfo.indexOf("<<<<<<< MINE"));
		if(filepath.endsWith(";")){
			filepath = filepath.substring(0, filepath.length() - 1);
		}
		Info info = new Info(projectName,mine,yours,filepath);
		return info;
	}
	
	public static void collect(MergeConflict mergeConflict){
		//avoiding path problems
		if(mergeConflict.originFile.contains("\\")){
			mergeConflict.originFile = mergeConflict.originFile.replaceAll(Matcher.quoteReplacement("\\"),"/");
		}
		
		String rawInfo = mergeConflict.originFile;
		rawInfo = rawInfo.substring(rawInfo.indexOf("projects") + 9 , rawInfo.length());
		String projectName = rawInfo.substring(0,rawInfo.indexOf('/'));
		rawInfo = rawInfo.substring(rawInfo.indexOf('/') + 1,rawInfo.length());
		rawInfo = rawInfo.substring(rawInfo.indexOf('/') + 1,rawInfo.length());
		
		String mineYours = rawInfo.substring(0,rawInfo.indexOf('/'));
		rawInfo = rawInfo.substring(rawInfo.indexOf('/') + 1,rawInfo.length());
		rawInfo = rawInfo.substring(rawInfo.indexOf('/') + 1,rawInfo.length());
		mineYours = mineYours.substring(mineYours.indexOf('_') + 1,mineYours.length());
		String mineCommit = mineYours.substring(0,mineYours.indexOf('_'));
		String yoursCommit= mineYours.substring(mineYours.indexOf('_') + 1,mineYours.length());
		//String filepath = rawInfo.substring(0,rawInfo.indexOf("<<<<<<< MINE"));
		String filepath = rawInfo;
		if(filepath.endsWith(";")){
			filepath = filepath.substring(0, filepath.length() - 1);
		}
		
		mergeConflict.projectName = projectName;
		mergeConflict.leftCommit = mineCommit;
		mergeConflict.rightCommit = yoursCommit;
		mergeConflict.filePath = filepath;
	}

	public static void collect(MatchedBlocks block) {
		int idx = block.revisionFile.lastIndexOf('/')+1;
		String revision = block.revisionFile.substring(idx, (block.revisionFile.length()));
		block.leftCommit 	=((revision.split("\\.")[0]).split("_")[1]).split("-")[0];
		block.rightCommit	=((revision.split("\\.")[0]).split("_")[1]).split("-")[1];
		block.projectname 	= (block.revisionFile.split("projects")[1]).split("/revisions/")[0];
		block.projectname 	= block.projectname.substring(1,block.projectname.length());
	}
	
}
