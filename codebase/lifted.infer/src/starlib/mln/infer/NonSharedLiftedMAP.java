package starlib.mln.infer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import starlib.gm.core.LogDouble;
import starlib.mln.core.Atom;
import starlib.mln.core.MLN;
import starlib.mln.core.WClause;
import starlib.mln.util.GrindingMill;
import starlib.mln.util.Parser;

public class NonSharedLiftedMAP {
	
	private WeightedMaxSatSolver solver;
	
	private static boolean print = true;
	
	public long networkConstructionTime = 0 ;
	public long solverTime = 0;
	public double bestValue = Double.NEGATIVE_INFINITY;
	
	public NonSharedLiftedMAP(WeightedMaxSatSolver _solver) {
		solver = _solver;
	}
	
	private void convertToWeightedMaxSat(MLN mln) {
		
		solver.setNoVar(mln.numberOfSymbols());
		solver.setNoClauses(mln.numberOfClauses());
		
		for (WClause clause : mln.getClauses()) {
			int power = 1;
			int[] c = new int[clause.atoms.size()];
			
			for (int i = 0; i < clause.atoms.size(); i++) {
				Atom atom = clause.atoms.get(i);
				power *= atom.getNumberOfGroundings();
				
				if(clause.sign.get(i)) {
					c[i] = -atom.symbol.id-1;
				} else {
					c[i] = atom.symbol.id+1;
				}
			}
			
			if(clause.weight.isInfinite()) {
				solver.addHardClause(c);
			} else {
				LogDouble weight = clause.weight.power(power);
				solver.addSoftClause(weight.getValue(), c);
			}
			
		}

	}
	
	private MLN convert(MLN mln){
		long time = System.currentTimeMillis();

		List<List<Integer>> termsToGround = new ArrayList<List<Integer>>(mln.numberOfSymbols());
		for (int i = 0; i < mln.numberOfSymbols(); i++) {
			termsToGround.add(new ArrayList<Integer>());
		}

		for (WClause clause : mln.getClauses()) {

			//link clauses
			for (int i = 0; i < clause.atoms.size(); i++) 
			{
				for (int j = 0; j < clause.atoms.get(i).terms.size(); j++)
				{
					for (int k = i+1; k < clause.atoms.size(); k++) 
					{
						for (int l = 0; l < clause.atoms.get(k).terms.size(); l++)
						{
							if(clause.atoms.get(i).terms.get(j) == clause.atoms.get(k).terms.get(l))
							{
								// There is a link between Term_{i,j} and Term_{k,l}

								termsToGround.get(clause.atoms.get(i).symbol.id).add(j);
								termsToGround.get(clause.atoms.get(k).symbol.id).add(l);
							}
						}
					}
				}
			}
		}
		
		MLN nonSharedMln = GrindingMill.ground(mln, termsToGround);

		System.out.println("Time taken to ground is " + (System.currentTimeMillis() - time) + " ms");
		
		return nonSharedMln;
	}
	
	public  void run(String filename) throws FileNotFoundException {
		long time = System.currentTimeMillis();
		long startTime = time;
		
		networkConstructionTime = time;
		solverTime = 0;
		
		MLN mln = new MLN();
		Parser parser = new Parser(mln);
		parser.parseInputMLNFile(filename);

		if(print)
			System.out.println("Time to parse = " + (System.currentTimeMillis() - time) + " ms");
		
		time = System.currentTimeMillis();
		MLN nonSharedMln = this.convert(mln);
		
		if(print)
			System.out.println("Time to convert = " + (System.currentTimeMillis() - time) + " ms");
		
		time = System.currentTimeMillis();
		networkConstructionTime = time - networkConstructionTime;
		
		this.convertToWeightedMaxSat(nonSharedMln);
		solver.solve();
		
		long endTime = System.currentTimeMillis();
		solverTime = endTime - time;
		
		bestValue = solver.bestValue();
		
		if(print) {
			System.out.println("Running time of LMAP = " + (endTime -  time) + " ms");
			System.out.println("Total running time is " + (endTime -  startTime) + " ms");
		}
	}
	
	public static void main(String[] args) throws IOException {
		WeightedMaxSatSolver solver = new GurobiWmsSolver();
		solver.setTimeLimit(100);
		
		NonSharedLiftedMAP lmap = new NonSharedLiftedMAP(solver);
		
		lmap.run("love_mln_int_10.txt");
	}

	protected void writeDimacs(MLN mln, PrintWriter out) {
		
		out.println("p wcnf " + mln.numberOfSymbols() + " " + mln.numberOfClauses());
		
		for (WClause clause : mln.getClauses()) {
			Double power = 1.0;
			int[] c = new int[clause.atoms.size()];
			
			for (int i = 0; i < clause.atoms.size(); i++) {
				Atom atom = clause.atoms.get(i);
				power *= atom.getNumberOfGroundings();
				if(power < 0) {
					System.err.println("Arithmatic error occurred!");
					System.exit(1);
				}
				
				if(clause.sign.get(i)) {
					c[i] = -atom.symbol.id-1;
				} else {
					c[i] = atom.symbol.id+1;
				}
			}
			
			LogDouble weight = clause.weight.power(power);
			out.print(weight.getValue() + " ");
			for (int i = 0; i < c.length; i++) {
				out.print(c[i] + " ");
			}
			out.println("0");
			out.flush();
			
		}
		out.close();

	}

}
