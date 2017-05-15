package printer.javam;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import printer.ArtifactPrintVisitor;
import printer.PrintVisitorException;
import de.ovgu.cide.fstgen.ast.FSTNode;
import de.ovgu.cide.fstgen.ast.FSTNonTerminal;
import de.ovgu.cide.fstgen.parsers.generated_java15_merge.SimplePrintVisitor;

public class JavaMergePrintVisitor extends ArtifactPrintVisitor {

	public JavaMergePrintVisitor() {
		super("Java-File");
	}
	public void processNode(FSTNode node, File folderPath) throws PrintVisitorException {
		if(node instanceof FSTNonTerminal) {
			FSTNonTerminal nonterminal = (FSTNonTerminal)node;
			for(FSTNode child : nonterminal.getChildren()) {
				
				//WORKAROUND
				String dir 	 = folderPath.getAbsolutePath();
				char fst 	 = dir.charAt(0);
				String dsk 	 = fst+":";
				String[] sep = dir.split(dsk);
				if(sep.length > 2) {
					dir = dsk + sep[sep.length-1];
					folderPath = new File(dir);
					folderPath.mkdirs();
				}
				
				String fileName = folderPath.getPath() + File.separator + nonterminal.getName();
				SimplePrintVisitor visitor;
				try {
					File f = new File(fileName);
					if(!f.exists()){
						folderPath.mkdirs();
						f.createNewFile();
					}
					visitor = new SimplePrintVisitor(new PrintStream(fileName));
					visitor.visit((FSTNonTerminal)child);
					visitor.getResult();
				} catch (IOException e) {
					throw new PrintVisitorException(e.getMessage());
				} 
			}
		} else {
			assert(!(node instanceof FSTNonTerminal));
		}
	}
}
