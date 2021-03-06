package com.googlecode.prolog_cafe.builtin;

import com.googlecode.prolog_cafe.lang.*;

import java.io.File;

/** {@code make_directory(+Dir)} */
public class PRED_make_directory_1 extends Predicate.P1 {
  public PRED_make_directory_1(Term a1, Operation next) {
    arg1 = a1;
    cont = next;
  }

  @Override
  public Operation exec(Prolog engine) throws PrologException {
    engine.requireFeature(Prolog.Feature.IO, this, arg1);
    engine.setB0();

    Term a1 = arg1.dereference();
    if ((a1 instanceof VariableTerm)) throw new PInstantiationException(this, 1);
    if (!(a1 instanceof SymbolTerm)) throw new IllegalDomainException(this, 1, "dir", a1);

    File file = new File(((SymbolTerm) a1).name());
    if (file.mkdir())
      return cont;
    else
      throw new PermissionException(this, "open", "dir", a1, "cannot create");
  }
}
