package cin.ufpe.br.parser;

import org.eclipse.jdt.core.dom.ASTNode;

public class Parsed {

	public ASTNode parsed;
	public PrettyPrinterASTTypes printer;
	
	public Parsed(ASTNode ast, PrettyPrinterASTTypes printer){
		this.parsed = ast;
		this.printer = printer;
	}
}
