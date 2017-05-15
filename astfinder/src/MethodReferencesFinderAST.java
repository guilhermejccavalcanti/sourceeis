

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import util.MergeResult;

public class MethodReferencesFinderAST {

	private MergeResult currentMergeResult;

	public void run(ArrayList<String> listOfRenamers, ArrayList<String> listOfImporterIssues, ArrayList<String> listOfDuplicaterIssues){
		this.lookForCompilantionProblems(listOfImporterIssues, ErrorType.IMPORT_ERROR);
		this.lookForCompilantionProblems(listOfDuplicaterIssues, ErrorType.DUPLICATED_ERROR);
		this.executeOptimizedRenamings(listOfRenamers);
	}

	public void run(MergeResult mergeResult, ArrayList<String> listOfRenamers, ArrayList<String> listOfImporterIssues, ArrayList<String> listOfDuplicaterIssues){
		currentMergeResult = mergeResult;
		this.lookForCompilantionProblems(mergeResult, listOfImporterIssues, ErrorType.IMPORT_ERROR);
		this.lookForCompilantionProblems(mergeResult, listOfDuplicaterIssues, ErrorType.DUPLICATED_ERROR);
		this.executeOptimizedRenamings(listOfRenamers);
	}

	//Entry format "revision;member;renamedMethod"
	public void executeOptimizedRenamings(String csvFile){
		//revision;member;renamed method
		try {
			//grouping entries that share same revision
			Multimap<String, String> groups = ArrayListMultimap.create();
			File revFile = new File(csvFile);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(revFile.getAbsolutePath()))); 
			String line;
			while ((line = bufferedReader.readLine()) != null) {	
				String entry 	 = line;
				String[] columns = entry.split(";");
				String revDir	 = columns[0];
				groups.put(revDir, entry);
			}
			bufferedReader.close();

			//executing entries that share same revision
			ArrayList<String> sharedRevisionEntryList = new ArrayList<String>();
			for(String keyRevDir : groups.keySet()){
				for(String currentEntry : groups.get(keyRevDir)){
					sharedRevisionEntryList.add(currentEntry);
				}
				executeGroupedEntries(sharedRevisionEntryList);
				sharedRevisionEntryList.clear();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public void executeOptimizedRenamings(ArrayList<String> listOfRenames){
		try {
			//grouping entries that share same revision
			Multimap<String, String> groups = ArrayListMultimap.create();
			for(String entry : listOfRenames){
				//entryList.add(entry);
				String[] columns = entry.split(";");
				String revDir= columns[0];
				groups.put(revDir, entry);
			}

			//executing entries that share same revision
			ArrayList<String> sharedRevisionEntryList = new ArrayList<String>();
			for(String keyRevDir : groups.keySet()){
				for(String currentEntry : groups.get(keyRevDir)){
					sharedRevisionEntryList.add(currentEntry);
				}
				executeGroupedEntries(sharedRevisionEntryList);
				sharedRevisionEntryList.clear();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public void parseSingleFile(String csvFile){
		try {
			File revFile = new File(csvFile);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(revFile.getAbsolutePath()))); 
			String line;
			while ((line = bufferedReader.readLine()) != null) {	
				String[] columns = line.split(";");
				String projectDir= columns[0];
				String className = columns[1];
				String renamedMethod = columns[2];

				//Listing the folders, encodings and classpath of the project
				ArrayList<String> folders = new ArrayList<String>();
				listSourceFolders(folders, projectDir);
				folders.add(projectDir);

				String[] sources = new String[folders.size()];
				sources = folders.toArray(sources);

				String[] encodings = fillEncodings(sources.length);

				//FIXME change if needed
				String[] classPaths = null;

				//Recursively looking for .java files and then parsing
				Multimap<String, String> invokers = ArrayListMultimap.create();
				createParserAndFindMethodReferences(sources, encodings, classPaths, new File(projectDir+File.separator+className+".java"), className, renamedMethod);
			}
			bufferedReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//Entry format "revision;member"
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void lookForCompilantionProblems(ArrayList<String> errorFiles, ErrorType errorToCheck){

		if(errorToCheck == ErrorType.IMPORT_ERROR){
			System.out.println("Looking for Ambiguous Type Errors...");	
		} else {
			System.out.println("Looking for Duplicate Errors...");	
		}


		try {
			for(String entry : errorFiles){
				String[] columns = entry.split(";");
				String revisionFile = columns[0];
				String classFile 	= columns[1];
				String projectDir	= revisionFile.split("\\.")[0];

				//Listing the folders, encodings and classpath of the project
				ArrayList<String> folders = new ArrayList<String>();
				listSourceFolders(folders, projectDir);
				folders.add(projectDir);

				String[] sources = new String[folders.size()];
				sources = folders.toArray(sources);

				String[] encodings = fillEncodings(sources.length);

				//FIXME change if needed
				String[] classPaths = null;

				File javaFile = new File(classFile);
				String contents = getFileContents(javaFile.getAbsolutePath());
				int errors = 0;
				if(contents != null){
					// Create the ASTParser which will be a CompilationUnit
					ASTParser parser = ASTParser.newParser(AST.JLS8);
					parser.setKind(ASTParser.K_COMPILATION_UNIT);
					parser.setSource(contents.toCharArray());

					// Parsing
					parser.setEnvironment(classPaths, sources, encodings, true);	
					parser.setBindingsRecovery(true);
					parser.setResolveBindings(true);
					parser.setUnitName(javaFile.getName());
					Map options = JavaCore.getOptions();
					options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8); 
					parser.setCompilerOptions(options);
					CompilationUnit parse = (CompilationUnit) parser.createAST(null);

					//Listing compilation problems
					IProblem[] problems = parse.getProblems();
					if (problems != null && problems.length > 0) {
						//System.out.println("Got {} problems compiling the source file: "+ problems.length);
						for (IProblem problem : problems) {
							//System.out.println("{}" + problem);
							if(errorToCheck 	 	== ErrorType.IMPORT_ERROR){
								if((problem.toString()).toLowerCase().contains("ambiguous"))errors++;
							} else if(errorToCheck 	== ErrorType.DUPLICATED_ERROR){
								if((problem.toString()).toLowerCase().contains("duplicate"))errors++;
							}
						}
					}
					printCompilationProblemsReport(entry,errorToCheck,errors);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void lookForCompilantionProblems(ArrayList<String> errorFiles){
		try {
			for(String entry : errorFiles){
				String[] columns = entry.split(";");
				String revisionFile = columns[0];
				String classFile 	= columns[1];
				String projectDir	= revisionFile.split("\\.")[0];

				//Listing the folders, encodings and classpath of the project
				ArrayList<String> folders = new ArrayList<String>();
				listSourceFolders(folders, projectDir);
				folders.add(projectDir);

				String[] sources = new String[folders.size()];
				sources = folders.toArray(sources);

				String[] encodings = fillEncodings(sources.length);

				//FIXME change if needed
				String[] classPaths = null;

				File javaFile = new File(classFile);
				String contents = getFileContents(javaFile.getAbsolutePath());
				int errors = 0;
				if(contents != null){
					// Create the ASTParser which will be a CompilationUnit
					ASTParser parser = ASTParser.newParser(AST.JLS8);
					parser.setKind(ASTParser.K_COMPILATION_UNIT);
					parser.setSource(contents.toCharArray());

					// Parsing
					parser.setEnvironment(classPaths, sources, encodings, true);	
					parser.setBindingsRecovery(true);
					parser.setResolveBindings(true);
					parser.setUnitName(javaFile.getName());
					Map options = JavaCore.getOptions();
					options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8); 
					parser.setCompilerOptions(options);
					CompilationUnit parse = (CompilationUnit) parser.createAST(null);

					//Listing compilation problems
					IProblem[] problems = parse.getProblems();
					if (problems != null && problems.length > 0) {
						System.out.println("Got {} problems compiling the source file: "+ problems.length);
						for (IProblem problem : problems) {
							System.out.println("{}" + problem);
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//Entry format "revision;member"
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void lookForCompilantionProblems(MergeResult mergeResult, ArrayList<String> errorFiles, ErrorType errorToCheck){

		if(errorToCheck == ErrorType.IMPORT_ERROR){
			System.out.println("Looking for Ambiguous Type Errors...");	
		} else {
			System.out.println("Looking for Duplicate Errors...");	
		}


		try {
			for(String entry : errorFiles){
				String[] columns = entry.split(";");
				String revisionFile = columns[0];
				String classFile 	= columns[1];
				String projectDir	= revisionFile.split("\\.")[0];

				//Listing the folders, encodings and classpath of the project
				ArrayList<String> folders = new ArrayList<String>();
				listSourceFolders(folders, projectDir);
				folders.add(projectDir);

				String[] sources = new String[folders.size()];
				sources = folders.toArray(sources);

				String[] encodings = fillEncodings(sources.length);

				//FIXME change if needed
				String[] classPaths = null;

				File javaFile = new File(classFile);
				String contents = getFileContents(javaFile.getAbsolutePath());
				int errors = 0;
				if(contents != null){
					// Create the ASTParser which will be a CompilationUnit
					ASTParser parser = ASTParser.newParser(AST.JLS8);
					parser.setKind(ASTParser.K_COMPILATION_UNIT);
					parser.setSource(contents.toCharArray());

					// Parsing
					parser.setEnvironment(classPaths, sources, encodings, true);	
					parser.setBindingsRecovery(true);
					parser.setResolveBindings(true);
					parser.setUnitName(javaFile.getName());
					Map options = JavaCore.getOptions();
					options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8); 
					parser.setCompilerOptions(options);
					CompilationUnit parse = (CompilationUnit) parser.createAST(null);

					//Listing compilation problems
					IProblem[] problems = parse.getProblems();
					if (problems != null && problems.length > 0) {
						//System.out.println("Got {} problems compiling the source file: "+ problems.length);
						for (IProblem problem : problems) {
							//System.out.println("{}" + problem);
							if(errorToCheck 	 	== ErrorType.IMPORT_ERROR){
								if((problem.toString()).toLowerCase().contains("ambiguous")){
									errors++;
									logCompitationProblem(entry,problem.toString(),ErrorType.IMPORT_ERROR);
								}
							} else if(errorToCheck 	== ErrorType.DUPLICATED_ERROR){
								if((problem.toString()).toLowerCase().contains("duplicate")){
									errors++;
									logCompitationProblem(entry,problem.toString(),ErrorType.DUPLICATED_ERROR);
								}
							}
						}
					}

					if(errors>0){
						printCompilationProblemsReport(entry,errorToCheck,errors);
					}
				}
			}

			if(errorToCheck == ErrorType.IMPORT_ERROR){
				System.out.println("Looking for Ambiguous Type Errors Done!");	
			} else {
				System.out.println("Looking for Duplicate Errors Done!");	
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void logCompitationProblem(String entry, String compilationProblemMessage, ErrorType errorToCheck) {
		try {
			String[] columns = entry.split(";");
			String revision	 = columns[0];
			String classFile = columns[1];
			if(errorToCheck == ErrorType.IMPORT_ERROR){
				File file = new File( "results/log_parser_import_issue.csv" );
				FileWriter fw = new FileWriter(file, true);
				BufferedWriter bw = new BufferedWriter( fw );
				bw.write(revision+";"+classFile+";"+compilationProblemMessage);
				bw.newLine();
				bw.close();
				fw.close();
			} else if(errorToCheck == ErrorType.DUPLICATED_ERROR){
				File file = new File( "results/log_parser_duplicated_issue.csv" );
				FileWriter fw = new FileWriter(file, true);
				BufferedWriter bw = new BufferedWriter( fw );
				bw.write(revision+";"+classFile+";"+compilationProblemMessage);
				bw.newLine();
				bw.close();
				fw.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void executeGroupedEntries(ArrayList<String> sharedRevisionEntryList) throws IOException {
		String groupEntrySample = sharedRevisionEntryList.get(0);

		String[] columns = groupEntrySample.split(";");
		String revDir= columns[0];

		//Listing the folders, encodings and classpath of the project
		ArrayList<String> folders = new ArrayList<String>();
		listSourceFolders(folders, revDir);
		folders.add(revDir);

		String[] sources = new String[folders.size()];
		sources = folders.toArray(sources);

		String[] encodings = fillEncodings(sources.length);

		//FIXME change if needed
		String[] classPaths = null;

		//Recursively looking for .java files and then parsing
		Multimap<String, String> invokers = ArrayListMultimap.create();
		ArrayList<String> acceptedFiles = preprocess(revDir, sharedRevisionEntryList);
		//callParserOptimized(sources, encodings, classPaths, acceptedFiles, sharedRevisionEntryList, invokers);
		callParserThread(sources, encodings, classPaths, acceptedFiles, sharedRevisionEntryList, invokers);
		printReferencesReportByRenamedMethodOptimized(sharedRevisionEntryList,invokers);
		printReferencesReportByRevision(sharedRevisionEntryList,invokers);		

	}

	private void callParserDefault(String[] sources, String [] encodings, String[] classPaths, String rootDir, String className, String renamedMethod, Multimap<String, String> invokers) throws IOException{
		File directory = new File(rootDir);
		File[] fList = directory.listFiles();
		for (File file : fList){
			if (file.isDirectory()){
				callParserDefault(sources, encodings, classPaths, file.getAbsolutePath(), className, renamedMethod, invokers);
			} else {
				if(FilenameUtils.getExtension(file.getAbsolutePath()).equalsIgnoreCase("java")){
					List<String> invocations = createParserAndFindMethodReferences(sources, encodings, classPaths, file, className, renamedMethod);
					for(String method: invocations){
						invokers.put(file.getAbsolutePath(), method);					}
				}
			}
		}
	}

	private void callParserOptimized(String[] sources, String [] encodings, String[] classPaths, ArrayList<String> acceptedFiles, ArrayList<String> sharedRevisionEntryList, Multimap<String, String> invokers) throws IOException{
		for(int i = 0; i<acceptedFiles.size();i++){
			String filePath = acceptedFiles.get(i);
			File file = new File(filePath);
			//if(FilenameUtils.getExtension(file.getAbsolutePath()).equalsIgnoreCase("java")){
			System.out.println("processing " +(i+1)+"/"+acceptedFiles.size()+": " + file.getName());
			createParserAndFindMethodReferencesOptimized(sources, encodings, classPaths, file, sharedRevisionEntryList, invokers);
			//}
		}
	}

	private void callParserThread(String[] sources, String [] encodings, String[] classPaths, ArrayList<String> acceptedFiles, ArrayList<String> sharedRevisionEntryList, Multimap<String, String> invokers) throws IOException{
		try {
			int numOfThreads = Runtime.getRuntime().availableProcessors();

			BlockingQueue<String> toProcess = new ArrayBlockingQueue<String>(acceptedFiles.size(), false, acceptedFiles);
			ExecutorService executorService = Executors.newFixedThreadPool(numOfThreads);

			for(int i = 0 ; i < numOfThreads ; ++i) {
				ParserRunnable runnerParser = new ParserRunnable(sources, encodings, classPaths, toProcess, sharedRevisionEntryList, invokers,numOfThreads);
				executorService.submit(runnerParser);
			}

			executorService.shutdown();
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked","unused" })
	private List<String> createParserAndFindMethodReferences(String[] sources, String [] encodings, String[] classPaths, File javaFile, String className, String renamedMethod) throws IOException {

		//char[] contents = getFileContents(javaClass);
		String contents = getFileContents(javaFile.getAbsolutePath());

		if(contents != null){

			// Create the ASTParser which will be a CompilationUnit
			ASTParser parser = ASTParser.newParser(AST.JLS8);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setSource(contents.toCharArray());

			// Parsing
			parser.setEnvironment(classPaths, sources, encodings, true);	
			parser.setBindingsRecovery(true);
			parser.setResolveBindings(true);
			parser.setUnitName(javaFile.getName());
			Map options = JavaCore.getOptions();
			options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8); 
			parser.setCompilerOptions(options);
			CompilationUnit parse = (CompilationUnit) parser.createAST(null);

			HashMap<MethodDeclaration, ArrayList<MethodInvocation>> invocationsForMethods =	new HashMap<MethodDeclaration, ArrayList<MethodInvocation>>();
			ArrayList<String> listOfInvocations = new ArrayList<String>();

			List<String> invokers = new ArrayList<String>();

			parse.accept(new ASTVisitor() {
				private MethodDeclaration activeMethod;

				@Override
				public boolean visit(MethodDeclaration node) {
					activeMethod = node;
					return super.visit(node);
				}

				@Override
				public boolean visit(MethodInvocation node) {
					if (invocationsForMethods.get(activeMethod) == null) {
						invocationsForMethods.put(activeMethod, new ArrayList<MethodInvocation>());
					}
					invocationsForMethods.get(activeMethod).add(node);

					IMethodBinding mb = node.resolveMethodBinding();
					if(mb!=null){
						if(mb.getKey()!=null){
							if(!listOfInvocations.contains(mb.getKey())){
								listOfInvocations.add(mb.getKey());
							}

							//SE CONTIVER UMA REFERÊNCIA, INFORME O MÉTODO QUE REFERENCIOU.
							String namespace = (mb.getKey().split("\\."))[0];
							//if(namespace.contains(className)){
							if(simplifyMethodSignature(mb).contains(renamedMethod)){
								invokers.add(activeMethod.toString());
							}
							//}

							System.out.println(mb.toString());
						}
					}
					return super.visit(node);
				}


			});

			// TEST CODE
			//		System.out.println(invocationsForMethods.keySet());
			//		System.out.println("=================");
			//		System.out.println(invocationsForMethods.values());
			//		for(MethodDeclaration me : invocationsForMethods.keySet() ){
			//			System.out.println(me.getName());
			//			System.out.println(me.parameters());
			//			System.out.println(me.getReturnType2());
			//		}
			//		System.out.println("=================");
			//		invocationsForMethods.values().forEach((me)  -> {
			//			me.forEach(m -> 
			//			System.out.println(m.getExpression().toString() +"|"+ m.getName().toString() +"|"+ m.arguments().toString() +"|"+ m.typeArguments().toString())
			//					);
			//		});
			//		System.out.println("=================");
			//		for(ArrayList<MethodInvocation> ar: invocationsForMethods.values() ){
			//			for(MethodInvocation node : ar){
			//				Expression exp = node.getExpression();
			//				ITypeBinding typeBinding = exp.resolveTypeBinding();
			//				//IType type = (IType) typeBinding.getJavaElement();
			//				// System.out.println("Type: " + typeBinding.toString());	
			//				System.out.println("Type: " + typeBinding.getBinaryName());	
			//				System.out.println("TypeMethod " + node.resolveMethodBinding().getMethodDeclaration());
			//				System.out.println("TypeMethod2 " + node.resolveMethodBinding().getKey());
			//			}
			//		}
			//
			//		System.out.println("=================");
			//		for(String e:listOfInvocations){
			//			System.out.println(e);
			//		}
			//
			System.out.println("=================");
			IProblem[] problems = parse.getProblems();
			if (problems != null && problems.length > 0) {
				System.out.println("Got {} problems compiling the source file: "+ problems.length);
				for (IProblem problem : problems) {
					System.out.println("{}" + problem);
				}
			}

			return invokers;
		} else {
			return new ArrayList<String>();
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void createParserAndFindMethodReferencesOptimized(String[] sources, String [] encodings, String[] classPaths, File javaFile, ArrayList<String> sharedRevisionEntryList,  Multimap<String, String> invokers) throws IOException {
		String contents = getFileContents(javaFile.getAbsolutePath());
		if(contents != null){
			// Create the ASTParser which will be a CompilationUnit
			ASTParser parser = ASTParser.newParser(AST.JLS8);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setSource(contents.toCharArray());

			// Parsing
			parser.setEnvironment(classPaths, sources, encodings, true);	
			parser.setBindingsRecovery(true);
			parser.setResolveBindings(true);
			parser.setUnitName(javaFile.getName());
			Map options = JavaCore.getOptions();
			options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8); 
			parser.setCompilerOptions(options);
			CompilationUnit parse = (CompilationUnit) parser.createAST(null);

			HashMap<MethodDeclaration, ArrayList<MethodInvocation>> invocationsForMethods =	new HashMap<MethodDeclaration, ArrayList<MethodInvocation>>();
			
			ArrayList<String> listOfInvocations = new ArrayList<String>();

			parse.accept(new ASTVisitor() {
				private MethodDeclaration activeMethod;

				@Override
				public boolean visit(MethodDeclaration node) {
					activeMethod = node;
					return super.visit(node);
				}
				
				@Override
				public boolean visit(MethodInvocation node) {
					if (invocationsForMethods.get(activeMethod) == null) {
						invocationsForMethods.put(activeMethod, new ArrayList<MethodInvocation>());
					}
					invocationsForMethods.get(activeMethod).add(node);

					IMethodBinding mb = node.resolveMethodBinding();
					if(mb!=null){
						if(mb.getKey()!=null){
							if(!listOfInvocations.contains(mb.getKey())){
								listOfInvocations.add(mb.getKey());
							}
							for(String entry : sharedRevisionEntryList){
								if(entryMatchesRenamedMethod(javaFile, invokers, mb, entry)){
									if(activeMethod!= null){
										invokers.put(entry, javaFile.getName() + " => " + activeMethod.toString());
									} else {
										invokers.put(entry, javaFile.getName() + " => [unknown]");
									}
								}
							}
						}
					}
					return super.visit(node);
				}
				
			});
		} 
	}

	private boolean entryMatchesRenamedMethod(File javaFile, Multimap<String, String> invokers, IMethodBinding methodBinding,  String entry) {
		try{
			String[] columns = entry.split(";");
			String classNamespace = columns[1];
			String renamedMethod =  columns[2];

			String methodBindingClassNamespace = (methodBinding.getKey().split("\\."))[0];

			if(simplifyMethodSignature(methodBinding).contains(renamedMethod)){
				if(classesNamesMatches(methodBindingClassNamespace, classNamespace)){
					return true;
				} else {/*the method belongs to a superclass or interface*/
					ITypeBinding declaringClass   = methodBinding.getDeclaringClass();
					ITypeBinding superClass 	  = declaringClass.getSuperclass();
					ITypeBinding[] interfaceBinds = declaringClass.getInterfaces();

					//[MISSING:FrameDecoder]
					if(superClass.toString().contains("MISSING")){
						String superClassRelaxed = (superClass.toString().split(":")[1]);
						superClassRelaxed 		 = superClassRelaxed.substring(0,superClassRelaxed.length() - 1);
						if(classesNamesMatches(superClassRelaxed, classNamespace)){
							return true;
						}
					} else {
						if(classesNamesMatches(superClass.getKey(), classNamespace)){
							return true;
						} else {
							for(ITypeBinding interfce : interfaceBinds){
								if(interfce.toString().contains("MISSING")){
									String interfaceRelaxed  = (interfce.toString().split(":")[1]);
									interfaceRelaxed 		 = interfaceRelaxed.substring(0,interfaceRelaxed.length() - 1);
									if(classesNamesMatches(interfaceRelaxed, classNamespace)){
										return true;
									}
								}else {
									if(classesNamesMatches(interfce.getKey(), classNamespace)){
										return true;
									}
								}
							}
						}
					}
					return false;
				}
			}else {
				return false;
			}
		}catch (NullPointerException e){
			return false;
		}
	}

	private boolean classesNamesMatches(String className1, String className2){
		className1 = className1.replaceAll(";", "");
		className1 = className1.replace("/", "\\");
		if(className1.startsWith("L"))className1 = className1.replaceFirst("L","");
		return (className1.contains(className2) || className2.contains(className1));
	}

	private String getFileContents(String filePath) throws IOException {
		StringBuilder fileData = new StringBuilder(1000);
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		char[] buf = new char[10];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
			buf = new char[1024];
		}
		reader.close();
		return  fileData.toString();	
	}

	private void listSourceFolders(ArrayList<String> folders, String rootDir){
		File directory = new File(rootDir);
		File[] fList = directory.listFiles();
		for (File file : fList){
			if (file.isDirectory()){
				folders.add(file.getAbsolutePath());
				listSourceFolders(folders, file.getAbsolutePath());
			}
		}
	}

	private String[] fillEncodings(int amount){
		String[] result = new String[amount];
		for(int i = 0; i<amount; i++){
			result[i] = "UFT_8";
		}
		return result;
	}

	private void printReferencesReportByRevision(ArrayList<String> sharedRevisionEntryList,Multimap<String, String> invokers) {
		try {
			//int references = getNumberOfReferencesByRevision(invokers);
			String header = "";
			String revision = (sharedRevisionEntryList.get(0).split(";"))[0];
			File file = new File("results/parser_references_by_revision_numbers.csv");
			if(!file.exists()){
				file.createNewFile();
				header = "revision;referencesToRenamedMethods";
			}
			FileWriter fw = new FileWriter(file, true);
			BufferedWriter bw = new BufferedWriter(fw);
			//bw.write(revision+";"+references);
			if(!header.isEmpty()){
				bw.write(header+"\n");
			}
			bw.write(revision+";"+invokers.keySet().size());
			if(null!=currentMergeResult)
				currentMergeResult.refToRenamedMethodsFromParser = invokers.keySet().size();
			bw.newLine();
			bw.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void printReferencesReportByRenamedMethodOptimized(ArrayList<String> sharedRevisionEntryList, Multimap<String, String> invokers) {
		try {
			String header = "";
			File file = new File("results/parser_references_by_renamed_method_numbers.csv");
			if(!file.exists()){
				file.createNewFile();
				header = "revision;file;methodSignature;references";
			}
			FileWriter fw = new FileWriter(file, true);
			BufferedWriter bw = new BufferedWriter(fw);
			if(!header.isEmpty()){
				bw.write(header+"\n");
			}
			for(String entry : sharedRevisionEntryList){
				int references = getNumberOfReferencesByRenamedMethodOptimized(entry, invokers);
				bw.write(entry+";"+references);
				bw.newLine();
			}
			bw.close();
			fw.close();

			for(String entry : sharedRevisionEntryList){
				printLogOfInvocationsOptimized(entry, invokers);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void printLogOfInvocationsOptimized(String entry, Multimap<String, String> invokers){
		try {
			//revision;member;renamed method;invoker
			String header = "";
			File file = new File("results/log_parser_invocations.log");
			if(!file.exists()){
				file.createNewFile();
				header = "revision;file;renamedMethod;invokers";
			}
			FileWriter fw = new FileWriter(file, true);
			BufferedWriter bw = new BufferedWriter(fw);
			if(!header.isEmpty()){
				bw.write(header+"\n");
			}
			for(String method : invokers.get(entry)){
				bw.write(entry+";"+method);
				bw.newLine();
			}
			bw.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void printCompilationProblemsReport(String entry, ErrorType errorToCheck, int errors) {
		try {
			String[] columns = entry.split(";");
			String revision	 = columns[0];
			String classFile = columns[1];
			if(errorToCheck == ErrorType.IMPORT_ERROR){
				String header = "";
				File file = new File( "results/parser_import_issue_numbers.csv" );
				if(!file.exists()){
					file.createNewFile();
					header = "revision;file;errors\n";
				}
				FileWriter fw = new FileWriter(file, true);
				BufferedWriter bw = new BufferedWriter( fw );
				if(!header.isEmpty()){
					bw.write(header);
				}
				bw.write(revision+";"+classFile+";"+errors);
				currentMergeResult.importIssuesFromParser += errors;
				bw.newLine();
				bw.close();
				fw.close();
			} else if(errorToCheck == ErrorType.DUPLICATED_ERROR){
				errors = errors/2;
				String header = "";
				File file = new File( "results/parser_duplicated_issue_numbers.csv" );
				if(!file.exists()){
					file.createNewFile();
					header = "revision;file;errors\n";
				}
				FileWriter fw = new FileWriter(file, true);
				BufferedWriter bw = new BufferedWriter( fw );
				if(!header.isEmpty()){
					bw.write(header);
				}
				bw.write(revision+";"+classFile+";"+errors);
				currentMergeResult.duplicationIssuesFromParser += errors;
				bw.newLine();
				bw.close();
				fw.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private int getNumberOfReferencesByRenamedMethodOptimized(String entry,Multimap<String, String> invokers) {
		int references = 0;
		references +=invokers.get(entry).size();
		return references;
	}

	private int getNumberOfReferencesByRevision(Multimap<String, String> invokers) {
		int references = 0;
		for(String key : invokers.keySet()){
			references +=invokers.get(key).size();
		}
		return references;
	}

	private String simplifyMethodSignature(IMethodBinding mb) {
		String simplifiedMethodSignature = ((mb.toString()).replaceAll("(\\w)+\\.", "")).replaceAll("\\s+","");
		return simplifiedMethodSignature;
	}

	private ArrayList<String> preprocess(String revDir, ArrayList<String> renamedMethodsList) throws IOException{
		System.out.println("looking for grep renaming canditates files...");
		ArrayList<String> acceptedFiles = new ArrayList<String>();
		findCandidateFiles(revDir,renamedMethodsList,acceptedFiles);
		System.out.println("found grep " +acceptedFiles.size()+" renaming canditates files...");
		return acceptedFiles;
	}

	private void findCandidateFiles(String revDir, ArrayList<String> renamedMethodsList, ArrayList<String> acceptedFiles) throws IOException {
		File directory = new File(revDir);
		File[] fList = directory.listFiles();
		for (File file : fList){
			if (file.isDirectory()){
				findCandidateFiles(file.getAbsolutePath(), renamedMethodsList, acceptedFiles);
			} else {
				if(FilenameUtils.getExtension(file.getAbsolutePath()).equalsIgnoreCase("java") && isCandidateFile(file, renamedMethodsList)){
					if(!acceptedFiles.contains(file.getAbsolutePath()))
						acceptedFiles.add(file.getAbsolutePath());
				}
			}
		}
	}

	private boolean isCandidateFile(File file, ArrayList<String> renamedMethodsList) throws IOException {
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file.getAbsolutePath()))); 
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			for(String entry : renamedMethodsList){
				String renamedMethod = (entry.split(";")[2]).split("\\(")[0];
				//if(line.contains(renamedMethod)){
				if(line.matches("(?s).*\\b"+renamedMethod+"\\b.*")){
					bufferedReader.close();
					return true;
				}
			}
		}
		bufferedReader.close();
		return false;
	}

	public static void main(String[] args) {
		MethodReferencesFinderAST m = new MethodReferencesFinderAST();

		//	String d = "C:/Users/Guilherme/Desktop/example_rename/rev;C:/Users/Guilherme/Desktop/example_rename/rev/Testes.java";
		String d = "C:\\GGTS\\workspace\\MRFinder\\src;C:\\GGTS\\workspace\\MRFinder\\src\\Test.java";
		ArrayList<String> ar = new ArrayList<String>();
		ar.add(d);

		//m.lookForCompilantionProblems(ar, ErrorType.DUPLICATED_ERROR);
		m.lookForCompilantionProblems(ar);


		long t0 = System.currentTimeMillis();
		//m.executeOptimizedRenamings("in.csv");
		long tf = System.currentTimeMillis();

		System.out.println("analysis time: " + ((tf-t0)/60000) + " minutes");
	}

	private static class ParserRunnable implements Runnable {
		private String[] sources;
		private String[] encodings;
		private BlockingQueue<String> toProcess;
		private String[] classPaths;
		private ArrayList<String> sharedRevisionEntryList;
		private Multimap<String, String> invokers;
		private MethodReferencesFinderAST finder;
		private int numOfThreads;

		public ParserRunnable(String[] sources, String[] encodings, String[] classPaths, BlockingQueue<String> acceptedFiles, ArrayList<String> sharedRevisionEntryList, Multimap<String, String> invokers, int numOfThreads) {
			this.sources 	= sources;
			this.encodings 	= encodings;
			this.classPaths = classPaths;
			this.toProcess 	= acceptedFiles;
			this.sharedRevisionEntryList = sharedRevisionEntryList;
			this.invokers 	= invokers;
			this.finder = new MethodReferencesFinderAST();
			this.numOfThreads = numOfThreads;
		}

		@Override
		public void run() {
			try {
				String javaFilePath = null;
				long threadId = Thread.currentThread().getId()%this.numOfThreads+1;
				while((javaFilePath = toProcess.poll()) != null) {
					File javaFile 	= new File(javaFilePath);
					System.out.println("processing renaming candidate by thread "+ threadId +": " + javaFilePath);
					finder.createParserAndFindMethodReferencesOptimized(sources, encodings, classPaths, javaFile, sharedRevisionEntryList, invokers);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private enum ErrorType {
		IMPORT_ERROR, 
		DUPLICATED_ERROR,
	}

}
