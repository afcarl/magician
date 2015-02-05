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
import starlib.mln.store.GroundStore;
import starlib.mln.store.GroundStoreFactory;
import starlib.mln.util.Parser;

public class EfficientMaxWalkSat {

	protected Random random = new Random(System.currentTimeMillis());

//	private int maxTries;

	protected int maxSteps = 100000000;
	
	protected double p = 0.7;
	
	protected double randomClauseProb = 0.1;
	
	protected int timeOut = Integer.MAX_VALUE;
	
	protected int printInterval = 10;
	
	
	protected MLN mln;
	
	// We will use an undirected graphical model as an interpretation
	protected GroundStore groundStore;
	protected GroundStore bestSolution;
	
	protected double minSum;

	protected double unsSum;
	
	protected double[] unsatClauseWeight;
	
	protected double[] clauseWeight;
	
	protected List<List<Integer>> clauseWiseAtomsToPickBest;
	
	protected int step;
	
	protected static boolean print = true;
	
	
	// XXX Hack !!! Local variables defined as class members to avoid GC
	protected List<Integer> groundClause;
	protected double rand_num;
	protected double cdf;

	
	public EfficientMaxWalkSat(MLN mln_) {
		this.mln = mln_;
		unsatClauseWeight = new double[mln.numberOfClauses()];
		clauseWeight = new double[mln.numberOfClauses()];
		groundClause = new ArrayList<>(mln.getMaximumClauseSize() + 1);
		initGroundStore();
		initSymbolToPickBest();
		
		int i=0;
		for (WClause clause : mln.getClauses()) {
			clauseWeight[i] = clause.weight.getLogValue();
			i++;
		}
	}
	
	protected void initGroundStore() {
		groundStore = GroundStoreFactory.createGraphModBasedGroundStore(mln);
	}
	
	protected void initSymbolToPickBest() {
		clauseWiseAtomsToPickBest = new ArrayList<>(mln.numberOfClauses());
		
		for (int i = 0; i < mln.numberOfClauses(); i++) {
			WClause clause = mln.getClause(i);

			List<PredicateSymbol> symbolsToVisit = new ArrayList<>(clause.atoms.size());
			List<Integer> atomIndexInClause = new ArrayList<>(clause.atoms.size());
			
			for (int j = 0; j < clause.atoms.size(); j++) {
				PredicateSymbol symbol = clause.atoms.get(j).symbol;
				
				if(symbolsToVisit.contains(symbol))
					continue; // Ignore self-joined clauses
				else {
					symbolsToVisit.add(symbol);
					atomIndexInClause.add(j);
				}
			}
				
			clauseWiseAtomsToPickBest.add(atomIndexInClause);
		}
	}
	
	public GroundStore getBestSolution() {
		return bestSolution;
	}
	
	public void run() {
		
		setRandomState();
		updateGroundStore();
		this.updateUnsatStat();

		// Start clock after initializing
		long clockStartTime = System.currentTimeMillis();
		long lastPrintTime = clockStartTime;
		
		minSum = Double.MAX_VALUE;
		step = 1;

		// run of the algorithm until condition of termination is reached
		saveBestSolution();
		while (step < maxSteps) {

			// choose between greedy step and random step
			if (random.nextDouble() < p)
				greedyMove();
			else 
				stochasticMove();

			step++;
			
			sampleSoultion();

			// if there is another new minimal unsatisfied value
			if (unsSum < minSum){
				// saves new minimum, resets the steps which shows how often the algorithm hits the minimum (minSteps)
				minSum = unsSum;
				saveBestSolution();
			}
			
			if(step%5 == 0) {
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
	
	protected void saveBestSolution() {
		bestSolution = groundStore.clone();
	}

	protected void sampleSoultion() {
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
		
		Atom atom = mln.getClause(clauseIndex).atoms.get(atomIndex);
		this.flipAtom(atom.symbol, atomId);
	}
	
	protected void flipAtom(PredicateSymbol symbol, int atomId) {
		groundStore.flipAtom(symbol, atomId);
		updateUnsatStat(symbol);
	}

	protected void tempFlipAtom(PredicateSymbol symbol, int atomId) {
		groundStore.flipAtom(symbol, atomId);
	}

	protected void tempUnflipAtom(PredicateSymbol symbol, int atomId) {
		groundStore.unflipAtom(symbol, atomId);
	}
	
	protected double tempFlipOverhead(PredicateSymbol symbol) {
		double deltaOverhead = 0.0;
		
		long time = System.currentTimeMillis();
		
		for (Integer clauseId : mln.getClauseIdsBySymbol(symbol)) {
			double clauseOverheadDelta = this.clauseOverheadDelta(clauseId);
			deltaOverhead += clauseOverheadDelta;
		}

		if(print)
			System.out.println("Time taken to compute overhead for "+mln.getClauseIdsBySymbol(symbol).size()+" clauses is: "+ (System.currentTimeMillis() - time) + " ms.");


		return deltaOverhead;
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
		int clauseIndex = random.nextInt(mln.numberOfClauses());

		// A ground clause is represented as list 
		groundStore.getRandomGroundClause(clauseIndex, groundClause);
		
		return groundClause;
	}
	
	private double clauseOverhead(int clauseId) {
		double unsatClauseCount = groundStore.noOfFalseGroundings(clauseId);
		
		double clauseOverhead = clauseWeight[clauseId] * unsatClauseCount;
		
		return clauseOverhead;
	}
	
	private double clauseOverheadDelta(int clauseId) {
		double unsatClauseCountIncreased = groundStore.noOfFalseGroundingsIncreased(clauseId);
		double clauseOverheadDelta = clauseWeight[clauseId] * unsatClauseCountIncreased;
		return clauseOverheadDelta;
	}
	
	private void pickAndFlipBestAtom(List<Integer> clause) {
		PredicateSymbol bestAtom = null;
		int bestGroundAtomId = -1;
		double bestCost = Double.MAX_VALUE;
		
		int clauseIndex = clause.get(0);
		WClause firstOrderClause = mln.getClause(clauseIndex);
		List<Atom> atoms = firstOrderClause.atoms;
		
		List<Integer> atomIndicesToPickBestFrom = clauseWiseAtomsToPickBest.get(clauseIndex);
		
		// If empty clause or all evidence clause do nothing return
		if(atomIndicesToPickBestFrom.size() < 1) {
			return;
		}
		
		// If unit clause, no need to choose the best (i.e.- straight away flip it)
		if(atomIndicesToPickBestFrom.size() == 1) {
			int bestAtomIndexInClause = atomIndicesToPickBestFrom.get(0);
			bestAtom = atoms.get(bestAtomIndexInClause).symbol;
			bestGroundAtomId = clause.get(bestAtomIndexInClause + 1);
			
			this.flipAtom(bestAtom, bestGroundAtomId);
			return;
		}
		
		for (int i = 0; i < atomIndicesToPickBestFrom.size(); i++) 
		{
			int atomIndexInClause = atomIndicesToPickBestFrom.get(i);
			PredicateSymbol symbol = atoms.get(atomIndexInClause).symbol;
			int atomId = clause.get(atomIndexInClause + 1);
			
			// Flip the ground atom to compute the cost
			this.tempFlipAtom(symbol, atomId);

			// Compute cost
			double deltaOverhead = tempFlipOverhead(symbol);
			
			// check whether the candidate is better
			boolean newBest = false;
			if (deltaOverhead < bestCost) {
				newBest = true;       // if the deltacosts enhances the state we found a new best candidate
			} else if (deltaOverhead == bestCost && random.nextBoolean()) {
				newBest = true;       // ties broken at random
			}
			
			if (newBest) {
				bestAtom = symbol;
				bestGroundAtomId = atomId;
				bestCost = deltaOverhead;
			}
			
			// Now undo the flip of the ground atom
			this.tempUnflipAtom(symbol, atomId);
		}
		
		this.flipAtom(bestAtom, bestGroundAtomId);
	}
	
	
	private void updateUnsatStat(PredicateSymbol symbol) {
		double deltaOverhead = 0.0;
		
		long time = System.currentTimeMillis();
		
		groundStore.update(mln.getClauseIdsBySymbol(symbol));
		if(print)
			System.out.println("Time taken to update the ground store is: "+ (System.currentTimeMillis() - time) + " ms.");
		
		for (Integer clauseId : mln.getClauseIdsBySymbol(symbol)) {
			double previousClauseOverHead = unsatClauseWeight[clauseId];
			double clauseOverhead = this.clauseOverhead(clauseId);
			deltaOverhead = deltaOverhead + clauseOverhead - previousClauseOverHead;
			
			unsatClauseWeight[clauseId] = clauseOverhead;
		}
		
		unsSum += deltaOverhead;
	}

	protected void updateUnsatStat() {
		double overhead = 0.0;
		
		for (int i = 0; i < mln.numberOfClauses(); i++) {
			double clauseOverhead = this.clauseOverhead(i);
			overhead += clauseOverhead;
			
			unsatClauseWeight[i] = clauseOverhead;
		}
		unsSum = overhead;
	}
	
	protected void updateGroundStore() {
		long time = System.currentTimeMillis();
		groundStore.update();
		System.out.println("Time taken to initialize the ground store: "+ (System.currentTimeMillis() - time) + " ms.");
	}

	public static void main(String[] args) throws IOException {
		
		int noOfRun = 2;
		
		String fileName = "webkb-magician.mln" ;
		String dbFile = "webkb-0.txt" ;
		for (int i = 0; i < noOfRun; i++) {
			System.out.println("Run " + (i+1) +" for file "+fileName );
			runFor(fileName, dbFile);
		}
	}
	
	public static List<Double> runFor(String mlnFile, String dbFile) throws FileNotFoundException {
		long time = System.currentTimeMillis();
		
		MLN mln = new MLN();
		Parser parser = new Parser(mln);
		parser.parseInputMLNFile(mlnFile);
		parser.parseDbFile(dbFile);
		
//		print = false;

		System.out.println("Time to parse = " + (System.currentTimeMillis() - time) + " ms");
		
		time = System.currentTimeMillis();
		EfficientMaxWalkSat slsAlgo = new EfficientMaxWalkSat(mln);
		long timeOut = 361*1000;
		System.out.println("Time out amount is "+ timeOut +" ms.");
		slsAlgo.printInterval = 10;
		slsAlgo.timeOut = (int) timeOut;
		slsAlgo.run();
		
		
		System.out.println();
		System.out.println("Best cost found = "+slsAlgo.minSum);
		
		return null;		
	}
}
