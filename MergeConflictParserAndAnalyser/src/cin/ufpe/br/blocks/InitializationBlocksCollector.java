package cin.ufpe.br.blocks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public final class InitializationBlocksCollector {

	public static ArrayList<MatchedBlocks> collectMineTheirsBlocksFromText(String filename) throws IOException{
		ArrayList<MatchedBlocks> allblocks = new ArrayList<>();

		try {
			File file = new File(filename);
			Scanner scanner = new Scanner(file);
			scanner.useDelimiter("\r\n");

			ArrayList<String> mineBlock  = new ArrayList<String>();
			ArrayList<String> yoursBlock = new ArrayList<String>();
			String revisionsFile = "";
			String originFile = ""; 
			int mineIndex = 0;
			int yoursIndex = 0;
			boolean ismine = false;
			boolean isyours = false;

			while (scanner.hasNext()) {
				String line = scanner.next();
				if(checkForRevisions(line)){
					if(!mineBlock.isEmpty() && !yoursBlock.isEmpty()){
						MatchedBlocks blocks = new MatchedBlocks( String.join("\n",mineBlock),  String.join("\n",yoursBlock), originFile, revisionsFile);
						allblocks.add(blocks);

						//reseting
						mineBlock  = new ArrayList<String>();
						yoursBlock = new ArrayList<String>();
						ismine 	= false;
						isyours = false;
						mineIndex 	= 0;
						yoursIndex 	= 0;
					}

					revisionsFile = line.split(";")[0];
					originFile = line.split(";")[1];

				}else if(checkForStatic(line) && !ismine){
					ismine 	= true;
					isyours = false;

				}else if(checkForStatic(line) && !isyours){
					isyours = true;
					ismine 	= false;
				}

				if(ismine){
					mineBlock.add(mineIndex,line);
					mineIndex++;
				} else if(isyours) {
					yoursBlock.add(yoursIndex,line);
					yoursIndex++;
				}

			}
			//the last entry from the log
			if(!mineBlock.isEmpty() && !yoursBlock.isEmpty()){
				MatchedBlocks blocks = new MatchedBlocks( String.join("\n",mineBlock),  String.join("\n",yoursBlock), originFile, revisionsFile);
				allblocks.add(blocks);
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return allblocks;
	}

	private static Boolean checkForStatic(String text){
		Boolean foundYoursEnd = false;
		if(text.contains("static")){
			foundYoursEnd = true;
		}
		return foundYoursEnd;
	}

	private static Boolean checkForRevisions(String text){
		Boolean foundYoursEnd = false;
		if(text.contains(".revisions")){
			foundYoursEnd = true;
		}
		return foundYoursEnd;
	}

	public static void main(String[] args) {
		try {
			ArrayList<MatchedBlocks> b = collectMineTheirsBlocksFromText("test.txt");
			System.out.println(b.size());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
