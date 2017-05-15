package de.ovgu.cide.fstgen.ast;

public abstract class FSTNode {
	private String name;
	private String type;
	private FSTNonTerminal parent = null;
	public int index = -1;

	//FPFN
	private boolean foundCompatibleNode = false;
	private FSTNode compatibleNode 		= null;

	protected FSTNode(String type, String name) {
		this.setType(type);
		this.setName(name);
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public void setParent(FSTNonTerminal parent) {
		this.parent = parent;
	}

	public FSTNonTerminal getParent() {
		return parent;
	}

	public boolean compatibleWith(FSTNode node) {
		return this.getType().equals(node.getType()) && this.getName().equals(node.getName());
	}

	public abstract FSTNode getShallowClone() ;

	public abstract FSTNode getDeepClone() ;

	public abstract String printFST(int i) ;

	public abstract void accept(FSTVisitor visitor);

	public String getFeatureName() {
		if (getType().equals("Feature")) {
			return getName();
		} else {
			return getParent().getFeatureName();
		}
	}

	//FPFN
	public boolean isFoundCompatibleNode() {
		return foundCompatibleNode;
	}

	//FPFN
	public void setFoundCompatibleNode(boolean foundCompatibleNode) {
		this.foundCompatibleNode = foundCompatibleNode;
	}


	//FPFN
	public FSTNode getCompatibleNode() {
		return compatibleNode;
	}
	//FPFN

	public void setCompatibleNode(FSTNode compatibleNode) {
		//if(this.compatibleNode == null){
			this.compatibleNode = compatibleNode;
		//}
	}


}
