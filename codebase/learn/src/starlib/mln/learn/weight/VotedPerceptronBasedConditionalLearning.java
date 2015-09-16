package starlib.mln.learn.weight;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;

import starlib.mln.core.MLN;
import starlib.mln.core.PredicateSymbol;
import starlib.mln.core.WClause;
import starlib.mln.infer.EfficientMaxWalkSat;
import starlib.mln.store.GroundStore;
import starlib.mln.store.GroundStoreFactory;

public class VotedPerceptronBasedConditionalLearning {
	
	private static double learning_rate = 1;
	
	private static double initial_weight = 0.0001;
	
	private static int sampleFrequency = 5;
	
	private static int weightUpdateFrequency = 80;
	
	private static double weightChangeThresold = 0.0001; // 0.01% change
	
	private static double INFINITE_WEIGHT = 50; 
	
	private long timeOut = Long.MAX_VALUE;
	
	private static int maxWalkSatTimeOut = Integer.MAX_VALUE;
	
	private GroundStore store;
	
	private MLN mln;
	
	private int noOfClauses;
	
	private double[] originalCounts;
	
	private double[] expectedCounts;
	
	private double[] bestCounts;
	
	private double[] weights;
	
	private double[] averageWeights;
	
	private double maxPctDelta;
	
	private static boolean[] isQuerySymbol;
	
	private int sampleSize;
	
	private int weightUpdateCount = 1;
	
	private boolean weightsConverged = false;
	
	private static boolean isFirstRun = true;
	
	private long startTime;
	private long endTime;
	
	private int iteration = 0;
	private Calendar cal = Calendar.getInstance();
	private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
	
	public VotedPerceptronBasedConditionalLearning(String mlnFile, String dbFile, String queryFile) throws FileNotFoundException {
		startTime = System.currentTimeMillis();
		store = GroundStoreFactory.createGraphModBasedGroundStoreWithApproxCount(mlnFile, dbFile);
		mln = store.getMln();
		endTime = System.currentTimeMillis();
		System.out.println("Ground store created in : "  + (endTime - startTime) + " ms");
		
		noOfClauses = mln.numberOfClauses();
		originalCounts = new double[noOfClauses];
		expectedCounts = new double[noOfClauses];
		bestCounts     = new double[noOfClauses];
		weights        = new double[noOfClauses];
		averageWeights = new double[noOfClauses];
		isQuerySymbol = new boolean[noOfClauses];
		
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
			if(predicateSymbolIndex > -1)
				isQuerySymbol[predicateSymbolIndex] = true;
		}
		endTime = System.currentTimeMillis();
		System.out.println("Query File processed in : "  + (endTime - startTime) + " ms");
	}
	
	private void initWeights() {
		for (int i = 0; i < noOfClauses; i++) {
//			weights[i] = learning_rate * ((originalCounts[i] + initial_weight) / store.noOfGroundings(i)); 
			weights[i] = mln.getClause(i).weight.getLogValue() + initial_weight; 
		}
	}
	
	private void computeExpectedCounts() {
		for (int i = 0; i < noOfClauses; i++) {
			expectedCounts[i] += store.noOfTrueGroundings(i);
		}
		sampleSize++;
	}
	
	private void updateWeights() {
		maxPctDelta = 0;
		for (int i = 0; i < noOfClauses; i++) {
			double originalCount = originalCounts[i];
			double expectedCount = (sampleSize > 0) ? 
					(bestCounts[i] + expectedCounts[i]/sampleSize)/2 
					: bestCounts[i];
			double cluaseWiseLearningRate = learning_rate / (1 + originalCount);
			
			double deltaWeight = cluaseWiseLearningRate * (originalCount - expectedCount);
			double newWeight = weights[i] + deltaWeight;
			
			if(Math.abs(newWeight) > INFINITE_WEIGHT) {
				newWeight = Math.signum(newWeight) * INFINITE_WEIGHT;
				deltaWeight = 0;
			}
			weights[i] = newWeight;
			averageWeights[i] += newWeight;
			
			double pctDelta = Math.abs(deltaWeight / weights[i]);
			if(pctDelta > maxPctDelta) {
				maxPctDelta = pctDelta;
			}
		}
		
		if(maxPctDelta < weightChangeThresold) {
			weightsConverged = true;
		}
		
		weightUpdateCount++;
		
		Arrays.fill(expectedCounts, 0);
		sampleSize = 0;
	}
	
	private void printCurrentWeights() {
		System.out.println();
		printCurrentTime();
		System.out.println("Weights after " + weightUpdateCount + " updates -");
		for (int i = 0; i < noOfClauses; i++) {
			System.out.println(weights[i]);
		}
		System.out.println();
		System.out.println();
		System.out.println("Averaged weights after iteration : " + iteration);
		for (int i = 0; i < noOfClauses; i++) {
			System.out.printf("%.5f %n", averageWeights[i]/weightUpdateCount);
		}
		System.out.println();
		System.out.printf("Maximum percentage weight change is: %.2f %% %n", (maxPctDelta*100));
		if(weightsConverged)
			System.out.println("Weights has converged!!");
	}
	
	private void printCurrentTime() {
		System.out.println("Current time is: "+ sdf.format(cal.getTime()) );
	}
	
	public void run() {
		
		printCurrentTime();
		
		startTime = System.currentTimeMillis();
		store.update();
		endTime = System.currentTimeMillis();
		System.out.println("Ground store updated in : "  + (endTime - startTime) + " ms");
		
		for (int i = 0; i < noOfClauses; i++) {
			originalCounts[i] = store.noOfTrueGroundings(i);
		}
		
		initWeights();
		
		QueryAwareWalkSat walkSat = new QueryAwareWalkSat(store, this);
		
		long clockStartTime = System.currentTimeMillis();
		while(!weightsConverged) {
			iteration++;
			walkSat.run();
			this.updateWeights();
			this.printCurrentWeights();

			long currentTime = System.currentTimeMillis();
			
			if(currentTime - clockStartTime > timeOut) {
				System.out.println("Time out reached!!!");
				break;
			}
		}
		long currentTime = System.currentTimeMillis();
		System.out.println("Weightes converged in  " + (currentTime - clockStartTime)/1000 +" seconds.");
	}
	
	public static void main(String[] args) throws FileNotFoundException {
//		String mlnFile = "webkb-magician.mln";
//		String dbFile =  "webkb-0.txt";
//		String queryFile = "webkb_qry.txt";
//		long timeOut = 600;
//		int maxWalkSatTimeOut = 200;
		
		String mlnFile = args[0];
		String dbFile =  args[1];
		String queryFile = args[2];
		long timeOut = Long.parseLong(args[3]);
		int maxWalkSatTimeOut = Integer.parseInt(args[4]);
		
		VotedPerceptronBasedConditionalLearning learningAlgo = new VotedPerceptronBasedConditionalLearning(mlnFile, dbFile, queryFile);
		learningAlgo.timeOut = timeOut*1000;
		VotedPerceptronBasedConditionalLearning.maxWalkSatTimeOut = maxWalkSatTimeOut*1000;
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
			maxSteps = 100000;
			p = 0.6;
			randomClauseProb = 0.2;
			timeOut = maxWalkSatTimeOut;
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
