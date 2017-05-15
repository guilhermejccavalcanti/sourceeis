package cin.ufpe.br.util;

import java.util.ArrayList;

public class ConflictPair{
	
	private ArrayList<String> mine;
	private ArrayList<String> yours;
	private String originFile;
	
	public ConflictPair(ArrayList<String> mine, ArrayList<String> yours){
		this.mine = mine;
		this.yours = yours;
	}
	
	public ConflictPair(ArrayList<String> mine, ArrayList<String> yours, String originFile){
		this.mine = mine;
		this.yours = yours;
		this.originFile = originFile;
	}

	public ArrayList<String> getMine() {
		return mine;
	}

	public void setMine(ArrayList<String> mine) {
		this.mine = mine;
	}

	public ArrayList<String> getYours() {
		return yours;
	}

	public void setYours(ArrayList<String> yours) {
		this.yours = yours;
	}

	public String getOriginFile() {
		return originFile;
	}

	public void setOriginFile(String originFile) {
		this.originFile = originFile;
	}
	
}
