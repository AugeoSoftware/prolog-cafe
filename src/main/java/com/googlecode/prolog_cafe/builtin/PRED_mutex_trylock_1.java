package com.googlecode.prolog_cafe.builtin;

import com.googlecode.prolog_cafe.lang.*;
import com.googlecode.prolog_cafe.lang.Predicate.P1;

import java.util.concurrent.locks.Lock;
/**

<p>mutex_trylock(+MutexId)
    As  mutex_lock/1,  but if the mutex  is held by another thread,  this
    predicates fails immediately.
    
  <p>description is copied from swi prolog documentation
 * @author semenov
 *
 */
public class PRED_mutex_trylock_1 extends P1 {

	public PRED_mutex_trylock_1(Term arg1, Operation cont) {
		this.arg1 = arg1;
		this.cont = cont;
	}

	@Override
	public Operation exec(Prolog engine) throws PrologException {
		Term a1 = arg1.dereference();

		Lock lock;
		if ((a1 instanceof SymbolTerm)){
			lock = Mutex.getInstance(a1.name());
		} else if ((a1 instanceof JavaObjectTerm) && (a1.toJava() instanceof Lock)) {
			lock = (Lock) a1.toJava();
		} else {
			throw new IllegalTypeException(this, 1, "atom or JavaObjectTerm(Lock)", a1);
		}
		if (!lock.tryLock()){
			return engine.fail();
		}
		return cont;
	}
}
