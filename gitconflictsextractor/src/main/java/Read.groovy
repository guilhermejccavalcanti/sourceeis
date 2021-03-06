import java.util.ArrayList;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class Read {

	private ArrayList<MergeCommit> listMergeCommit
	private ArrayList<Project> listProject
	private String csvCommitsFile
	private String csvProjectsFile

	public Read(fileName, boolean withGitMiner){
		this.listMergeCommit = new ArrayList<MergeCommit>()
		this.listProject 	 = new ArrayList<Project>()
		this.csvProjectsFile = fileName
		this.readProjectsCSV(withGitMiner)
	}

	def setCsvCommitsFile(fileName){
		this.csvCommitsFile = fileName
	}

	def readCommitsCSV() {
		BufferedReader br = null
		String line 	  = ""
		String csvSplitBy = ","
		try {
			br = new BufferedReader(new FileReader(this.csvCommitsFile))
			br.readLine()
			while ((line = br.readLine()) != null) {
				String[] shas = line.split(csvSplitBy)

				def mergeCommit = new MergeCommit()
				mergeCommit.sha 	= shas[0]
				mergeCommit.parent1 = shas[1]
				mergeCommit.parent2 = shas[2]

				this.listMergeCommit.add(mergeCommit)

				println ("SHA's [MergeCommit= " + mergeCommit.sha 	+ " , Parent1=" + mergeCommit.parent1 + " , Parent2=" + mergeCommit.parent2 +  "]")
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace()
		} catch (IOException e) {
			e.printStackTrace()
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace()
				}
			}
		}
	}

	def readProjectsCSV(boolean withGitMiner) {
		BufferedReader br = null
		BufferedReader reader;
		String line = ""
		String csvSplitBy = ","
		try {
			reader = Files.newBufferedReader(Paths.get(this.csvProjectsFile));
			List<String> listProjects = reader.lines().collect(Collectors.toList());
			for(int i = 1;i<listProjects.size();i++){
				line = listProjects.get(i);
				if(!line.isEmpty()){
					String[] info = line.split(csvSplitBy)
					def project 	= new Project()
					project.name 	= info[0]
					project.url 	= info[1]

					if(!withGitMiner && info.length == 4) {
						project.miningSinceDate = info[2]
						project.miningUntilDate = info[3]
					} else if (!withGitMiner && info.length == 3) {
						project.miningUntilDate = info[2]
					} else if(withGitMiner){
						project.graph 	= info[2]
					}

					this.listProject.add(project)
					if(withGitMiner)
						println ("PROJECT [Name= " + project.name + " , Url=" + project.url + " , Graph dir=" + project.graph +  "]")
					else
						println ("PROJECT [Name= " + project.name + " , Url=" + project.url +  "]")
				}
			}

			/*br = new BufferedReader(new FileReader(this.csvProjectsFile))
			 br.readLine()
			 while (!(line = br.readLine()).isEmpty()) {
			 String[] info = line.split(csvSplitBy)
			 def project 	= new Project()
			 project.name 	= info[0]
			 project.url 	= info[1]
			 if(!withGitMiner && info.length == 4) {
			 project.miningSinceDate = info[2]
			 project.miningUntilDate = info[3]
			 } else if (!withGitMiner && info.length == 3) {
			 project.miningUntilDate = info[2]
			 } else if(withGitMiner){
			 project.graph 	= info[2]
			 }
			 this.listProject.add(project)
			 if(withGitMiner)
			 println ("PROJECT [Name= " + project.name + " , Url=" + project.url + " , Graph dir=" + project.graph +  "]")
			 else
			 println ("PROJECT [Name= " + project.name + " , Url=" + project.url +  "]")
			 }*/

		} catch (FileNotFoundException e) {
			e.printStackTrace()
		} catch (IOException e) {
			e.printStackTrace()
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace()
				}
			}
		}
	}

	def ArrayList<MergeCommit> getMergeCommitsList(){
		return this.listMergeCommit
	}

	def ArrayList<Project> getProjects(){
		return this.listProject
	}

	//	static void main(args) {
	//		Reader obj = new Reader("commits.csv")
	//		obj.readCSV()
	//	}
}
