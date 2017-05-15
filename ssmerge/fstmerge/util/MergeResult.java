package util;

public class MergeResult {
		public String projectName	= "";
		public String revision		= "";
		
		public int ssmergeConfs 	= 0;
		public int ssmergeLOC		= 0;
		public int ssmergeFiles		= 0;

		public int linedbasedConfs 	= 0;
		public int linebasedLOC		= 0;
		public int linebasedFiles	= 0;
		
		public int jdimeConfs 	= 0;
		public int jdimeLOC		= 0;
		public int jdimeFiles	= 0;
		
		public int semanticConfs	= 0;
		
		//false positives
		public int renamingConflictsFromSsmerge = 0;
		public int refToRenamedMethodsFromParser  = 0;
		public int orderingConflicts  			= 0;
		public int consecutiveLinesConflicts 	= 0;
		public int spacingConflicts 		 	= 0;
		public int consecutiveLinesAndSpacingConflicts 	 	= 0;
		public int renamingConflictsAndSpacingFromSsmerge 	= 0;
		public int renamingConflictsFromSsmergeDueToIdentation = 0;
		public int enumRenamingConflictsFromSsmerge  = 0;
		
		//false negatives
		public int importIssuesFromSsmergePackagePackage = 0;
		public int importIssuesFromSsmergePackageMember  = 0;
		public int importIssuesFromSsmergeMemberMember   = 0;
		public int importIssuesFromParser	    = 0;
		public int importsInserted				= 0;
		
		public int duplicationIssuesFromSsmerge = 0;
		public int duplicationIssuesFromParser  = 0;
		
		public int editionsToDifferentPartsOfSameStmt= 0;
		public int newArtefactsReferencingEditedOnes = 0;
		public int anonymousBlocks 					 = 0;
		public int acidentalConflicts				 = 0;

		
		public int equalConfs						 = 0;
		public int crosscutingConflicts              = 0;
}
