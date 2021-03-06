package com.googlecode.prolog_cafe.builtin;
import  com.googlecode.prolog_cafe.lang.*;
import java.lang.reflect.*;
/**
 * <code>java_set_declared_field0/3</code>
 * @author Mutsunori Banbara (banbara@kobe-u.ac.jp)
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 * @version 1.1
 */
public class PRED_java_set_declared_field0_3 extends JavaPredicate {
	private final Term arg1, arg2, arg3;

    public PRED_java_set_declared_field0_3(Term a1, Term a2, Term a3, Operation cont) {
	arg1 = a1;
	arg2 = a2;
	arg3 = a3;
	this.cont = cont;
    }

    public Operation exec(Prolog engine) {
        engine.requireFeature(Prolog.Feature.JAVA_REFLECTION, this, arg1);
        engine.setB0();

	Term a1, a2, a3;
	a1 = arg1;
	a2 = arg2;
	a3 = arg3;

	Class  clazz = null;
	Object instance = null;
	Field  field = null;
	Object value = null;

	try {
	    // 1st. argument (atom or java term)
	    a1 = a1.dereference();
	    if ((a1 instanceof VariableTerm)) {
		throw new PInstantiationException(this, 1);
	    } else if ((a1 instanceof SymbolTerm)){      // class
		clazz = Class.forName(((SymbolTerm)a1).name());
	    } else if ((a1 instanceof JavaObjectTerm)) { // instance
		instance = ((JavaObjectTerm)a1).object();
		clazz = ((JavaObjectTerm)a1).getClazz();
	    } else {
		throw new IllegalTypeException(this, 1, "atom_or_java", a1);
	    }
	    // 2nd. argument (atom)
	    a2 = a2.dereference();
	    if ((a2 instanceof VariableTerm)) {
		throw new PInstantiationException(this, 2);
	    } else if (! (a2 instanceof SymbolTerm)) {
		throw new IllegalTypeException(this, 2, "atom", a2);
	    }
	    field = clazz.getDeclaredField(((SymbolTerm)a2).name());
	    // 3rd. argument (term)
	    a3 = a3.dereference();
	    if ((a3 instanceof JavaObjectTerm))
		value = a3.toJava();
	    else
		value = a3;
	    field.setAccessible(true);
	    field.set(instance, value);
	    return cont; 
	} catch (ClassNotFoundException e) {    // Class.forName
	    throw new JavaException(this, 1, e);
	} catch (NoSuchFieldException e) {      // Class.getField(..)
	    throw new JavaException(this, 2, e);
	} catch (SecurityException e) {         // Class.getField(..)
	    throw new JavaException(this, 2, e);
	} catch (NullPointerException e) {      // Class.getField(..)
	    throw new JavaException(this, 2, e);
	} catch (IllegalAccessException e) {    // Field.get(..)
	    throw new JavaException(this, 2, e);
	} catch (IllegalArgumentException e) {  // Field.get(..)
	    throw new JavaException(this, 2, e);
	}
    }
}


