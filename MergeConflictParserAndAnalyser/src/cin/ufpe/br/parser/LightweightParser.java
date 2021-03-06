package cin.ufpe.br.parser;

import java.io.File;
import java.util.List;

import cin.ufpe.br.util.FileHandlerr;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;

public final class LightweightParser {

	public static CompilationUnit parse(String sourceCode){
		CompilationUnit parsed = JavaParser.parse(sourceCode);
		return parsed;
	}

	//utils
	public static MethodDeclaration findMethodDeclaration(CompilationUnit parsedCode, String methodSignature){
		List<MethodDeclaration> declarations = parsedCode.getNodesByType(MethodDeclaration.class);
		for(MethodDeclaration m : declarations){
			String signature = m.getDeclarationAsString();
			if(		FileHandlerr.getStringContentIntoSingleLineNoSpacing(signature)
					.equals(FileHandlerr.getStringContentIntoSingleLineNoSpacing(methodSignature)))
				return m;
		}
		return null;
	}
	
	public static List<InitializerDeclaration> findInitializationBlocks(CompilationUnit parsedCode){
		List<InitializerDeclaration> declarations = parsedCode.getNodesByType(InitializerDeclaration.class);
		return declarations;
	}


	void findMethodCalls(MethodDeclaration methodDeclaration, List<File> filesToSearchIn, List<String> solverpaths){
		for(File file : filesToSearchIn){
			String sourceCode = FileHandlerr.readFile(file);
			CompilationUnit parsed = parse(sourceCode);
			parsed.getNodesByType(MethodCallExpr.class).stream();


			CombinedTypeSolver typesolver = new CombinedTypeSolver();
			for(String path : solverpaths){ typesolver.add(new JavaParserTypeSolver(new File(path)));}
		}

	}

	//test
	public static void main(String[] args) {
/*		CompilationUnit p = parse(FileHandler.readFile(new File("C:\\Users\\Guilherme\\Desktop\\og\\OG-Platform\\git\\projects\\OG-Financial\\src\\com\\opengamma\\financial\\analytics\\fudgemsg\\MathCurve.java")));
		MethodDeclaration m = findMethodDeclaration(p, "public InterpolatedDoublesCurve buildObject(final FudgeDeserializer deserializer, final FudgeMsg message) ");
		System.out.println(m.toString());*/
		CompilationUnit p = parse(FileHandlerr.readFile(new File("test.txt")));
		MethodDeclaration m = findMethodDeclaration(p,"public void m()");
		List<FieldAccessExpr> leftaccesss = m.getNodesByType(FieldAccessExpr.class);
		for(FieldAccessExpr leftaccess : leftaccesss){
			System.out.println(leftaccess.getNameAsString());
		}
	}

}
