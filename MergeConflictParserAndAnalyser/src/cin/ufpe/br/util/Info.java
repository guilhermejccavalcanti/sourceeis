package cin.ufpe.br.util;

public class Info {

	private String projectName;
	private String mine;
	private String yours;
	private String filepath;
	
	public Info(String projectName, String mine, String yours, String filepath){
		this.projectName = projectName;
		this.mine = mine;
		this.yours = yours;
		this.filepath = filepath;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getMine() {
		return mine;
	}

	public void setMine(String mine) {
		this.mine = mine;
	}

	public String getYours() {
		return yours;
	}

	public void setYours(String yours) {
		this.yours = yours;
	}

	public String getfilepath() {
		return filepath;
	}

	public void setfilepath(String filepath) {
		this.filepath = filepath;
	}
	
}
