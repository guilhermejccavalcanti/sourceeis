import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;


public class ConflictsParser {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void parse(){
		String content = readFileContent(new File("filetoread.txt"));
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setSource(content.toCharArray());
		parser.setKind(ASTParser.K_CLASS_BODY_DECLARATIONS);
		Map options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8); 
		parser.setCompilerOptions(options);
		ASTNode parsed = parser.createAST(null); //toString.size > 3


		//CopyOfNaiveASTFlattener p = new CopyOfNaiveASTFlattener();
		NaiveASTFlattenerKCOMPILATIOUNIT p = new NaiveASTFlattenerKCOMPILATIOUNIT();
		//OriginalNaiveASTFlattener p = new OriginalNaiveASTFlattener();
		parsed.accept(p);
		String result = p.getResult();
		System.out.println(result);

	}


	public String readFileContent(File file){
		String content = "";
		try{
			BufferedReader reader = Files.newBufferedReader(Paths.get(file.getAbsolutePath()), StandardCharsets.ISO_8859_1);
			content = reader.lines().collect(Collectors.joining("\n"));
		}catch(Exception e){}
		return content;
	}

	public static void main(String[] args) {
		new ConflictsParser().parse();
	}

}
