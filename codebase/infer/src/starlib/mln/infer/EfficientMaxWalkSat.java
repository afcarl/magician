package starlib.mln.infer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import starlib.mln.core.Atom;
import starlib.mln.core.MLN;
import starlib.mln.core.PredicateSymbol;
import starlib.mln.core.WClause;
import starlib.mln.infer.store.GraphModBasedKB;
import starlib.mln.util.Parser;

public class EfficientMaxWalkSat {

	protected Random random = new Random(System.currentTimeMillis());

//	private int maxTries;

	protected int maxSteps = 100000000;
	
	protected double p = 0.999;
	
	protected double randomClauseProb = 0.1;
	
	protected int timeOut = Integer.MAX_VALUE;
	
	protected int printInterval = 10;
	
	
	protected MLN mln;
	
	// We will use an undirected graphical model as an interpretation
	protected GraphModBasedKB groundStore;
	protected GraphModBasedKB bestSolution;
	
	protected double minSum;

	protected double unsSum;
	
	protected double[] unsatClauseWeight;
	
	protected List<List<Integer>> symbolClauseMap = new ArrayList<List<Integer>>();
	
	protected static boolean print = true;
	
	
	// XXX Hack !!! Local variables defined as class members to avoid GC
	protected List<Integer> groundClause;
	protected double rand_num;
	protected double cdf;

	
	public EfficientMaxWalkSat(MLN mln_) {
		this.mln = mln_;
		initGroundStore();
	}
	
	protected void initGroundStore() {
		groundStore = new GraphModBasedKB(mln);
		unsatClauseWeight = new double[mln.clauses.size()];
		
		List<Integer> numberOfGroundings = new ArrayList<Integer>();
		for (int i = 0; i < mln.symbols.size(); i++) {
			numberOfGroundings.add(null);
			symbolClauseMap.add(new ArrayList<Integer>());
		}
		
		int maximumClauseSize = 0;
		for (int i=0; i<mln.clauses.size(); i++) {
			WClause clause = mln.clauses.get(i);
			if(clause.atoms.size() > maximumClauseSize) {
				maximumClauseSize = clause.atoms.size();
			}
			for (Atom atom : clause.atoms) {
				Integer savedNumberOfGrounding = numberOfGroundings.get(atom.symbol.id);
				int atomsNumberOfGrounding = atom.getNumberOfGroundings();
				assert(savedNumberOfGrounding == null || savedNumberOfGrounding.equals(atomsNumberOfGrounding));
				numberOfGroundings.set(atom.symbol.id, atomsNumberOfGrounding);
				
				symbolClauseMap.get(atom.symbol.id).add(i);
			}
		}
		
		groundStore.init();
		groundClause = new ArrayList<>(maximumClauseSize + 1);
	}
	
//	public GraphModBasedKB getBestSolution() {
//		return bestSolution;
//	}
	
	public void run() {
		
		setRandomState();
		this.updateUnsatStat();

		// Start clock after initializing
		long clockStartTime = System.currentTimeMillis();
		long lastPrintTime = clockStartTime;
		
		minSum = Double.MAX_VALUE;
		int step = 1;

		String move = "";
		// run of the algorithm until condition of termination is reached
//		bestSolution = groundStore.clone();
		while (step < maxSteps && unsSum > 0) {
			boolean newBest = false;
			boolean moveChange = true;

			// choose between greedy step and random step
			if (random.nextDouble() < p) {
				greedyMove();
				moveChange = (move != "greedy");
				move = "greedy";
			} 
			else {
				stochasticMove();
				moveChange = (move != "random");
				move = "random";
			}
			step++;

			// if there is another new minimal unsatisfied value
			if (unsSum < minSum){
				newBest = true;
				// saves new minimum, resets the steps which shows how often the algorithm hits the minimum (minSteps)
				minSum = unsSum;
				// saves current best state
//				bestSolution = groundStore.clone();
			}
			
			// print progress
			if(newBest || moveChange) {
				if(print)
					System.out.printf("  step %d: %s move, sum of unsatisfied weights: %s, best: %s  %s\n", step, move, unsSum, minSum, newBest ? "[NEW BEST]" : "");
			}
			
			if(step%50 == 0) {
				long currentTime = System.currentTimeMillis();
				
				if(currentTime - lastPrintTime > 1000*printInterval) {
					lastPrintTime = currentTime;
					System.out.println(minSum);
				}
				
				if(currentTime - clockStartTime > timeOut) {
					System.out.println("Time out reached!!!");
					break;
				}
			}
		}
		System.out.println("Solution found after " + step + " steps.");
	}

	protected void setRandomState() {
		groundStore.randomAssignment();
	}
	
	protected void greedyMove() {
		// Select a clause
		List<Integer> clause = selectClause();
		
		// Select the best assignment of the best atom from the clause
		
		pickAndFlipBestAtom(clause);
	}
	
	protected void stochasticMove() {
		// Select a clause
		List<Integer> clause;
		
		if(random.nextDouble() < randomClauseProb) {
			clause = randomClause();
		} else {
			clause = selectClause();
		}
		
		int clauseIndex = clause.get(0);
		
		//Select a random atom from the clause
		int atomIndex = random.nextInt(clause.size() - 1);
		int atomId = clause.get(atomIndex+1);
		
		Atom atom = mln.clauses.get(clauseIndex).atoms.get(atomIndex);
		
		// Flip the random atom
		groundStore.flipAtom(atom.symbol, atomId);
		
		// Update the solver state, 
		updateUnsatStat(atom.symbol);
		
	}

	private List<Integer> selectClause() {
		return randomUnsatClause();
	}
	
	private List<Integer> randomUnsatClause() {
		long time = System.currentTimeMillis();

		rand_num = random.nextDouble();
		cdf = 0.0;
		int clauseIndex = -1;
		
		for (int i = 0; i < unsatClauseWeight.length; i++) {
			cdf += unsatClauseWeight[i]/unsSum;
			if (rand_num <= cdf){
				clauseIndex = i;
				break;
			}
		}
		
		// A ground clause is represented as list TODO: Fix this create a class for this 
		groundStore.getRandomUnsatGroundClause(clauseIndex, groundClause);
		
		if(print)
			System.out.println("Time taken to sample clause no "+clauseIndex+" is: "+ (System.currentTimeMillis() - time) + " ms.");

		return groundClause;
	}
	
	private List<Integer> randomClause() {
		//Random selection of clauses
		int clauseIndex = random.nextInt(mln.clauses.size());

		// A ground clause is represented as list 
		groundStore.getRandomGroundClause(clauseIndex, groundClause);
		
		return groundClause;
	}
	
	private double clauseOverhead(int clauseId) {
		double unsatClauseCount = groundStore.noOfFalseGroundings(clauseId);
		
		double clauseOverhead = mln.clauses.get(clauseId).weight.getValue() * unsatClauseCount;
		
		if(clauseOverhead < 0) {
			System.err.println("Unexpected!!");
		}
		
		return clauseOverhead;
	}
	
	private double clauseOverheadDelta(int clauseId) {
		double unsatClauseCountIncreased = groundStore.noOfFalseGroundingsIncreased(clauseId);
		double clauseOverheadDelta = mln.clauses.get(clauseId).weight.getValue() * unsatClauseCountIncreased;
		return clauseOverheadDelta;
	}
	
	private void pickAndFlipBestAtom(List<Integer> clause) {
		PredicateSymbol bestAtom = null;
		int bestGroundAtomId = -1;
		double bestCost = Double.MAX_VALUE;
		
		int clauseIndex = clause.get(0);
		
		List<Atom> atoms = mln.clauses.get(clauseIndex).atoms;
		
		// XXX Hack !!! If unit clause no need to choose the best (i.e.- straight away flip it)
		if(atoms.size() == 1) {
			bestAtom = atoms.get(0).symbol;
			bestGroundAtomId = clause.get(1);
			groundStore.flipAtom(bestAtom, bestGroundAtomId);
			updateUnsatStat(bestAtom);
			return;
		}
		
		for (int i = 0; i < atoms.size(); i++) {
			PredicateSymbol symbol = atoms.get(i).symbol;
			int atomId = clause.get(i+1);
			
			// Flip the ground atom to compute the cost
			groundStore.flipAtom(symbol, atomId);

			// Compute cost
			double deltaOverhead = 0.0;
			
			for (Integer clauseId : symbolClauseMap.get(symbol.id)) {
				double clauseOverheadDelta = this.clauseOverheadDelta(clauseId);
				deltaOverhead += clauseOverheadDelta;
			}
			
			// check whether the candidate is better
			boolean newBest = false;
			if (deltaOverhead < bestCost) {
				// if the deltacosts enhances the state we found a new best candidate
				newBest = true;
			} else if (deltaOverhead == bestCost && random.nextBoolean()) {
				// ties broken at random
				newBest = true;
			}
			
			if (newBest) {
				bestAtom = symbol;
				bestGroundAtomId = atomId;
				bestCost = deltaOverhead;
			}
			
			// Now undo the flip of the ground atom
			groundStore.unflipAtom(symbol, atomId);
			
		}
		
		groundStore.flipAtom(bestAtom, bestGroundAtomId);
		updateUnsatStat(bestAtom);
	}
	
	
	private void updateUnsatStat(PredicateSymbol symbol) {
		double deltaOverhead = 0.0;
		
		long time = System.currentTimeMillis();
		
		groundStore.update(symbolClauseMap.get(symbol.id));
		if(print)
			System.out.println("Time taken to update the ground store is: "+ (System.currentTimeMillis() - time) + " ms.");
		
		for (Integer clauseId : symbolClauseMap.get(symbol.id)) {
			double previousClauseOverHead = unsatClauseWeight[clauseId];
			double clauseOverhead = this.clauseOverhead(clauseId);
			deltaOverhead = deltaOverhead + clauseOverhead - previousClauseOverHead;
			
			unsatClauseWeight[clauseId] = clauseOverhead;
		}
		
		unsSum += deltaOverhead;
	}

	protected void updateUnsatStat() {
		long time = System.currentTimeMillis();
		
		groundStore.update();
		
		System.out.println("Time taken to initialize the ground store: "+ (System.currentTimeMillis() - time) + " ms.");
		
		double overhead = 0.0;
		
		for (int i = 0; i < mln.clauses.size(); i++) {
			double clauseOverhead = this.clauseOverhead(i);
			overhead += clauseOverhead;
			
			unsatClauseWeight[i] = clauseOverhead;
		}
		
		unsSum = overhead;
	}

	public static void main(String[] args) throws IOException {
		
		int noOfRun = 10;
		
//		List<String> domainList = new ArrayList<String>();
//		domainList.add("10");
//		
//		for (String domainSize : domainList) {
//			
//			String fileName = "love_mln_int_" + domainSize +".txt" ;
//			
//			System.out.println();
//			for (int i = 0; i < noOfRun; i++) {
//				System.out.println("Run " + (i+1) +" for file "+fileName );
//				runFor(fileName);
//			}
//			System.out.println();
//			
//		}
		
		String fileName = "student_mln_100.txt" ;
		for (int i = 0; i < noOfRun; i++) {
			System.out.println("Run " + (i+1) +" for file "+fileName );
			runFor(fileName);
		}
	}
	
	public static List<Double> runFor(String mlnFile) throws FileNotFoundException {
		long time = System.currentTimeMillis();
//		long startTime = time;
		
		MLN mln = new MLN();
		Parser parser = new Parser(mln);
		parser.parseInputMLNFile(mlnFile);
		
		print = false;

		System.out.println("Time to parse = " + (System.currentTimeMillis() - time) + " ms");
		
		time = System.currentTimeMillis();
		EfficientMaxWalkSat slsAlgo = new EfficientMaxWalkSat(mln);
		long timeOut = 101*1000;
		System.out.println("Time out amount is "+ timeOut +" ms.");
		slsAlgo.printInterval = 10;
		slsAlgo.timeOut = (int) timeOut;
		slsAlgo.run();
//		long endTime = System.currentTimeMillis();
		
		
		System.out.println();
		System.out.println("Best cost found = "+slsAlgo.minSum);
		
		return null;
		
//		System.out.println("Running time of SLS = " + (endTime -  time) + " ms");
//		System.out.println("Total running time is " + (endTime -  startTime) + " ms");
	}
}
