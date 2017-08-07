package cin.ufpe.br.blocks;



public class MatchedBlocks {

	public String mineblock;
	public String yoursblock;
	public String mergeblock;
	public String originFile;
	public String revisionFile;
	public String projectname;
	public String filePath;

	public String baseCommit;
	public String mergeCommit;
	public String rightCommit;
	public String leftCommit;

	public MatchedBlocks(String m, String y, String o, String r) {
		this.mineblock = m.substring(0, m.lastIndexOf('}')+1);
		this.yoursblock = y.substring(0, y.lastIndexOf('}')+1);
		this.originFile = o;
		this.revisionFile = r;
	}
	
	public MatchedBlocks(String o, String r) {
		this.originFile = o;
		this.revisionFile = r;
	}
}
