package cin.ufpe.br.util;

import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.ASTParser;

import cin.ufpe.br.parser.Parsed;
import cin.ufpe.br.parser.Parser;

public final class ConflictsCollector {

	public static ArrayList<MergeConflict> collect(String filename) throws IOException{
		ArrayList<ConflictPair> pairs  = collectMineTheirsFromText(filename);
		ArrayList<MergeConflict> conflicts = new ArrayList<MergeConflict>();
		int index = 0;
		while (index < pairs.size()) {
			ConflictPair pair = pairs.get(index);
			MergeConflict mc = new MergeConflict(String.join("\n",pair.getMine())+"\n", String.join("\n",pair.getYours())+"\n");
			mc.originFile = pair.getOriginFile();
			tryAndCallParser(mc);
			conflicts.add(mc);
			index++;
		}
		return conflicts;
	}
	
	public static ArrayList<MergeConflict> collectNoParser(String filename) throws IOException{
		ArrayList<ConflictPair> pairs  = collectMineTheirsFromText(filename);
		ArrayList<MergeConflict> conflicts = new ArrayList<MergeConflict>();
		int index = 0;
		while (index < pairs.size()) {
			ConflictPair pair = pairs.get(index);
			MergeConflict mc = new MergeConflict(String.join("\n",pair.getMine())+"\n", String.join("\n",pair.getYours())+"\n");
			mc.originFile = pair.getOriginFile();
			conflicts.add(mc);
			index++;
		}
		return conflicts;
	}

	@SuppressWarnings("unchecked")
	public static void tryAndCallParser(MergeConflict mc) {
		Parser parser = new Parser();
		if(!FileHandlerr.getStringContentIntoSingleLineNoSpacing(mc.right).isEmpty()){
			Parsed rightparsed = null;
			//trying different types of parsing
			try{
				rightparsed = parser.parse(mc.right, ASTParser.K_COMPILATION_UNIT);
			} catch(Exception e1){
				try{
					rightparsed = parser.parse(mc.right, ASTParser.K_CLASS_BODY_DECLARATIONS);
				}catch(Exception e2){
					mc.rightunableToParse = true;
				}
			}
			if(null!=rightparsed){
				mc.rightTypes = rightparsed.printer.getTypes();
				mc.rightTypesString = rightparsed.printer.getResult();
			}
		}

		if(!FileHandlerr.getStringContentIntoSingleLineNoSpacing(mc.left).isEmpty()){
			Parsed leftparsed = null;
			try{
				leftparsed = parser.parse(mc.left, ASTParser.K_COMPILATION_UNIT);
			} catch(Exception e1){
				try{
					leftparsed = parser.parse(mc.left, ASTParser.K_CLASS_BODY_DECLARATIONS);
				}catch(Exception e2){
					mc.leftunableToParse = true;
				}
			}
			if(null!=leftparsed){
				mc.leftTypes = leftparsed.printer.getTypes();
				mc.leftTypesString = leftparsed.printer.getResult();
			}
		}
	}

	public static ArrayList<ConflictPair> collectMineTheirsFromText(String filename) throws IOException{
		ArrayList<String> fileText = FileHandlerr.readFile(filename);
		ArrayList<ConflictPair> allConflicts = new ArrayList<>();
		int index = 0;

		while(index < fileText.size()){
			if(checkForMine(fileText.get(index))){
				String originFile = fileText.get(index).split(";")[0];
				ArrayList<String> mine  = new ArrayList<String>();
				ArrayList<String> yours = new ArrayList<String>();

				if(checkForEOF(fileText.size(), index + 1)){
					index += 1;
					int mineIndex = 0;
					while(!checkForConflictHeadline(fileText.get(index))){
						mine.add(mineIndex,fileText.get(index));
						mineIndex++;
						index++;
					}
					/* Jumps the "======" */
					index++;

					if(checkForEOF(fileText.size(), index + 1)){
						int yoursIndex = 0;
						while(!checkForYoursEnd(fileText.get(index))){
							yours.add(yoursIndex,fileText.get(index));
							yoursIndex++;
							index++;
						}
					}
					ConflictPair ConflictPair = new ConflictPair(mine, yours,originFile);
					allConflicts.add(ConflictPair);
				}
			}
			index++;
		}
		return allConflicts;
	}

	private static Boolean checkForConflictHeadline(String text){
		Boolean foundConflict = false;
		if(text.contains("=======")){
			foundConflict = true;
		}
		return foundConflict;
	}

	private static Boolean checkForEOF(int numberOfLines, int index){
		Boolean eof = false;
		if(index < numberOfLines){
			eof = true;
		}
		return eof;
	}

	private static Boolean checkForYoursEnd(String text){
		Boolean foundYoursEnd = false;
		if(text.contains(">>>>>>> YOURS")){
			foundYoursEnd = true;
		}
		return foundYoursEnd;
	}

	private static Boolean checkForMine(String text){
		Boolean foundMine = false;
		if(text.contains("<<<<<<< MINE")){
			foundMine = true;
		}
		return foundMine;
	}
}
