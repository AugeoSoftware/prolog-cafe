package com.googlecode.prolog_cafe.lang;
/**
 * System error.<br>
 *
 * @author Mutsunori Banbara (banbara@kobe-u.ac.jp)
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 * @version 1.0
 */
public class SystemException extends PrologException {
    /** A functor symbol of <code>system_error/1</code>. */
    public static final SymbolTerm SYSTEM_ERROR = SymbolTerm.intern("system_error", 1);

    /** Holds a message. */
    public final String message;

    /** Constructs a new <code>SystemException</code> with a message. */
    public SystemException(String _message) {
	message = _message;
    }

    /** Returns a term representation of this <code>SystemException</code>:
     * <code>system_error(message)</code>.
     */
    public Term getMessageTerm() {
	Term[] args = {SymbolTerm.create(message)};
	return new ErrorTerm(this, SYSTEM_ERROR, args);
    }

    /** Returns a string representation of this <code>SystemException</code>. */
    public String toString() {
	String s = "{SYSTEM ERROR: " + message;
	s += "}";
	return s;
    }
}
