package starlib.mln.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MLN {

	public int max_predicate_id;
	public int maxDegree;
	public List<PredicateSymbol> symbols = new ArrayList<PredicateSymbol>();
	public List<WClause> clauses = new ArrayList<WClause>();
	public List<Formula> formulas = new ArrayList<Formula>();

	public List<WClause> evidence = new ArrayList<WClause>();
	
	public static WClause create_new_clause(WClause clause) {

		WClause new_clause = new WClause();
		new_clause.sign = new ArrayList<Boolean>(clause.sign);
		new_clause.satisfied = clause.satisfied;
		new_clause.weight = clause.weight;

		//if atoms have common terms their relationship must be maintained when new clause is created
		List<Term> newTerms = new ArrayList<Term>();
		List<Term> oldTerms = new ArrayList<Term>();

		for (int i = 0; i < clause.atoms.size(); i++) 
		{
			for (int j = 0; j < clause.atoms.get(i).terms.size(); j++)
			{
				int termPosition=-1;
				for(int m=0;m<oldTerms.size();m++)
				{
					if(oldTerms.get(m)==clause.atoms.get(i).terms.get(j))
					{
						termPosition = m;
					}
				}
				if(termPosition==-1)
				{
					Term term = new Term();
					term.type = clause.atoms.get(i).terms.get(j).type;
					for(int k=0;k<clause.atoms.get(i).terms.get(j).domain.size();k++)
						term.domain.add(clause.atoms.get(i).terms.get(j).domain.get(k));
					newTerms.add(term);
					oldTerms.add(clause.atoms.get(i).terms.get(j));
				}
				else
				{
					newTerms.add(newTerms.get(termPosition));
					oldTerms.add(clause.atoms.get(i).terms.get(j));
				}
			}
		}
		int ind=0;
		new_clause.atoms = new ArrayList<Atom>(clause.atoms.size());
		for (int i = 0; i < clause.atoms.size(); i++) {
			new_clause.atoms.add(new Atom());
			new_clause.atoms.get(i).symbol = create_new_symbol(clause.atoms.get(i).symbol);
			new_clause.atoms.get(i).terms = new ArrayList<Term>(clause.atoms.get(i).terms.size());
			for (int j = 0; j < clause.atoms.get(i).terms.size(); j++) {
				new_clause.atoms.get(i).terms.add(newTerms.get(ind));
				ind++;
			}
		}
		return new_clause;
	}

	public static Atom create_new_atom(Atom atom) {
		List<Term> terms = new ArrayList<Term>();
		for(int i=0;i<atom.terms.size();i++)
		{
			Term newTerm = create_new_term(atom.terms.get(i));
			terms.add(newTerm);
		}
		//Atom newAtom = new Atom(atom.symbol,terms);
		Atom newAtom = new Atom(create_new_symbol(atom.symbol),terms);
		return newAtom;
	}

	public static Term create_new_term(Term term) {
		int type = term.type;
		List<Integer> domain = new ArrayList<Integer>();
		for(int k=0;k<term.domain.size();k++)
			domain.add(term.domain.get(k));
		return new Term(type,domain);
	}
	public static PredicateSymbol create_new_symbol(PredicateSymbol symbol) {
		List<Integer> var_types = new ArrayList<Integer>(symbol.variable_types);
		PredicateSymbol newSymbol = new PredicateSymbol(symbol.id,symbol.symbol,var_types,symbol.pweight,symbol.nweight);
		newSymbol.parentId = symbol.parentId;
		return newSymbol;
	}

	public static void toNormalForm(PredicateSymbol symbol,List<WClause> clauses) {


		List<WClause> new_clauses = new ArrayList<WClause>();
		List<WClause> symbol_clauses = new ArrayList<WClause>();
		List<Integer> symbol_loc = new ArrayList<Integer>();
		for (int i = 0; i < clauses.size(); i++) {
			boolean relevant = false;
			for (int j = 0; j < clauses.get(i).atoms.size(); j++) {
				if (clauses.get(i).atoms.get(j).symbol.id == symbol.id) 
				{
					symbol_clauses.add(create_new_clause(clauses.get(i)));
					symbol_loc.add(j);
					relevant = true;
					break;
				}
			}
			if (!relevant)
				new_clauses.add(create_new_clause(clauses.get(i)));
		}
		for (int i = 0; i < symbol.variable_types.size(); i++) {
			boolean changed = true;
			while (changed) {
				changed = false;
				for (int a = 0; a < symbol_clauses.size(); a++) {
					if (!symbol_clauses.get(a).valid()) continue;
					for (int b = a + 1; b < symbol_clauses.size(); b++) {
						if (!symbol_clauses.get(b).valid()) continue;
						int j = symbol_loc.get(a);
						int k = symbol_loc.get(b);

						Set<Integer> set1 = new HashSet<Integer>(symbol_clauses.get(a).atoms.get(j).terms.get(i).domain);
						Set<Integer> set2 = new HashSet<Integer>(symbol_clauses.get(b).atoms.get(k).terms.get(i).domain);

						// if the domains are disjoint
						Set<Integer> intersection = new HashSet<Integer>(set1);
						intersection.retainAll(set2);

						if (set1.equals(set2) || intersection.isEmpty()) {
							continue;
						} else {
							changed = true;
							WClause clause1 = create_new_clause(symbol_clauses.get(a));
							WClause clause2 = create_new_clause(symbol_clauses.get(b));

							Set<Integer> difference12 = new HashSet<Integer>(set1);
							difference12.removeAll(set2);
							clause1.atoms.get(j).terms.get(i).domain = new ArrayList<Integer>(difference12);

							Set<Integer> difference21 = new HashSet<Integer>(set2);
							difference21.removeAll(set1);
							clause2.atoms.get(k).terms.get(i).domain = new ArrayList<Integer>(difference21);

							// do set intersection
							set1.retainAll(set2);

							set2 = set1;
							symbol_clauses.add(clause1);
							symbol_loc.add(j);
							symbol_clauses.add(clause2);
							symbol_loc.add(k);
							break;
						}
					}
					if (changed)
						break;
				}
			}
		}
		for (int i = 0; i < symbol_clauses.size(); i++){
			if(symbol_clauses.get(i).valid())
				new_clauses.add(create_new_clause(symbol_clauses.get(i)));
		}

		//cleanup old clauses that are no longer used
		clauses.clear();
		clauses.addAll(new_clauses);
	}

	public static void copyAllClauses(List<WClause> origClauses, List<WClause> newClauses) {
		for(int i=0;i<origClauses.size();i++)
		{
			WClause newClause = create_new_clause(origClauses.get(i));
			newClauses.add(newClause);
		}
	}
	public static void print(List<WClause> clauses,String banner)
	{
		StringBuilder tmp = new StringBuilder("###########");
		tmp.append(banner);
		tmp.append("-START");
		tmp.append("###########");
		System.out.println(tmp);
		for(int i=0;i<clauses.size();i++)
			clauses.get(i).print();

		StringBuilder tmp1 = new StringBuilder("###########");
		tmp1.append(banner);
		tmp1.append("-END");
		tmp1.append("###########");
		System.out.println(tmp1);
	}

	public void setMaxPredicateId(int Id)
	{
		max_predicate_id = Math.max(max_predicate_id,Id);
	}

	public int getMaxPredicateId()
	{
		if(max_predicate_id == 0)
			setMaxPredicateId(symbols.size());
		return max_predicate_id;
	}

	//	void preprocessEvidence();
	public void putWeightsOnClauses() {

		for(int i=0;i<formulas.size();i++)
		{
			//cout<<mln.formulas.get(i).weight<<endl;
			//FOR NOW EACH FORMULA HAS EXACTLY ONE CLAUSE
			clauses.get(i).weight = formulas.get(i).weight;
		}
	}

	public void clearData()
	{
		symbols.clear();
		clauses.clear();
		formulas.clear();
		maxDegree = -1;
		max_predicate_id = 0;
	}

	public void setMLNPoperties()
	{
		//max degree of MLN
		int maxterms = 0;
		for(int i=0;i<symbols.size();i++)
		{
			if(symbols.get(i).variable_types.size() > maxterms)
				maxterms = symbols.get(i).variable_types.size();
		}
		maxDegree = maxterms;
	}

	public int getMaxDegree()
	{
		if(maxDegree == -1)
		{
			setMLNPoperties();
		}
		return maxDegree;
	}

	public MLN() {
		max_predicate_id = (0);
		maxDegree = (-1);
	}
}
