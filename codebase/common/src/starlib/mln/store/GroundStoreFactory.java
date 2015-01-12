package starlib.mln.store;

import java.io.FileNotFoundException;

import starlib.mln.core.MLN;
import starlib.mln.util.Parser;

public class GroundStoreFactory {
	

	public static GroundStore createGraphModBasedGroundStore(MLN mln) {
		GroundStore store = new GraphModBasedGroundStore(mln);
		store.init();
		
		return store;
	}

	public static GroundStore createGraphModBasedGroundStore(String mlnFile, String dbFile) throws FileNotFoundException {
		MLN mln = new MLN();
		
		Parser parser = new Parser(mln);
		parser.parseInputMLNFile(mlnFile);
		if(dbFile != null)
			parser.parseDbFile(dbFile);
		
		return createGraphModBasedGroundStore(mln);
	}

	public static GroundStore createGraphModBasedGroundStore(String mlnFile) throws FileNotFoundException {
		return createGraphModBasedGroundStore(mlnFile, null);
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		String mlnFile = "love_mln.txt";
		String dbFile = "love_mln_db.txt";

		GroundStore gs = createGraphModBasedGroundStore(mlnFile, dbFile);
		
		System.out.println();
	}

}
