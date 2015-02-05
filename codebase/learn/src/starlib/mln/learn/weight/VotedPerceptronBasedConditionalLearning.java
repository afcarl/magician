package starlib.mln.learn.weight;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import starlib.mln.core.MLN;
import starlib.mln.core.PredicateSymbol;
import starlib.mln.core.WClause;
import starlib.mln.infer.EfficientMaxWalkSat;
import starlib.mln.store.GroundStore;
import starlib.mln.store.GroundStoreFactory;

public class VotedPerceptronBasedConditionalLearning {
	
	private static double learning_rate = 0.1;
	
	private static double initial_weight = 0.0001;
	
	private static int sampleFrequency = 5;
	
	private static int weightUpdateFrequency = 80;
	
	private long timeOut = Long.MAX_VALUE;
	
	private GroundStore store;
	
	private MLN mln;
	
	private int noOfClauses;
	
	private double[] originalCounts;
	
	private double[] expectedCounts;
	
	private double[] bestCounts;
	
	private double[] weights;
	
	private static boolean[] isQuerySymbol;
	
	private int sampleSize;
	
	private int weightUpdateCount = 1;
	
	private static boolean isFirstRun = true;
	
	private long startTime;
	private long endTime;
	
	public VotedPerceptronBasedConditionalLearning(String mlnFile, String dbFile, String queryFile) throws FileNotFoundException {
		startTime = System.currentTimeMillis();
		store = GroundStoreFactory.createGraphModBasedGroundStore(mlnFile, dbFile);
		mln = store.getMln();
		endTime = System.currentTimeMillis();
		System.out.println("Ground store created in : "  + (endTime - startTime) + " ms");
		
		noOfClauses = mln.numberOfClauses();
		originalCounts = new double[noOfClauses];
		expectedCounts = new double[noOfClauses];
		bestCounts     = new double[noOfClauses];
		weights        = new double[noOfClauses];
		isQuerySymbol = new boolean[noOfClauses];
		
		Arrays.fill(weights, initial_weight); // Initialize weights to some small weight
		
		startTime = System.currentTimeMillis();
		Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(queryFile))));

		while(scanner.hasNextLine()) {
			String line = scanner.nextLine().replaceAll("\\s","");

			if(line.isEmpty()) {
				continue;
			}
			
			// Line is the query predicate symbol. Search the symbol in MLN
			String predicateName = line;
			int predicateSymbolIndex = -1;
			for (int i = 0; i < mln.numberOfSymbols(); i++) {
				//found the predicate
				if(mln.getSymbol(i).symbol.equals(predicateName)) {
					predicateSymbolIndex = i;
					break;
				}
			}
			isQuerySymbol[predicateSymbolIndex] = true;
		}
		endTime = System.currentTimeMillis();
		System.out.println("Query File processed in : "  + (endTime - startTime) + " ms");
	}
	
	private void computeExpectedCounts() {
		for (int i = 0; i < noOfClauses; i++) {
			expectedCounts[i] += store.noOfTrueGroundings(i);
		}
		sampleSize++;
	}
	
	private void updateWeights() {
		for (int i = 0; i < noOfClauses; i++) {
			double originalCount = originalCounts[i];
			double expectedCount = (sampleSize > 0) ? 
					(bestCounts[i] + expectedCounts[i]/sampleSize)/2 
					: bestCounts[i];
			double cluaseWiseLearningRate = learning_rate / (1 + originalCount);
			
			weights[i] = weights[i] + cluaseWiseLearningRate * (originalCount - expectedCount) / weightUpdateCount;
		}
		weightUpdateCount++;
		
		Arrays.fill(expectedCounts, 0);
		sampleSize = 0;
	}
	
	private void printCurrentWeights() {
		System.out.println();
		for (int i = 0; i < noOfClauses; i++) {
			System.out.println(weights[i]);
		}
		System.out.println();
	}
	
	public void run() {
		startTime = System.currentTimeMillis();
		store.update();
		endTime = System.currentTimeMillis();
		System.out.println("Ground store updated in : "  + (endTime - startTime) + " ms");
		
		for (int i = 0; i < noOfClauses; i++) {
			originalCounts[i] = store.noOfTrueGroundings(i);
		}
		
		QueryAwareWalkSat walkSat = new QueryAwareWalkSat(store, this);
		
		long clockStartTime = System.currentTimeMillis();
		while(true) {
			walkSat.run();
			this.updateWeights();
			this.printCurrentWeights();

			long currentTime = System.currentTimeMillis();
			
			if(currentTime - clockStartTime > timeOut) {
				System.out.println("Time out reached!!!");
				break;
			}
		}
		
	}
	
	public static void main(String[] args) throws FileNotFoundException {
//		String mlnFile = "webkb-magician.mln";
//		String dbFile =  "webkb-0.txt";
//		String queryFile = "qry.txt";
//		long timeOut = 360;
		
		String mlnFile = args[0];
		String dbFile =  args[1];
		String queryFile = args[2];
		long timeOut = Long.parseLong(args[3]);
		
		VotedPerceptronBasedConditionalLearning learningAlgo = new VotedPerceptronBasedConditionalLearning(mlnFile, dbFile, queryFile);
		learningAlgo.timeOut = timeOut*1000;
		learningAlgo.run();
	}
	
	private class QueryAwareWalkSat extends EfficientMaxWalkSat {
		
		public QueryAwareWalkSat(GroundStore store, VotedPerceptronBasedConditionalLearning learningAlgo) {
			super(store.getMln());
			groundStore = store;
			clauseWeight = VotedPerceptronBasedConditionalLearning.this.weights;
			this.setUpParams();
		}
		
		private void setUpParams() {
			maxSteps = 10000;
			p = 0.6;
			randomClauseProb = 0.2;
			timeOut = 300*1000;
			print = false;
		}
		
		@Override
		protected void initGroundStore() {
			// Do nothing
		}
		
		@Override
		protected void updateGroundStore() {
			if(!isFirstRun)
				super.updateGroundStore();
			isFirstRun = false;
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
					else if(isQuerySymbol[symbol.id]) {
						symbolsToVisit.add(symbol);
						atomIndexInClause.add(j);
					}
				}
				clauseWiseAtomsToPickBest.add(atomIndexInClause);
			}
		}
		
		protected void flipAtom(PredicateSymbol symbol, int atomId) {
			if(isQuerySymbol[symbol.id])
				super.flipAtom(symbol, atomId);
		}

		protected void tempFlipAtom(PredicateSymbol symbol, int atomId) {
			if(isQuerySymbol[symbol.id])
				super.tempFlipAtom(symbol, atomId);
		}

		protected void tempUnflipAtom(PredicateSymbol symbol, int atomId) {
			if(isQuerySymbol[symbol.id])
				super.tempUnflipAtom(symbol, atomId);
		}
		
		protected double tempFlipOverhead(PredicateSymbol symbol) {
			if(isQuerySymbol[symbol.id])
				return super.tempFlipOverhead(symbol);
			return Double.MAX_VALUE;
		}
		
		@Override
		protected void setRandomState() {
			for (int i = 0; i < isQuerySymbol.length; i++) {
				if(isQuerySymbol[i])
					groundStore.randomAssignment(mln.getSymbol(i));
			}
		}
		
		@Override
		protected void sampleSoultion() {
			if(step % sampleFrequency == 0) {
				// After every few step take a sample
				VotedPerceptronBasedConditionalLearning.this.computeExpectedCounts();
			}
			
			if(step % weightUpdateFrequency == 0) {
				// Update weights after a few iteration
				VotedPerceptronBasedConditionalLearning.this.updateWeights();
				super.updateUnsatStat();
			}
		}
		
		@Override
		protected void saveBestSolution() {
			for (int i = 0; i < VotedPerceptronBasedConditionalLearning.this.noOfClauses; i++) {
				VotedPerceptronBasedConditionalLearning.this.bestCounts[i] = 
						VotedPerceptronBasedConditionalLearning.this.store.noOfTrueGroundings(i);
			}
		}

	}
}
