package cin.ufpe.br.git;

import java.util.ArrayList;

import cin.ufpe.br.util.FileHandlerr;
import cin.ufpe.br.util.MergeConflict;

public final class CheckFileForMineYours {

	public static String checkForMineYours(ArrayList<String> filetext, ArrayList<String> mine, ArrayList<String> yours){
		boolean mineCheck = false;
		boolean yoursCheck = false;
		String resolveWay = "";
	
		for(int i = 0;i < filetext.size();i++){
			if(!mineCheck){
				mineCheck = checkTextEquality(filetext, mine, i);
			}
			if(!yoursCheck){
				yoursCheck = checkTextEquality(filetext, yours, i);				
			}
		}
		if(mineCheck){
			resolveWay = "MINE";
		}
		if(yoursCheck){
			resolveWay = "YOURS";
		}
		if(mineCheck && yoursCheck){
			resolveWay = "BOTH";
		}
		if(!mineCheck && !yoursCheck){
			resolveWay = "NONE";
		}
		return resolveWay;
	}
	
	public static String checkForMineYours(String filetext, MergeConflict mergeConflict){
		String resolveWay 	= "";
	
		String auxfiletext = FileHandlerr.getStringContentIntoSingleLineNoSpacing(filetext);
		String auxleft	   = FileHandlerr.getStringContentIntoSingleLineNoSpacing(mergeConflict.left);
		String auxright    = FileHandlerr.getStringContentIntoSingleLineNoSpacing(mergeConflict.right);
		
		
		if(auxfiletext.contains(auxleft) && auxfiletext.contains(auxright)){
			resolveWay = "BOTH";
		} else if(auxfiletext.contains(auxleft)){
			resolveWay = "MINE";
		} else if(auxfiletext.contains(auxright)){
			resolveWay = "YOURS";
		} else {
			resolveWay = "NONE";
		}
		
		return resolveWay;
	}

	private static boolean checkTextEquality(ArrayList<String> filetext1, ArrayList<String> filetext2, int i){
		boolean check = false;
		if(filetext1.get(i).equals(filetext2.get(0))){
			int j = 1;
			i++;
			check = true;
			while(j < filetext2.size() && check == true){
				if(filetext2.get(j).equals(filetext1.get(i))){
					check = true;
				}else{
					check = false;
				}
				j++;
				i++;
			}
		}
		return check;
	}
}
