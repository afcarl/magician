package starlib.mln.infer;


import java.io.FileNotFoundException;
import java.io.IOException;

import starlib.gm.core.LogDouble;
import starlib.mln.core.Atom;
import starlib.mln.core.MLN;
import starlib.mln.core.WClause;
import starlib.mln.store.GroundStore;
import starlib.mln.store.GroundStoreFactory;

public class UnsoundLiftedMAP {
	
	private WeightedMaxSatSolver solver;
	
	private static boolean print = true;
	
	public long networkConstructionTime = 0 ;
	public long solverTime = 0;
	public double bestValue = Double.NEGATIVE_INFINITY;
	public int[] model;
	
	public UnsoundLiftedMAP(WeightedMaxSatSolver _solver) {
		solver = _solver;
	}
	
	private void convertToWeightedMaxSat(MLN mln) {
		
		solver.setNoVar(mln.numberOfSymbols());
		solver.setNoClauses(mln.numberOfClauses());
		
		for (WClause clause : mln.getClauses()) {
			int power = clause.getNumberOfGroundings();
			int[] c = new int[clause.atoms.size()];
			
			for (int i = 0; i < clause.atoms.size(); i++) {
				Atom atom = clause.atoms.get(i);

				if(clause.sign.get(i)) {
					c[i] = -atom.symbol.id-1;
				} else {
					c[i] = atom.symbol.id+1;
				}
			}
			
			LogDouble weight = clause.weight.power(power);
			solver.addSoftClause(weight.getValue(), c);
			
		}

	}
	
	public  void run(String filename, String dbFile) throws FileNotFoundException {
		long time = System.currentTimeMillis();
		long startTime = time;
		
		networkConstructionTime = time;
		solverTime = 0;
		
		GroundStore gs = GroundStoreFactory.createGraphModBasedGroundStore(filename, dbFile);
		MLN mln = gs.getMln();

		if(print)
			System.out.println("Time to parse = " + (System.currentTimeMillis() - time) + " ms");
		
		time = System.currentTimeMillis();
		
		networkConstructionTime = time - networkConstructionTime;
		
		this.convertToWeightedMaxSat(mln);
		solver.solve();
		
		long endTime = System.currentTimeMillis();
		solverTime = endTime - time;
		
		bestValue = solver.bestValue();
		model = solver.model();
		
		if(print) {
			System.out.println("Running time of LMAP = " + (endTime -  time) + " ms");
			System.out.println("Total running time is " + (endTime -  startTime) + " ms");
		}
	}
	
	public static void main(String[] args) throws IOException {
		WeightedMaxSatSolver solver = new GurobiWmsSolver();
		solver.setTimeLimit(500);
		
		UnsoundLiftedMAP lmap = new UnsoundLiftedMAP(solver);
		lmap.run("webkb-magician.mln", "webkb-0.txt");
	}


}
