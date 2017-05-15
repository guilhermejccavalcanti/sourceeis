package cin.ufpe.br.parser;

import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;

public class Parser {
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Parsed parse(String contentToParse, int type) throws Exception{
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setSource(contentToParse.toCharArray());
		//parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setKind(type);
		Map options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8); 
		parser.setCompilerOptions(options);
		ASTNode ast = parser.createAST(null);
		
		if(ast.toString().isEmpty())throw new Exception();
		
		PrettyPrinterASTTypes printer = new PrettyPrinterASTTypes();
		ast.accept(printer);
		return new Parsed(ast, printer);
	}

}
