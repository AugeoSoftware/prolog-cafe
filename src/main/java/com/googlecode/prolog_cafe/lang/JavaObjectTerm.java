package com.googlecode.prolog_cafe.lang;
/**
 * Java-term.<br>
 * The <code>JavaObjectTerm</code> class wraps a java object.<br>
 * 
 * <pre>
 *  import java.util.Hashtable;
 *  Term t = new JavaObjectTerm(new Hashtable());
 *  Hashtable hash = (Hashtable)(((JavaObjectTerm)t).object());
 * </pre>
 *
 * @author Mutsunori Banbara (banbara@kobe-u.ac.jp)
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 * @version 1.0
 */
public class JavaObjectTerm extends Term {
    /** Holds a java object that this <code>JavaObjectTerm</code> wraps. */
    protected Object obj;

    /** Constructs a new Prolog java-term that wraps the argument object. */
    public JavaObjectTerm(Object _obj) {
    	if ( _obj==null) {
    		throw new NullPointerException("Error: constructing JavaObjectTerm around null");
    	}
    	obj   = _obj;
    }

    /** Sets the argument object to this <code>JavaObjectTerm</code>. */
    public void setObject(Object _obj) {
    	if ( _obj==null) {
    		throw new NullPointerException("Error: JavaObjectTerm can not wrap null");
    	}
    	obj   = _obj;
    }

    /** Returns the object wrapped by this <code>JavaObjectTerm</code>. */
    public Object  object() { return obj; }

    /** Returns a <code>java.lang.Class</code> of object wrapped by this <code>JavaObjectTerm</code>. */
    public Class   getClazz() { return obj.getClass(); }

    public String name() { return ""; }
    
    @Override
    public String toQuotedString() { return toString(); }
    @Override
    public void toQuotedString(StringBuilder sb) { toString(sb); }

    /* Term */
    public boolean unify(Term t, Trail trail) {
    	t = t.dereference();
    	return (t instanceof VariableTerm) ? ((VariableTerm)t).bind(this, trail) :
    		((t instanceof JavaObjectTerm) && obj==(((JavaObjectTerm)t).obj));
    }

    /** 
     * Check whether the wrapped object is convertible with the given Java class type.
     * @return the <code>boolean</code> whose value is
     * <code>convertible(getClazz(), type)</code>.
     * @see #getClazz()
     * @see Term#convertible(Class, Class)
     */
    public boolean convertible(Class type) { return convertible(obj.getClass(), type); }

    /** 
     * Returns the object wrapped by this <code>JavaObjectTerm</code>.
     * @return the value of <code>obj</code>.
     * @see #obj
     */
    public Object toJava() { return obj; }

    /* Object */
    /**
     * Checks <em>term equality</em> of two terms.
     * The result is <code>true</code> if and only if the argument is an instance of
     * <code>JavaObjectTerm</code>, and 
     * both terms point to the same java object.
     * @param o the object to compare with. This must be dereferenced.
     * @return <code>true</code> if the given object represents a java-term
     * equivalent to this <code>JavaObjectTerm</code>, false otherwise.
     * @see #compareTo
     */
    public boolean equals(Object o) {
		return o instanceof JavaObjectTerm && obj==(((JavaObjectTerm) o).obj);
	}

    public int hashCode() {
    	return System.identityHashCode(obj);
    }

    /** Returns a string representation of this <code>JavaObjectTerm</code>. */
    @Override
    public String toString() {
		return obj.getClass().getName()
	      + "(0x" + Integer.toHexString(hashCode()) + ")";
    }
    
    /** Adds a string representation of this <code>JavaObjectTerm</code> to given StringBuilder instance. */
    @Override
    public void toString(StringBuilder sb) {
		sb.append(obj.getClass().getName());
	    sb.append("(0x"); 
	    sb.append(Integer.toHexString(hashCode()));
	    sb.append(")");
    }

    /* Comparable */
    /** 
     * Compares two terms in <em>Prolog standard order of terms</em>.<br>
     * It is noted that <code>t1.compareTo(t2) == 0</code> has the same
     * <code>boolean</code> value as <code>t1.equals(t2)</code>.
     * @param anotherTerm the term to compared with. It must be dereferenced.
     * @return the value <code>0</code> if two terms are identical; 
     * a value less than <code>0</code> if this term is <em>before</em> the <code>anotherTerm</code>;
     * and a value greater than <code>0</code> if this term is <em>after</em> the <code>anotherTerm</code>.
     */
    public int compareTo(Term anotherTerm) { // anotherTerm must be dereferenced.
		if ((anotherTerm instanceof VariableTerm)
		    || (anotherTerm instanceof NumberTerm)
		    || (anotherTerm instanceof SymbolTerm)
		    || (anotherTerm instanceof ListTerm)
		    || (anotherTerm instanceof StructureTerm))
		    return AFTER;
		if (! (anotherTerm instanceof JavaObjectTerm))
		    return BEFORE;
		if (obj==(((JavaObjectTerm) anotherTerm).obj))
		    return EQUAL;
		return obj.hashCode() - ((JavaObjectTerm) anotherTerm).obj.hashCode(); //???
    }

	@Override
	public final boolean isImmutable() {
		return true; // FIXME this.obj is not final
	}
}
