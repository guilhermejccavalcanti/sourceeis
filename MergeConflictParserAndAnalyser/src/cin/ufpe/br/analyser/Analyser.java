package cin.ufpe.br.analyser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import cin.ufpe.br.git.CheckFileForMineYours;
import cin.ufpe.br.git.CheckoutCommit;
import cin.ufpe.br.git.MergeCommitsNumberFinder;
import cin.ufpe.br.parser.LightweightParser;
import cin.ufpe.br.util.ConflictsCollector;
import cin.ufpe.br.util.FileHandlerr;
import cin.ufpe.br.util.LoggingOutputStream;
import cin.ufpe.br.util.MergeConflict;
import cin.ufpe.br.util.ProjectsInfoCollector;
import cin.ufpe.br.util.StdOutErrLevel;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;

public final class Analyser {

	private static String workingDirectory;

	public static void loadProperties(){
		if(new File("config.properties").exists()){
			Properties prop = new Properties();
			InputStream input = null;

			try {
				//load a properties
				input = new FileInputStream("config.properties");
				prop.load(input);

				workingDirectory 	= prop.getProperty("working_directory") + "/projects";
			} catch (IOException ex) {
				ex.printStackTrace();
				System.exit(-1);
			} finally{
				if(input!=null){
					try {
						input.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		} else {
			workingDirectory 	= "E:\\Mestrado\\FPFNAnalysis\\projects\\";
			//workingDirectory 	="/home/local/CIN/gjcc/fpfnanalysis/projects/";
		}
	}

	public static void analyse(List<MergeConflict> conflicts){
		loadProperties();

		findConflictsInvolvingSameSyntacticType(conflicts);

		analyseConflictsResolution(null);

		checkMethodsDependencies();

		analyseRenamingResolution();
	}

	private static void findConflictsInvolvingSameSyntacticType(List<MergeConflict> conflicts) {
		System.out.println("running type analysis...");
		List<String> logEqualTypesConflictsEntries = new ArrayList<String>();
		List<String> logFullUnparsableConflictsEntries = new ArrayList<String>();
		List<String> logPartialUnparsableConflictsEntries = new ArrayList<String>();
		List<String> logOtherConflictsEntries = new ArrayList<String>();
		List<String> logEntireClassConflictsEntries = new ArrayList<String>();

		for(MergeConflict mergeConflict: conflicts){
			if(mergeConflict.rightunableToParse && mergeConflict.leftunableToParse){
				logFullUnparsableConflictsEntries.add((mergeConflict.originFile+";"+mergeConflict.body));
			}else if(mergeConflict.rightunableToParse || mergeConflict.leftunableToParse) {
				logPartialUnparsableConflictsEntries.add((mergeConflict.originFile+";"+mergeConflict.body));
			}else{
				boolean arealltypesequal = false;
				String equalType = "";
				if(!mergeConflict.rightTypes.isEmpty()){
					ArrayList<String> typesstring = new ArrayList<String>(Arrays.asList(mergeConflict.rightTypesString.split("\n")));
					final String t1 = typesstring.get(0);
					arealltypesequal = typesstring.stream().allMatch(s -> s.equals(t1)); 
					equalType = t1;
					if(!mergeConflict.leftTypes.isEmpty()){
						typesstring = new ArrayList<String>(Arrays.asList(mergeConflict.leftTypesString.split("\n")));
						final String t2 = typesstring.get(0);
						arealltypesequal = arealltypesequal && typesstring.stream().allMatch(s -> s.equals(t2));
						equalType = t2;
					}
				} else if(!mergeConflict.leftTypes.isEmpty()){
					ArrayList<String> typesstring = new ArrayList<String>(Arrays.asList(mergeConflict.leftTypesString.split("\n")));
					final String t3 = typesstring.get(0);
					arealltypesequal = typesstring.stream().allMatch(s -> s.equals(t3));
					equalType = t3;
				}

				boolean isEntireClassConflict = Arrays.asList(mergeConflict.rightTypesString.split("\n")).contains("TypeDeclaration") || Arrays.asList(mergeConflict.leftTypesString.split("\n")).contains("TypeDeclaration");
				if(arealltypesequal && !isEntireClassConflict){
					if(!equalType.isEmpty())logEqualTypesConflictsEntries.add(mergeConflict.originFile +";"+equalType);
				} else {
					if(isEntireClassConflict){
						logEntireClassConflictsEntries.add((mergeConflict.originFile+";"+mergeConflict.body));
					} else {
						logOtherConflictsEntries.add((mergeConflict.originFile+";"+mergeConflict.body));
					}
				}			
			}
		}

		logTypeAnalysis(logFullUnparsableConflictsEntries, SyntacticType.UNPARSABLE);
		logTypeAnalysis(logPartialUnparsableConflictsEntries, SyntacticType.PARTIAL);
		logTypeAnalysis(logEqualTypesConflictsEntries, SyntacticType.EQUAL);
		logTypeAnalysis(logOtherConflictsEntries, SyntacticType.OTHER);
		logTypeAnalysis(logEntireClassConflictsEntries, SyntacticType.CLASS);
	}

	private static void analyseConflictsResolution(ArrayList<MergeConflict> renamingconflicts){
		System.out.println("running conflict resolution analysis...");
		ArrayList<MergeConflict> conflicts = null;

		//gathering information
		try {
			if(renamingconflicts == null){
				conflicts = ConflictsCollector.collectNoParser("results/log_type_fullunparsable_conflicts.csv");
				conflicts.addAll(ConflictsCollector.collectNoParser("results/log_type_partialunparsable_conflicts.csv"));
			} else {
				conflicts = renamingconflicts;
			}

			//conflicts equality log
			List<String> logNONEEntries = new ArrayList<String>();
			List<String> logMINEEntries = new ArrayList<String>();
			List<String> logYOURSEntries= new ArrayList<String>();
			List<String> logBOTHEntries = new ArrayList<String>();
			List<String> logGENERALEntries = new ArrayList<String>();

			for(MergeConflict mc : conflicts){
				ProjectsInfoCollector.collect(mc);
				MergeCommitsNumberFinder.find(mc);

				try{
					try{
						(new CheckoutCommit()).checkoutRepositoryCMD(workingDirectory, mc,mc.mergeCommit);
					}catch(Exception e){
						e.printStackTrace();
						continue;
					}

					File mergedfile = new File(workingDirectory + mc.projectName + "\\git\\" + mc.filePath);
					if(renamingconflicts != null && !mergedfile.exists()){ //possibly the file was moved to another directory
						List<String> paths = FileHandlerr.listAllFilesPath(workingDirectory + mc.projectName + "\\git\\");
						for(String path: paths){
							File f = new File(path);
							if(f.getName().equals(mergedfile.getName())){
								mergedfile = f;
								break;
							}
						}
					}
					String mergedFileContent = FileHandlerr.readFile(mergedfile);
					String answer = CheckFileForMineYours.checkForMineYours(mergedFileContent,mc);

					String logentry = mc.originFile+";"+mc.body;
					String general  = mc.originFile + ";" + answer;
					logGENERALEntries.add(general);
					switch(answer){
					case "NONE":
						logNONEEntries.add(logentry);
						break;
					case "MINE":
						logMINEEntries.add(logentry);
						break;
					case "YOURS":
						logYOURSEntries.add(logentry);
						break;
					case "BOTH":
						logBOTHEntries.add(logentry);
						break;
					}
					System.out.println(general);
				}catch(Exception e){}
			}

			if(renamingconflicts == null){
				logConflictsResolutionAnalysis(logNONEEntries,ResolutionType.NONE, false);
				logConflictsResolutionAnalysis(logMINEEntries,ResolutionType.MINE, false);
				logConflictsResolutionAnalysis(logYOURSEntries,ResolutionType.YOURS, false);
				logConflictsResolutionAnalysis(logBOTHEntries,ResolutionType.BOTH, false);
				logConflictsResolutionAnalysis(logGENERALEntries,ResolutionType.GENERAL, false);
			} else {
				logConflictsResolutionAnalysis(logNONEEntries,ResolutionType.NONE, true);
				logConflictsResolutionAnalysis(logMINEEntries,ResolutionType.MINE, true);
				logConflictsResolutionAnalysis(logYOURSEntries,ResolutionType.YOURS, true);
				logConflictsResolutionAnalysis(logBOTHEntries,ResolutionType.BOTH, true);
				logConflictsResolutionAnalysis(logGENERALEntries,ResolutionType.GENERAL, true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void checkMethodsDependencies() {
		System.out.println("running dependency analysis...");
		try{
			ArrayList<MergeConflict> conflicts = ConflictsCollector.collectNoParser("results/log_resolution_yours_conflicts.csv");
			conflicts.addAll(ConflictsCollector.collectNoParser("results/log_resolution_mine_conflicts.csv"));

			//conflicts dependency log
			List<String> logNONEEntries = new ArrayList<String>();
			List<String> logCALLEntries = new ArrayList<String>();
			List<String> logNAMEEntries= new ArrayList<String>();
			List<String> logBODYEntries = new ArrayList<String>();
			List<String> logFIELDEntries = new ArrayList<String>();

			for(MergeConflict mc: conflicts){
				ProjectsInfoCollector.collect(mc);
				MergeCommitsNumberFinder.find(mc);

				System.out.println(mc.originFile);

				boolean similarMethods 	 = false;
				boolean similarNames   	 = false;
				boolean hasCallReference = false;
				boolean hasFieldReference= false;

				//1. get all methods signatures involved in a conflict
				List<String> leftmethods = FileHandlerr.findMethodSignatures(mc.left);
				List<String> rightmethods= FileHandlerr.findMethodSignatures(mc.right);

				//2. get the respective method declaration
				for(String leftsignature : leftmethods){
					(new CheckoutCommit()).checkoutRepositoryCMD(workingDirectory, mc, mc.leftCommit);
					File leftfile = new File(workingDirectory + mc.projectName + "\\git\\" + mc.filePath);
					String commitFileContentLeft = FileHandlerr.readFile(leftfile);
					CompilationUnit parsedleft = LightweightParser.parse(commitFileContentLeft);
					MethodDeclaration leftdecl = LightweightParser.findMethodDeclaration(parsedleft, leftsignature);

					for(String rightsignature : rightmethods){
						if(FileHandlerr.getStringContentIntoSingleLineNoSpacing(leftsignature) != FileHandlerr.getStringContentIntoSingleLineNoSpacing(rightsignature)){
							(new CheckoutCommit()).checkoutRepositoryCMD(workingDirectory,mc, mc.rightCommit);
							File rightfile = new File(workingDirectory + mc.projectName + "\\git\\" + mc.filePath);
							String commitFileContentRight = FileHandlerr.readFile(rightfile);
							CompilationUnit parsedright = LightweightParser.parse(commitFileContentRight);
							MethodDeclaration rightdecl = LightweightParser.findMethodDeclaration(parsedright, rightsignature);

							if(leftdecl != null && rightdecl != null){
								//3. compare methods body similarity
								String leftbody = leftdecl.getBody().toString();
								String rightbody = rightdecl.getBody().toString();
								double bodysimilarity = FileHandlerr.computeStringSimilarity(leftbody, rightbody);
								if(bodysimilarity > 0.80 && bodysimilarity < 1.0){
									similarMethods = true;
								}

								//4. compare methods name similarity 
								String leftname = leftdecl.getName().toString();
								String rightname = rightdecl.getName().toString();
								double namesimilarity = FileHandlerr.computeStringSimilarity(leftname, rightname);
								//if(namesimilarity > 0.80)similarNames = true;
								if(namesimilarity > 0.80 && namesimilarity < 1.0){
									similarNames = true;
								}

								//5. check if methods refer each other
								List<MethodCallExpr> leftcalls = leftdecl.getNodesByType(MethodCallExpr.class);
								for(MethodCallExpr leftcall: leftcalls){
									if(leftcall.getNameAsString().equals(rightname)){
										hasCallReference= true;break;}
								}
								if(!hasCallReference){
									List<MethodCallExpr> rightcalls = rightdecl.getNodesByType(MethodCallExpr.class);
									for(MethodCallExpr rightcall: rightcalls){
										if(rightcall.getNameAsString().equals(leftname)){
											hasCallReference= true;break;}
									}
								}

								//6. check if methods access same field
								List<FieldAccessExpr> leftaccesss = leftdecl.getNodesByType(FieldAccessExpr.class);
								List<FieldAccessExpr> rightaccesss= rightdecl.getNodesByType(FieldAccessExpr.class);
								for(FieldAccessExpr leftaccess : leftaccesss){
									for(FieldAccessExpr rightaccess: rightaccesss){
										if(leftaccess.getNameAsString().equals(rightaccess.getNameAsString())){
											hasFieldReference= true;break;					
										}
									}
								}
							}
						}
					}
				}

				String logentry = mc.originFile+";"+mc.body;
				if(similarMethods){logBODYEntries.add(logentry);}
				if(similarNames){logNAMEEntries.add(logentry);}
				if(hasCallReference){logCALLEntries.add(logentry);}
				if(hasFieldReference){logFIELDEntries.add(logentry);}
				if(!similarMethods && !similarNames && !hasCallReference && !hasFieldReference){logNONEEntries.add(logentry);}
			}

			logConflictsDependencyAnalysis(logBODYEntries, DependencyType.BODY);
			logConflictsDependencyAnalysis(logNAMEEntries, DependencyType.NAME);
			logConflictsDependencyAnalysis(logCALLEntries, DependencyType.CALL);
			logConflictsDependencyAnalysis(logNONEEntries, DependencyType.NONE);
			logConflictsDependencyAnalysis(logFIELDEntries,DependencyType.FIELD);

		}catch(Exception e){
			e.printStackTrace();

		}
	}

	private static void tryFixUnparsableConflicts(){
		System.out.println("running fix unparsable analysis...");
		ArrayList<MergeConflict> conflicts 	= new ArrayList<MergeConflict>();
		ArrayList<MergeConflict> pconflicts = new ArrayList<MergeConflict>();
		try {
			conflicts = ConflictsCollector.collect("results/log_ttype_fullunparsable_conflicts.csv");
			conflicts.addAll(ConflictsCollector.collect("results/log_ttype_partialunparsable_conflicts.csv"));
			//System.out.println(conflicts.size());
			for(MergeConflict mc : conflicts){
				if(mc.leftunableToParse && mc.rightunableToParse){
					mc.right = mc.right.concat("}");
					mc.left  = mc.left.concat("}");
				} else if(mc.leftunableToParse){
					mc.left  = mc.left.concat("}");
				} else if(mc.rightunableToParse){
					mc.left  = mc.left.concat("}");
				}
				mc.rightunableToParse 	= false;
				mc.leftunableToParse 	= false;
				ConflictsCollector.tryAndCallParser(mc);
				pconflicts.add(mc);
			}
			findConflictsInvolvingSameSyntacticType(pconflicts);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void analyseRenamingResolution(){
		System.out.println("running renaming analysis...");
		try{
			ArrayList<MergeConflict> conflicts = ConflictsCollector.collectNoParser("results/log_ssmerge_different_conflicts_ssmerge.csv");
			ArrayList<MergeConflict> updatedconflicts = new ArrayList<MergeConflict>();

			for(MergeConflict mc: conflicts){
				//System.out.println(mc.originFile);

				ProjectsInfoCollector.collect(mc);
				MergeCommitsNumberFinder.find(mc);

				//1. get the possible renamed methods
				List<String> leftmethods = FileHandlerr.findMethodSignatures(mc.left);
				List<String> rightmethods= FileHandlerr.findMethodSignatures(mc.right);

				if(leftmethods.size() == 1 && rightmethods.size() == 0){	// a renaming conflict is constituted by only one method
					try{
						(new CheckoutCommit()).checkoutRepositoryCMD(workingDirectory, mc, mc.leftCommit);
					}catch(Exception e){
						e.printStackTrace();
						continue;
					}
					File leftfile = new File(workingDirectory + mc.projectName + "\\git\\" + mc.filePath);
					String commitedFileContentLeft = FileHandlerr.readFile(leftfile);
					CompilationUnit parsedleft = LightweightParser.parse(commitedFileContentLeft);
					String methodsignature = leftmethods.get(0);
					MethodDeclaration leftdecl = LightweightParser.findMethodDeclaration(parsedleft, methodsignature);
					if(leftdecl != null){
						//2. try to find the renamed method
						try{
							(new CheckoutCommit()).checkoutRepositoryCMD(workingDirectory, mc, mc.rightCommit);
						}catch(Exception e){
							e.printStackTrace();
							continue;
						}
						File rightfile = new File(workingDirectory + mc.projectName + "\\git\\" + mc.filePath);
						if(!rightfile.exists()){ //possibly the file was moved to another directory
							List<String> paths = FileHandlerr.listAllFilesPath(workingDirectory + mc.projectName + "\\git\\");
							for(String path: paths){
								File f = new File(path);
								if(f.getName().equals(rightfile.getName())){
									rightfile = f;
									break;
								}
							}
						}
						if(rightfile.exists()){
							String commitedFileContentRight = FileHandlerr.readFile(rightfile);
							CompilationUnit parsedright = LightweightParser.parse(commitedFileContentRight);
							List<MethodDeclaration> rightdecls = parsedright.getNodesByType(MethodDeclaration.class);
							for(MethodDeclaration rightdecl : rightdecls){
								String leftBody = leftdecl.toString();
								String rightBody= rightdecl.toString();
								double similarity = FileHandlerr.computeStringSimilarity(leftBody, rightBody);
								if(similarity > 0.70){
									MergeConflict updatedmergeconflict = updateMergeConflict(mc, leftBody, rightBody);
									updatedconflicts.add(updatedmergeconflict);
									break;

								}
							}
						} else { //it is a deletion
							MergeConflict updatedmergeconflict = updateMergeConflict(mc, mc.left, "");
							updatedconflicts.add(updatedmergeconflict);
						}
					}
				}
				else if(leftmethods.size() == 0 && rightmethods.size() == 1){	
					try{
						(new CheckoutCommit()).checkoutRepositoryCMD(workingDirectory, mc, mc.rightCommit);
					}catch(Exception e){
						e.printStackTrace();
						continue;
					}
					File rightfile = new File(workingDirectory + mc.projectName + "\\git\\" + mc.filePath);
					String commitedFileContentright = FileHandlerr.readFile(rightfile);
					CompilationUnit parsedright = LightweightParser.parse(commitedFileContentright);
					String methodsignature = rightmethods.get(0);
					MethodDeclaration rightdecl = LightweightParser.findMethodDeclaration(parsedright, methodsignature);
					if(rightdecl != null){
						//2. try to find the renamed method
						try{
							(new CheckoutCommit()).checkoutRepositoryCMD(workingDirectory, mc, mc.leftCommit);
						}catch(Exception e){
							e.printStackTrace();
							continue;
						}
						File leftfile = new File(workingDirectory + mc.projectName + "\\git\\" + mc.filePath);
						if(!leftfile.exists()){ //possibly the file was moved to another directory
							List<String> paths = FileHandlerr.listAllFilesPath(workingDirectory + mc.projectName + "\\git\\");
							for(String path: paths){
								File f = new File(path);
								if(f.getName().equals(leftfile.getName())){
									leftfile = f;
									break;
								}
							}
						}
						if(leftfile.exists()){
							String commitedFileContentleft = FileHandlerr.readFile(leftfile);
							CompilationUnit parsedleft = LightweightParser.parse(commitedFileContentleft);
							List<MethodDeclaration> leftdecls = parsedleft.getNodesByType(MethodDeclaration.class);
							for(MethodDeclaration leftdecl : leftdecls){
								String leftBody = leftdecl.toString();
								String rightBody= rightdecl.toString();
								double similarity = FileHandlerr.computeStringSimilarity(leftBody, rightBody);
								if(similarity > 0.70){
									MergeConflict updatedmergeconflict = updateMergeConflict(mc, leftBody, rightBody);
									updatedconflicts.add(updatedmergeconflict);
									break;
								}
							}
						} else { //it is a deletion
							MergeConflict updatedmergeconflict = updateMergeConflict(mc, "", mc.right);
							updatedconflicts.add(updatedmergeconflict);
						}
					}
				}
			}

			//System.out.println(conflicts.size());
			//System.out.println(updatedconflicts.size());

			if(!updatedconflicts.isEmpty()){
				analyseConflictsResolution(updatedconflicts);
			}
		}catch(Exception e){e.printStackTrace();}
	}

	private static MergeConflict updateMergeConflict(MergeConflict toBeUpdated,	String updatedLeftBody, String updatedRightBody) {
		MergeConflict updatedmergeconflict = new MergeConflict(updatedLeftBody, updatedRightBody);
		updatedmergeconflict.originFile = toBeUpdated.originFile;
		updatedmergeconflict.filePath   = toBeUpdated.filePath;
		updatedmergeconflict.projectName= toBeUpdated.projectName;
		updatedmergeconflict.mergeCommit= toBeUpdated.mergeCommit;
		updatedmergeconflict.leftCommit = toBeUpdated.leftCommit;
		updatedmergeconflict.rightCommit= toBeUpdated.rightCommit;
		updatedmergeconflict.baseCommit = toBeUpdated.baseCommit;
		return updatedmergeconflict;
	}

	// aux for logging
	private static enum SyntacticType{
		UNPARSABLE,
		PARTIAL,
		EQUAL,
		CLASS,
		OTHER
	}
	// aux for logging
	private static enum ResolutionType{
		NONE,
		BOTH,
		MINE,
		YOURS,
		GENERAL
	}
	// aux for logging
	private static enum DependencyType{
		NONE,
		CALL,
		NAME,
		BODY,
		FIELD
	}

	private static void logTypeAnalysis(List<String> logentries, SyntacticType type) {
		try {
			File file = null; 
			switch(type){
			case UNPARSABLE:
				file = new File("results/log_type_fullunparsable_conflicts.csv" );
				break;
			case PARTIAL:
				file = new File("results/log_type_partialunparsable_conflicts.csv" );
				break;
			case EQUAL:
				file = new File("results/log_type_equaltypes_conflicts.csv");
				break;
			case CLASS:
				file = new File("results/log_type_entireclass_conflicts.csv");
				break;
			case OTHER:
				file = new File("results/log_type_other_conflicts.csv" );
				break;
			}

			if(!file.exists()){file.createNewFile();}
			PrintWriter pw = new PrintWriter(new FileOutputStream(file, true), true);
			try{
				for(String entry : logentries){pw.append(entry + "\n");}
				pw.flush();pw.close();
			}finally{
				try{pw.close();}
				catch(Exception e){e.printStackTrace();}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void logConflictsResolutionAnalysis(List<String> logentries, ResolutionType type, boolean isrenaming) {
		try {
			File file = null; 
			switch(type){
			case NONE:
				file =(isrenaming)? new File("results/log_renaming_resolution_none_conflicts.csv" ) 	: new File("results/log_resolution_none_conflicts.csv" );
				break;
			case MINE:
				file = (isrenaming)?new File("results/log_renaming_resolution_mine_conflicts.csv" )		: new File("results/log_resolution_mine_conflicts.csv" );
				break;
			case YOURS:
				file = (isrenaming)?new File("results/log_renaming_resolution_yours_conflicts.csv" ) 	: new File("results/log_resolution_yours_conflicts.csv" );
				break;
			case BOTH:
				file = (isrenaming)?new File("results/log_renaming_resolution_both_conflicts.csv" ) 	: new File("results/log_resolution_both_conflicts.csv" );
				break;
			case GENERAL:
				file = (isrenaming)?new File("results/log_renaming_resolution_general_conflicts.csv" ) 	: new File("results/log_resolution_general_conflicts.csv" );
				break;
			}
			if(!file.exists()){file.createNewFile();}
			PrintWriter pw = new PrintWriter(new FileOutputStream(file, true), true);
			try{
				for(String entry : logentries){pw.append(entry + "\n");}
				pw.flush();pw.close();
			}finally{
				try{pw.close();}
				catch(Exception e){e.printStackTrace();}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void logConflictsDependencyAnalysis(List<String> logentries, DependencyType type) {
		List<String> noduplicateentries = new ArrayList<>(new LinkedHashSet<>(logentries));
		try {
			File file = null; 
			switch(type){
			case NONE:
				file = new File("results/log_dependency_conflicts_none.csv" );
				break;
			case CALL:
				file = new File("results/log_dependency_conflicts_call.csv" );
				break;
			case NAME:
				file = new File("results/log_dependency_conflicts_name.csv" );
				break;
			case BODY:
				file = new File("results/log_dependency_conflicts_body.csv" );
				break;
			case FIELD:
				file = new File("results/log_dependency_conflicts_field.csv" );
				break;
			}
			if(!file.exists()){file.createNewFile();}
			PrintWriter pw = new PrintWriter(new FileOutputStream(file, true), true);
			try{
				for(String entry : noduplicateentries){pw.append(entry + "\n");}
				pw.flush();pw.close();
			}finally{
				try{pw.close();}
				catch(Exception e){e.printStackTrace();}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void logger() throws Exception {
		// initialize logging to go to rolling log file
		LogManager logManager = LogManager.getLogManager();
		logManager.reset();

		// log file max size 10K, 3 rolling files, append-on-open
		Handler fileHandler = new FileHandler("logerr", 10000000, 3, true);
		fileHandler.setFormatter(new SimpleFormatter());
		Logger.getLogger("").addHandler(fileHandler);

		// preserve old stdout/stderr streams in case they might be useful
		PrintStream stdout = System.out;
		PrintStream stderr = System.err;

		// now rebind stdout/stderr to logger
		Logger logger;
		LoggingOutputStream los;

		logger = Logger.getLogger("stderr");
		los	= new LoggingOutputStream(logger, StdOutErrLevel.STDERR);
		System.setErr(new PrintStream(los, true));
	}

	public static void main(String[] args) {
		try{
			logger();

			long t0 = System.currentTimeMillis();

			analyseRenamingResolution();

			long tf = System.currentTimeMillis();
			long mergeTime = ((tf-t0)/60000);
			System.out.println("analysis time: " + mergeTime + " minutes");
		}catch(Exception e){
			e.printStackTrace();
		}


		/*		try{
			ArrayList<MergeConflict> conflicts = ConflictsCollector.collectNoParser("log_dependency_conflicts_body.csv");
			conflicts.addAll(ConflictsCollector.collectNoParser("log_dependency_conflicts_call.csv"));
			conflicts.addAll(ConflictsCollector.collectNoParser("log_dependency_conflicts_field.csv"));
			conflicts.addAll(ConflictsCollector.collectNoParser("log_dependency_conflicts_name.csv"));
			System.out.println(conflicts.size());

			List<String> unique = new ArrayList<String>();
			for(MergeConflict mc: conflicts){
				String logentry = mc.originFile+";"+mc.body;
				unique.add(logentry);
			}
			List<String> noduplicateentries = new ArrayList<>(new LinkedHashSet<>(unique));
			System.out.println(noduplicateentries.size());


		}catch(Exception e){
			e.printStackTrace();
		}*/
	}
}