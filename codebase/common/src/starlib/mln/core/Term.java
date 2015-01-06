package starlib.mln.core;

import java.util.ArrayList;
import java.util.List;

public class Term {

	// There is a one to one mapping between constants and the domain
	public int type;
	public List<Integer> domain = new ArrayList<Integer>();

	public Term() {
	}

	public Term(int type_, List<Integer> domain_) {
		type = type_;
		domain = new ArrayList<Integer>(domain_);
	}

	public Term(int type_, int value) {
		type = type_;
		domain = new ArrayList<Integer>(1);
		domain.add(value);
	}

	Term(Term term) {
		type = term.type;
		for (int k = 0; k < term.domain.size(); k++)
			domain.add(term.domain.get(k));
	}

}
