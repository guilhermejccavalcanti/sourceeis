package cin.ufpe.br.app;

import java.io.IOException;
import java.util.ArrayList;

import cin.ufpe.br.analyser.Analyser;
import cin.ufpe.br.util.ConflictsCollector;
import cin.ufpe.br.util.MergeConflict;

public class Main {

	public static void main(String[] args) {
		try {
			long t0 = System.currentTimeMillis();

			ArrayList<MergeConflict> conflicts = ConflictsCollector.collect("test.txt");
			Analyser.analyse(conflicts);
		
			long tf = System.currentTimeMillis();
			long mergeTime = ((tf-t0)/60000);
			System.out.println("analysis time: " + mergeTime + " minutes");
		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
