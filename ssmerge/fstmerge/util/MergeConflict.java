package util;

/**
 * Class representing a merge conflict.
 * FPFN
 * @author Guilherme
 *
 */

public class MergeConflict {

	public String fileName;
	public String left;
	public String base;
	public String right;
	public String body;
	public String bodyInclBase;
	public String originpath;

	public MergeConflict(String fileName, String left, String base, String right) {
		this.fileName = fileName;
		this.left 	= left;
		this.right 	= right;
		this.base 	= base;
	}

	public MergeConflict(String fileName, String left, String right) {
		this.left = left;
		this.right = right;
		this.fileName = fileName;
	}

	public MergeConflict(String fileName, String left, String base, String right, String conflict) {
		this.fileName = fileName;
		this.left 	= left.replaceAll("<<<<<<<","");
		this.right 	= right.replaceAll(">>>>>>>", "");
		this.base 	= base;
		this.body 	= conflict;
	}

	public MergeConflict(String leftConflictingContent,	String rightConflictingContent) {
		this.left  = leftConflictingContent.split("=======")[0];
		this.right = rightConflictingContent.split(">>>>>>>")[0];

		//FPFN getting base content
		if(this.left.contains("|||||||")){
			String[] temp = this.left.split("\\|\\|\\|\\|\\|\\|\\|");
			this.left = temp[0];
			this.base = temp[1].substring(temp[1].indexOf('\n')+1);;

			this.bodyInclBase = 
					"<<<<<<< MINE\n"+
							this.left+
							"||||||| BASE\n"+
							this.base +
							"=======\n"+
							this.right+
							">>>>>>> YOURS";
		}

		this.body  ="<<<<<<< MINE\n"+
				this.left+
				"=======\n"+
				this.right+
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

	public boolean containsRelaxed(String leftPattern, String rightPattern){
		if(leftPattern.isEmpty() || rightPattern.isEmpty()){
			return false;
		} else {
			leftPattern  	 = (leftPattern.replaceAll("\\r\\n|\\r|\\n","")).replaceAll("\\s+","");
			rightPattern 	 = (rightPattern.replaceAll("\\r\\n|\\r|\\n","")).replaceAll("\\s+","");
			String lefttrim  = (this.left.replaceAll("\\r\\n|\\r|\\n","")).replaceAll("\\s+","");
			String righttrim = (this.right.replaceAll("\\r\\n|\\r|\\n","")).replaceAll("\\s+","");

			leftPattern 	= Util.removeReservedKeywords(leftPattern);
			rightPattern 	= Util.removeReservedKeywords(rightPattern);
			lefttrim 		= Util.removeReservedKeywords(lefttrim);
			righttrim 		= Util.removeReservedKeywords(righttrim);

			return (lefttrim.contains(leftPattern) && righttrim.contains(rightPattern));
		}
	}

	public String getLeft() {
		return left;
	}

	public void setLeft(String left) {
		this.left = left;
	}

	public String getBase() {
		return base;
	}

	public void setBase(String base) {
		this.base = base;
	}

	public String getRight() {
		return right;
	}

	public void setRight(String right) {
		this.right = right;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}	

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}
	
	@Override
	public String toString() {
		return this.body;
	}
	
	public String toStringInclBase(){
		return this.bodyInclBase;
	}

}
