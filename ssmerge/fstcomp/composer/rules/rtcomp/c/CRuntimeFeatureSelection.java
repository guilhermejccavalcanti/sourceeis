package composer.rules.rtcomp.c;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import metadata.CompositionMetadataStore;

public class CRuntimeFeatureSelection {

	CompositionMetadataStore meta;
	private File cnfFile;
	private String headerContents;
	private String cFileContents;
	
	public CRuntimeFeatureSelection(CompositionMetadataStore meta, File cnfFile) {
		this.meta = meta;
		this.cnfFile = cnfFile;
	}
	
	public void process() {
		headerContents = "#ifndef FEATURESELECT_H\n" +
				"#define FEATURESELECT_H\n" +
				"/*\n" +
				" * DO NOT EDIT! THIS FILE IS AUTOGENERATED BY fstcomp \n" +
				" */\n" +
				"\n" +
				"" +
				"";
		
		// a global variable per feature
		Set<String> featureSet = new HashSet<String>(meta.getFeatures());
		for (String feature: featureSet) {
			headerContents += "int __SELECTED_FEATURE_" + feature + ";\n\n";
		}

		cFileContents = "#include \"featureselect.h\"\n\n" +
		"/*\n" +
		" * DO NOT EDIT! THIS FILE IS AUTOGENERATED BY fstcomp \n" +
		" */\n" +
		"\n";
		
		cFileContents += "//random value selection code; optimized for CPAchecker\n" +
			"extern int __VERIFIER_nondet_int(void);\n" +
			"int select_one() {if (__VERIFIER_nondet_int()) return 1; else return 0;}\n" +
			"\n\n" +
			"void select_features() {\n";
		for (String feature: featureSet) {
			cFileContents += "\t__SELECTED_FEATURE_" + feature + " = select_one();\n";
		}
		
		cFileContents += "}\n\n";
				
		processRestrictions();
		
		headerContents += "\n\nint select_one();\n";
		headerContents += "void select_features();\n";
		headerContents += "void select_helpers();\n";
		headerContents += "int valid_product();\n";
		

		headerContents += "#endif\n";
		
		}
	
	public void saveTo(String filebasename) throws IOException {
		process();
		FileWriter headerfile = null;
		try {
		headerfile = new FileWriter(filebasename + ".h");
		headerfile.write(headerContents);
		} finally {
			if (headerfile != null) {
				headerfile.close();
			}
		}
		
		FileWriter cfile = null;
		try {
			cfile = new FileWriter(filebasename + ".c");
			cfile.write(cFileContents);
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		} finally {
			if (cfile != null) {
				cfile.close();
			}
		}
	}
	 
	public void processRestrictions() {
		Scanner scanner = null;
		try {
			scanner = new Scanner(cnfFile);
		} catch (FileNotFoundException e) {
			System.out.println("model restrictions file not found!");
			System.out.println("looked in: " + cnfFile);
			throw new RuntimeException();

		}
		
		scanner.useDelimiter("\\A");
		
		String cnf = scanner.next();
		scanner.close();
		
		cnf = cnf.replaceAll("//[^\\n]*", ""); //strip comments
		cnf = cnf.replaceAll(" \\n", " ");
		cnf = cnf.replaceAll("\\n", " ");
		cnf = cnf.replaceAll("xor ", "^ ");
		cnf = cnf.replaceAll("and ", "&& ");
		cnf = cnf.replaceAll("or ", "|| ");
		cnf = cnf.replaceAll("not ", "! ");
		cnf = cnf.replaceAll(" \\n", " ");
		cnf = cnf.replaceAll("\\n", " ");
		cnf = cnf.replaceAll(" \\r", " ");
		cnf = cnf.replaceAll("\\r", " ");
		cnf = cnf.replaceAll("\\(", "( ");
		cnf = cnf.replaceAll("\\)", " )");
		
	    Pattern varsRegEx = Pattern.compile("[a-zA-Z_]+[a-zA-Z0-9_]+");
		Matcher matcher = varsRegEx.matcher(cnf);

		Set<String> variables = new HashSet<String>();
		Set<String> nonterminals = new HashSet<String>();
		
		if (!matcher.find()) {
			System.out.println("Expected at least one production in cnfFile, none found!!");
			throw new RuntimeException();
		}
		matcher.reset();// start from the beginning again
		// Find all matches
	    while (matcher.find()) {	      
	      variables.add(matcher.group());
	    }
		
	    for (String var: variables) {
	    	
	    	String replacement;
	    	if (meta.getFeatures().contains(var)) {
	            replacement = "__SELECTED_FEATURE_" + var;
	        } else {
	            replacement = "__GUIDSL_NON_TERMINAL_" + var;
	            nonterminals.add(replacement);
	        }
	    	// replace feature variables in string
	    	if (cnf.matches(".*" + "\\s" + var)) {
	        	cnf = cnf + " "; // this causes the next line to find the feature variable
	        }
	        cnf = cnf.replaceAll("\\s" + var + "\\s", ' ' + replacement + ' ');
	        // if the first letter is the start of a feature variable, we can use replaceFirst
	        if (cnf.matches(var + "\\s" + ".*")) {
	        	cnf = cnf.replaceFirst(var + "\\s", replacement + ' ');
	        }
	    }
	    //cosmetics
	    cnf = cnf.replaceAll("\\) \\)","))");
	    cnf = cnf.replaceAll("\\( \\(","((");
	    cnf = cnf.replaceAll("! ","!");

		headerContents += "int __GUIDSL_ROOT_PRODUCTION;\n";
	    
	    for (String nt: nonterminals) {
	    	headerContents += "int " + nt + ";\n";
	    }
	    
		StringBuffer res = new StringBuffer();
	    
	    res.append("\nvoid select_helpers() {\n");
	    res.append("\t__GUIDSL_ROOT_PRODUCTION = 1;\n");
	    for (String nt: nonterminals) {
	        //res.append("\t" + nt + " = select_one();\n");
	    	res.append("\t" + nt + " = 1;\n");
	    }    
	    res.append("}\n\n");	    
	    res.append("int valid_product() {\n");
	    res.append("\t return " + cnf + ";\n");
	    res.append("}\n");
		
		cFileContents += res.toString();
	}
	
	
}