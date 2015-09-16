package starlib.mln.store;

import java.io.FileNotFoundException;

import starlib.mln.core.MLN;
import starlib.mln.store.clause.CompiledStructureFactory;
import starlib.mln.store.clause.jt.JoinGraph;
import starlib.mln.store.clause.jt.JoinTreeInt;
import starlib.mln.store.clause.jt.JoinTreeIntApprox;
import starlib.mln.util.Parser;

public class GroundStoreFactory {
	

	public static GroundStore createGraphModBasedGroundStore(MLN mln) {
		CompiledStructureFactory<JoinTreeInt> factory = new CompiledStructureFactory<>(JoinTreeInt.class);
		GroundStore store = new GraphModBasedGroundStore<JoinTreeInt>(mln, factory);
		store.init();
		
		return store;
	}

	public static GroundStore createGraphModBasedGroundStoreWithApproxCount(MLN mln) {
		CompiledStructureFactory<JoinTreeIntApprox> factory = new CompiledStructureFactory<>(JoinTreeIntApprox.class);
		GroundStore store = new GraphModBasedGroundStore<JoinTreeIntApprox>(mln, factory);
		store.init();
		
		return store;
	}

	public static GroundStore createGraphModBasedGroundStoreWithBetheCount(MLN mln) {
		CompiledStructureFactory<JoinGraph> factory = new CompiledStructureFactory<>(JoinGraph.class);
		GroundStore store = new GraphModBasedGroundStore<JoinGraph>(mln, factory);
		store.init();
		
		return store;
	}

	public static GroundStore createGraphModBasedGroundStore(String mlnFile, String dbFile) throws FileNotFoundException {
		MLN mln = parseMlnFile(mlnFile, dbFile);
		return createGraphModBasedGroundStore(mln );
	}

	public static GroundStore createGraphModBasedGroundStoreWithApproxCount(String mlnFile, String dbFile) throws FileNotFoundException {
		MLN mln = parseMlnFile(mlnFile, dbFile);
		return createGraphModBasedGroundStoreWithApproxCount(mln );
	}

	public static GroundStore createGraphModBasedGroundStoreWithBetheCount(String mlnFile, String dbFile) throws FileNotFoundException {
		MLN mln = parseMlnFile(mlnFile, dbFile);
		return createGraphModBasedGroundStoreWithBetheCount(mln );
	}

	public static GroundStore createGraphModBasedGroundStore(String mlnFile) throws FileNotFoundException {
		return createGraphModBasedGroundStore(mlnFile, null);
	}
	
	private static MLN parseMlnFile(String mlnFile, String dbFile) throws FileNotFoundException {
		MLN mln = new MLN();
		
		Parser parser = new Parser(mln);
		parser.parseInputMLNFile(mlnFile);
		if(dbFile != null)
			parser.parseDbFile(dbFile);
		
		return mln;
	}
}
