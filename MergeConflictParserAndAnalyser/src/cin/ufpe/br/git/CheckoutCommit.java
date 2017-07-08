package cin.ufpe.br.git;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;

import cin.ufpe.br.util.Info;
import cin.ufpe.br.util.MergeConflict;

public class CheckoutCommit {
	
	public void checkoutRepository(String mergecommitNumber,Info info) throws IOException, RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, CheckoutConflictException, GitAPIException{
		Git git;
		File gitrepo = new File("C:\\Users\\jpms2\\Desktop\\projects\\" + info.getProjectName() + "\\git\\.git");
		git = Git.open(gitrepo);
		git.reset().setRef(mergecommitNumber).setMode(ResetType.HARD).call();
		git.clean().setForce(true).call();
		git.close();
	}

	public void checkoutRepository(MergeConflict mc, String commitNumber) throws Exception {
		Git git;
		File gitrepo = new File("E:\\Mestrado\\FPFNAnalysis\\projects\\" + mc.projectName + "\\git\\.git");
		//File gitrepo = new File("/home/local/CIN/gjcc/fpfnanalysis/projects/" + mc.projectName + "/git/.git");
		git = Git.open(gitrepo);
		git.reset().setRef(commitNumber).setMode(ResetType.HARD).call();
		git.clean().setForce(true).call();
		git.close();
	}
	
	public void checkoutRepositoryCMD(MergeConflict mc, String commitNumber) throws Exception {
		String gitcmd ="git reset --hard " + commitNumber;
		String cmdpath= "E:\\Mestrado\\FPFNAnalysis\\projects\\" + mc.projectName + "\\git";
		//String cmdpath= "/home/local/CIN/gjcc/fpfnanalysis/projects/" + mc.projectName + "/git/";
		Runtime run = Runtime.getRuntime();
		Process pr 	= run.exec(gitcmd,null,new File(cmdpath));
		
		//status
		BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
		String line  	= "";
		String output 	= "";
		while ((line=buf.readLine())!=null) {output+=line;/*System.err.println(line);*/}
		pr.getErrorStream().close();
		pr.getOutputStream().close();
		
		if(output.contains("fatal")) throw new Exception();
	}
	
	public void checkoutRepositoryCMD(String workingDirectory, MergeConflict mc, String commitNumber) throws Exception {
		String gitcmd ="git reset --hard " + commitNumber;
		String cmdpath= workingDirectory + File.separator + mc.projectName + File.separator +"git";
		Runtime run = Runtime.getRuntime();
		Process pr 	= run.exec(gitcmd,null,new File(cmdpath));
		
		//status
		BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
		String line  	= "";
		String output 	= "";
		while ((line=buf.readLine())!=null) {output+=line;/*System.err.println(line);*/}
		pr.getErrorStream().close();
		pr.getOutputStream().close();
		
		if(output.contains("fatal")) throw new Exception();
	}
	
	public void checkoutRepositoryCMD(String workingDirectory, String projectName, String commitNumber) throws Exception {
		String gitcmd ="git reset --hard " + commitNumber;
		String cmdpath= workingDirectory + projectName + "\\git";
		Runtime run = Runtime.getRuntime();
		Process pr 	= run.exec(gitcmd,null,new File(cmdpath));
		
		//status
		BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
		String line  	= "";
		String output 	= "";
		while ((line=buf.readLine())!=null) {output+=line;System.err.println(line);}
		pr.getErrorStream().close();
		pr.getOutputStream().close();
		
		if(output.contains("fatal")) throw new Exception();
	}
}