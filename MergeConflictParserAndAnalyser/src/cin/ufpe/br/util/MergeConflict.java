package cin.ufpe.br.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;

public class MergeConflict {

	//file information
	public String originFile;
	public String projectName;
	public String filePath;
	public String renamedpath = null;

	//textual representation
	public String left; 	//mine
	public String base;
	public String right; 	//yours
	public String body;
	
	//syntatic representation
	public List<ASTNode> leftTypes = new ArrayList<ASTNode>();
	public List<ASTNode> rightTypes= new ArrayList<ASTNode>();
	public String leftTypesString = "";
	public String rightTypesString= "";
	
	//parsing status
	public boolean rightunableToParse 	= false; 
	public boolean leftunableToParse 	= false; 
	
	//git information
	public String leftCommit;
	public String rightCommit;
	public String baseCommit;
	public String mergeCommit;

	
	public MergeConflict(String leftConflictingContent,	String rightConflictingContent) {
		this.left  = leftConflictingContent;
		this.right = rightConflictingContent;
		this.body  ="<<<<<<< MINE\n"+
				    leftConflictingContent+
				    "=======\n"+
				    rightConflictingContent+
				    ">>>>>>> YOURS";
	}
	
	public boolean contains(String leftPattern, String rightPattern){
		if(leftPattern.isEmpty() || rightPattern.isEmpty()){
			return false;
		} else {
			leftPattern  = (leftPattern.replaceAll("\\r\\n|\\r|\\n","")).replaceAll("\\s+","");
			rightPattern = (rightPattern.replaceAll("\\r\\n|\\r|\\n","")).replaceAll("\\s+","");
			String lefttrim  = (this.left.replaceAll("\\r\\n|\\r|\\n","")).replaceAll("\\s+","");
			String righttrim = (this.right.replaceAll("\\r\\n|\\r|\\n","")).replaceAll("\\s+","");
			return (lefttrim.contains(leftPattern) && righttrim.contains(rightPattern));
		}
	}
	
	@Override
	public String toString() {
		return this.body;
	}
}
