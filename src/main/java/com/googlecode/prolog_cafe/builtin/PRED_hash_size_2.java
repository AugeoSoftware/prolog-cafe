package com.googlecode.prolog_cafe.builtin;
import com.googlecode.prolog_cafe.lang.*;
import java.util.Hashtable;
/**
   <code>hash_size/2</code><br>
   @author Mutsunori Banbara (banbara@kobe-u.ac.jp)
   @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
   @version 1.0
*/
public class PRED_hash_size_2 extends Predicate.P2 {
    public PRED_hash_size_2(Term a1, Term a2, Operation cont) {
        arg1 = a1;
        arg2 = a2;
        this.cont = cont;
    }

    public Operation exec(Prolog engine) {
        engine.setB0();
        Term a1, a2;
        a1 = arg1;
        a2 = arg2;

	Object hash = null;

	a1 = a1.dereference();
	if ((a1 instanceof VariableTerm)) {
	    throw new PInstantiationException(this, 1);
	} else if ((a1 instanceof SymbolTerm)) {
	    if (! engine.getHashManager().containsKey(a1))
		throw new ExistenceException(this, 1, "hash", a1, "");
	    hash = ((JavaObjectTerm) engine.getHashManager().get(a1)).object();
	} else if ((a1 instanceof JavaObjectTerm)) {
	    hash = ((JavaObjectTerm) a1).object();
	} else {
	    throw new IllegalDomainException(this, 1, "hash_or_alias", a1);
	}
	if (! (hash instanceof HashtableOfTerm))
	    throw new InternalException(this + ": Hash is not HashtableOfTerm");
	a2 = a2.dereference();
	if (! (a2 instanceof VariableTerm) && ! (a2 instanceof IntegerTerm))
	    throw new IllegalTypeException(this, 1, "integer", a2);
	if (! a2.unify(new IntegerTerm(((HashtableOfTerm)hash).size()), engine.trail))
	    return engine.fail();
        return cont;
    }
}
