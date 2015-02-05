package starlib.mln.store.clause;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import starlib.mln.store.clause.jt.JoinTreeIntApprox;
import starlib.mln.store.internal.IntFunction;

public class CompiledStructureFactory<E extends CompiledStructure> {
	
	private Class<E> clazz;
	
	public CompiledStructureFactory(Class<E> clazz) {
		this.clazz = clazz;
	}
	
	public E create(int id, List<IntFunction> functions) {
		try {
			return clazz.getConstructor(int.class, List.class).newInstance(id, functions);
		} catch (InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static void main(String[] args) {
		CompiledStructureFactory<JoinTreeIntApprox> factory = new CompiledStructureFactory<>(JoinTreeIntApprox.class);
		JoinTreeIntApprox jt = factory.create(0, new ArrayList<IntFunction>());
		System.out.println();
	}

}
