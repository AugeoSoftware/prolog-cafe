package com.googlecode.prolog_cafe.lang;
import java.io.*;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * Prolog engine.
 *
 * @author Mutsunori Banbara (banbara@kobe-u.ac.jp)
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 * @version 1.2
 */
public final class Prolog {

	private final PrologLogger logger;

	private final static Logger javaUtilLogger = Logger.getLogger(Prolog.class.getName());

	private static final SymbolTerm NONE = SymbolTerm.intern("$none");

	private final ConcurrentMap<String, Object> externalData = new ConcurrentHashMap<>();

	/** Prolog thread */
	public final PrologControl control;

	/** Argument registers */
	public Term areg1, areg2, areg3, areg4, areg5, areg6, areg7, areg8;
	public Term[] aregs;
	private static final Term[] NO_REGISTERS = {};

	/** Continuation goal register */
	public Operation cont;
	/** Choice point frame stack */
	public final ChoicePointStack stack;
	/** Trail stack */
	public final Trail trail;
	/** Cut pointer */
	public int B0;
	/** Class loader */
	public PrologClassLoader pcl;
	/** Internal Database */
	public InternalDatabase internalDB;

	/** Current time stamp of choice point frame */
	private long CPFTimeStamp;
	/** Stack for keeping B value of error catchers */
	private int[] catchersB = new int[256];
	private int catchersBindex = -1;

	/**
	 * Exception level of continuation passing loop:
	 * <li><code>0</code> for no exception,
	 * <li><code>1</code> for <code>halt/0</code>,
	 * <li><code>1+N</code> for <code>halt(N)</code>.
	 * </ul>
	 */
	public int halt;

	/** <font color="red">Not supported yet</font>. Prolog implementation flag: <code>bounded</code>. */
	private final boolean bounded = false;
	/** Prolog implementation flag: <code>max_integer</code>. */
	private static final int maxInteger = Integer.MAX_VALUE;
	/** Prolog implementation flag: <code>min_integer</code>. */
	private static final int minInteger = Integer.MIN_VALUE;
	/** Prolog implementation flag: <code>integer_rounding_function</code>. */
	private final String integerRoundingFunction = "down";
	/** <font color="red">Not supported yet</font>. Prolog implementation flag: <code>char_conversion</code>. */
	private String charConversion;
	/** Prolog implementation flag: <code>debug</code>. */
	private String debug;
	/** Prolog implementation flag: <code>max_arity</code>. */
	int maxArity = 255;
	/** Prolog implementation flag: <code>unknown</code>. */
	private String unknown;
	/** <font color="red">Not supported yet</font>. Prolog implementation flag: <code>double_quotes</code>. */
	private String doubleQuotes;
	/** Prolog implementation flag: <code>print_stack_trace</code>. */
	private String printStackTrace;

	/** Holds an exception term for <code>catch/3</code> and <code>throw/1</code>. */
	private Term exception;

	/** Holds the start time as <code>long</code> for <code>statistics/2</code>. */
	private long startRuntime;
	/** Holds the previous time as <code>long</code> for <code>statistics/2</code>. */
	private long previousRuntime;

//    /** Hashtable for creating a copy of term. */
//    protected IdentityHashMap<VariableTerm,VariableTerm> copyHash;

	/** The size of the pushback buffer used for creating input streams. */
	public static final int PUSHBACK_SIZE = 256;

	/** Standard input stream. */
	private transient PushbackReader userInput;
	/** Standard output stream. */
	private transient PrintWriter userOutput;
	/** Standard error stream. */
	private transient PrintWriter userError;
	/** Current input stream. */
	private transient PushbackReader currentInput;
	/** Current output stream. */
	private transient PrintWriter currentOutput;
	/** Hashtable for managing input and output streams. */
	private HashtableOfTerm streamManager;

	/** Hashtable for managing internal databases. */
	private final HashtableOfTerm hashManager;

	/** Name of the builtin package. */
	public static final String BUILTIN = "com.googlecode.prolog_cafe.builtin";

	/** Holds an atom <code>[]<code> (empty list). */
	public static final SymbolTerm Nil     = SymbolTerm.intern("[]");

	/* Some symbols for stream options */
	private static final SymbolTerm SYM_MODE_1     = SymbolTerm.intern("mode", 1);
	private static final SymbolTerm SYM_ALIAS_1    = SymbolTerm.intern("alias", 1);
	private static final SymbolTerm SYM_TYPE_1     = SymbolTerm.intern("type", 1);
	private static final SymbolTerm SYM_READ       = SymbolTerm.intern("read");
	private static final SymbolTerm SYM_APPEND     = SymbolTerm.intern("append");
	private static final SymbolTerm SYM_INPUT      = SymbolTerm.intern("input");
	private static final SymbolTerm SYM_OUTPUT     = SymbolTerm.intern("output");
	private static final SymbolTerm SYM_TEXT       = SymbolTerm.intern("text");
	private static final SymbolTerm SYM_USERINPUT  = SymbolTerm.intern("user_input");
	private static final SymbolTerm SYM_USEROUTPUT = SymbolTerm.intern("user_output");
	private static final SymbolTerm SYM_USERERROR  = SymbolTerm.intern("user_error");

	private static final PrintWriter NO_OUTPUT = new PrintWriter(new Writer() {
		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {
			throw new IOException("Prolog.Feature.IO disabled");
		}

		@Override
		public void flush() throws IOException {
		}

		@Override
		public void close() throws IOException {
		}
	});

	private static final PushbackReader NO_INPUT = new PushbackReader(new Reader() {
		@Override
		public int read(char[] cbuf, int off, int len) throws IOException {
			return -1;
		}

		@Override
		public void close() throws IOException {
		}
	});

	public enum Feature {
		/** Enable the {@code java_*} predicates, supporting reflection in Prolog. */
		JAVA_REFLECTION,

		/** Access to the local filesystem and console. */
		IO,

		/** Track the running time of evaluations */
		STATISTICS_RUNTIME
	}
	protected final EnumSet<Feature> features = EnumSet.allOf(Feature.class);

	Prolog(PrologControl c) {
		logger = new PrologLogger(javaUtilLogger);
		control = c;
		trail = new Trail();
		stack = new ChoicePointStack(trail);
//      copyHash = new IdentityHashMap<VariableTerm, VariableTerm>();
		hashManager = new HashtableOfTerm();
	}

	Prolog(PrologControl c, PrologMachineCopy pmc) {
		logger = new PrologLogger(javaUtilLogger);
		control = c;
		trail = new Trail();
		stack = new ChoicePointStack(trail);
//      copyHash = new IdentityHashMap<VariableTerm, VariableTerm>();
		pcl = pmc.pcl;

		// During restore there is no need to copy terms. clause/2 inside of
		// builtins.pl copies the predicate when it reads from internalDB.
		hashManager = PrologMachineCopy.copyShallow(pmc.hashManager);
		internalDB = new InternalDatabase(pmc.internalDB, false, new IdentityHashMap<VariableTerm, VariableTerm>());
	}

	/**
	 * Initializes some local instances only once.
	 * This <code>initOnce</code> method is invoked in the constructor
	 * and initializes the following instances:
	 * <ul>
	 *   <li><code>userInput</code>
	 *   <li><code>userOutput</code>
	 *   <li><code>userError</code>
	 *   <li><code>copyHash</code>
	 *   <li><code>streamManager</code>
	 * </ul>
	 */
	private void initOnce(InputStream in, PrintStream out, PrintStream err) {
		if (8 < maxArity)
			aregs = new Term[maxArity - 8];
		else
			aregs = NO_REGISTERS;

		if (pcl == null) pcl = new PrologClassLoader();
		if (internalDB == null) internalDB = new InternalDatabase();

		streamManager = new HashtableOfTerm(7);

		if (features.contains(Feature.IO)) {
			userInput = new PushbackReader(new BufferedReader(new InputStreamReader(in)), PUSHBACK_SIZE);
			userOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(out)), true);
			userError = new PrintWriter(new OutputStreamWriter(err), true);

			streamManager.put(SYM_USERINPUT, new JavaObjectTerm(userInput));
			streamManager.put(new JavaObjectTerm(userInput),
					makeStreamProperty(SYM_READ, SYM_INPUT, SYM_USERINPUT, SYM_TEXT));

			streamManager.put(SYM_USEROUTPUT, new JavaObjectTerm(userOutput));
			streamManager.put(new JavaObjectTerm(userOutput),
					makeStreamProperty(SYM_APPEND, SYM_OUTPUT, SYM_USEROUTPUT, SYM_TEXT));

			streamManager.put(SYM_USERERROR, new JavaObjectTerm(userError));
			streamManager.put(new JavaObjectTerm(userError),
					makeStreamProperty(SYM_APPEND, SYM_OUTPUT, SYM_USERERROR, SYM_TEXT));
		} else {
			userInput = NO_INPUT;
			userOutput = NO_OUTPUT;
			userError = userOutput;
		}
	}

	/** Initializes this Prolog engine. */
	public void init(InputStream in, PrintStream out, PrintStream err) {
		if (aregs == null)
			initOnce(in,out,err);
		stack.init();
		trail.init();
		B0 = stack.top();
		CPFTimeStamp = Long.MIN_VALUE;

		// Creates an initial choice point frame.
		//ChoicePointFrame initialFrame = new ChoicePointFrame(this, Failure.FAILURE, ++CPFTimeStamp);  //ChoicePointFrame.S0(null);
		trail.timeStamp = ++CPFTimeStamp;
		stack.push(this, Failure.FAILURE, ChoicePointStack::restore0);
		logger.init(stack.top);

		halt = 0;

		charConversion  = "off";
		debug           = "off";
		unknown         = "error";
		doubleQuotes    = "codes";
		printStackTrace = "off";

		exception = NONE;
		startRuntime = features.contains(Feature.STATISTICS_RUNTIME)
				? System.currentTimeMillis()
				: 0;
		previousRuntime = 0;

		currentInput  = userInput;
		currentOutput = userOutput;
	}

	/** Ensure a feature is enabled, throwing if not. */
	public void requireFeature(Prolog.Feature f, Operation goal, Term arg) {
		if (!features.contains(f)) {
			throw new PermissionException(goal, "use", f.toString().toLowerCase(), arg, "disabled");
		}
	}

	public void pushCatcherB(int b){
		catchersBindex++;
		if (catchersBindex>=catchersB.length){
			ensureCatchersCapability();
		}
		catchersB[catchersBindex] = b;
	}

	private void ensureCatchersCapability() {
		int[] newCatchersB = new int[catchersB.length<<1];
		System.arraycopy(catchersB, 0, newCatchersB, 0, catchersB.length);
		catchersB = newCatchersB;
	}

	public int popCatcherB(){
		return (catchersBindex>=0) ? catchersB[catchersBindex--] : -1;
	}

	public int peekCatcherB() {
		return (catchersBindex>=0) ? catchersB[catchersBindex] : -1;
	}

	/** Sets B0 to the top of the choice point stack.. */
	public void setB0()    { B0 = stack.top(); }

	/** Discards all choice points after the value of <code>i</code>. */
	public void cut(int i) { stack.cut(i); }

	/** Discards all choice points after the value of <code>B0</code>. */
	public void neckCut()  { stack.cut(B0); }

	/**
	 * Returns a copy of term <code>t</code>.
	 * @param t a term to be copied. It must be dereferenced.
	 */
	public Term copy(Term t) {
		if (t.isImmutable()){
			return t;
		} else {
			//copyHash.clear();
//    		copyHash = new IdentityHashMap<VariableTerm, VariableTerm>();
			return t.copy(new IdentityHashMap<VariableTerm, VariableTerm>());
		}
	}

	/**
	 * Do backtrak.
	 * This method restores the value of <code>B0</code>
	 * and returns the backtrak point in current choice point.
	 */
	public Operation fail() {
		ChoicePointFrame top = stack.top;
    	logger.fail(top.bp, top);
		B0 = top.b0;     // restore B0
		return top.bp;   // execute next clause
	}

	/**
	 * Returns the <code>Predicate</code> object refered, respectively,
	 * <code>var</code>, <code>Int</code>, <code>flo</code>,
	 * <code>con</code>, <code>str</code>, or <code>lis</code>,
	 * depending on whether the dereferenced value of argument
	 * register <code>areg[1]</code> is a
	 * variable, integer, float,
	 * atom, compound term, or non-empty list, respectively.
	 */
	public Operation switch_on_term(Operation var,
									Operation Int,
									Operation flo,
									Operation con,
									Operation str,
									Operation lis) {
		Term arg1 = areg1.dereference();
		if (arg1 instanceof VariableTerm)
			return var;
		if (arg1 instanceof IntegerTerm)
			return Int;
		if (arg1 instanceof DoubleTerm)
			return flo;
		if (arg1 instanceof SymbolTerm)
			return con;
		if (arg1 instanceof StructureTerm)
			return str;
		if (arg1 instanceof ListTerm)
			return lis;
		return var;
	}

	/**
	 * If the dereferenced value of arugment register <code>areg[1]</code>
	 * is an integer, float, atom, or compound term (except for non-empty list),
	 * this returns the <code>Predicate</code> object to which its key is mapped
	 * in hashtable <code>hash</code>.
	 *
	 * The key is calculated as follows:
	 * <ul>
	 *   <li>integer - itself
	 *   <li>float - itself
	 *   <li>atom - itself
	 *   <li>compound term - functor/arity
	 * </ul>
	 *
	 * If there is no mapping for the key of <code>areg[1]</code>,
	 * this returns <code>otherwise</code>.
	 */
	public Operation switch_on_hash(HashMap<Term,Operation> hash, Operation otherwise) {
		Term arg1 = areg1.dereference();
		Term key;
		if (((arg1 instanceof IntegerTerm) || arg1 instanceof DoubleTerm) || (arg1 instanceof SymbolTerm)) {
			key = arg1;
		} else if ((arg1 instanceof StructureTerm)) {
			key = ((StructureTerm) arg1).functor();
		} else {
			throw new SystemException("Invalid argument in switch_on_hash");
		}
		Operation p = hash.get(key);
		if (p != null)
			return p;
		else
			return otherwise;
	}

// --Commented out by Inspection START (03.04.2017 11:14):
//	/** Restores the argument registers and continuation goal register from the current choice point frame. */
//	public final void restore() {
//		stack.top.restore(this);
//	}
// --Commented out by Inspection STOP (03.04.2017 11:14)

	/** Creates a new choice point frame. */
	public Operation jtry0(Operation p, Operation next) {
		trail.timeStamp = ++CPFTimeStamp;
		logger.jtry(p, next, stack.push(this, next, ChoicePointStack::restore0));
		return p;
	}
	public Operation jtry1(Operation p, Operation next) {
		trail.timeStamp = ++CPFTimeStamp;
		logger.jtry(p, next, stack.push(this, areg1, next));
		return p;
	}
	public Operation jtry2(Operation p, Operation next) {
		trail.timeStamp = ++CPFTimeStamp;
		logger.jtry(p, next, stack.push(this, areg1, areg2, next));
		return p;
	}
	public Operation jtry3(Operation p, Operation next) {
		trail.timeStamp = ++CPFTimeStamp;
		logger.jtry(p, next, stack.push(this, areg1, areg2, areg3, next));
		return p;
	}

	public Operation jtry4(Operation p, Operation next) {
		trail.timeStamp = ++CPFTimeStamp;
		logger.jtry(p, next, stack.push(this, areg1, areg2, areg3, areg4, next));
		return p;
	}

	public Operation jtry5(Operation p, Operation next) {
		trail.timeStamp = ++CPFTimeStamp;
		logger.jtry(p, next, stack.push(this, areg1, areg2, areg3, areg4, areg5, next));
		return p;
	}

	public Operation jtry6(Operation p, Operation next) {
		trail.timeStamp = ++CPFTimeStamp;
		logger.jtry(p, next, stack.push(this, areg1, areg2, areg3, areg4, areg5, areg6, next));
		return p;
	}

	public Operation jtry7(Operation p, Operation next) {
		trail.timeStamp = ++CPFTimeStamp;
		logger.jtry(p, next, stack.push(this, areg1, areg2, areg3, areg4, areg5, areg6, areg7, next));
		return p;
	}

	public Operation jtry8(Operation p, Operation next) {
		trail.timeStamp = ++CPFTimeStamp;
		logger.jtry(p, next, stack.push(this, areg1, areg2, areg3, areg4, areg5, areg6, areg7, areg8, next));
		return p;
	}

	public Operation jtry(int arity, Operation p, Operation next) {
		trail.timeStamp = ++CPFTimeStamp;
		logger.jtry(p, next, stack.push(this, arity, next));
		return p;
	}

//    private Operation finishjtry(Operation p, Operation next, ChoicePointFrame entry) {
//      entry.b0 = B0;
//      entry.bp = next;
//      entry.tr = trail.top();
//      entry.timeStamp = ++CPFTimeStamp;
//      stack.push(entry);
////      logger.jtry(p,next,entry);
//      return p;
//    }

	/**
	 * Resets all necessary information from the current choice point frame,
	 * updates its next clause field to <code>next</code>,
	 * and then returns <code>p</code>.
	 */
	public Operation retry(Operation p, Operation next) {
		ChoicePointFrame top = stack.top;
		top.restore.accept(top,this);
		logger.retry(p, next, top);
		trail.unwind(top.tr);
		top.bp = next;
		return p;
	}

	/**
	 * Resets all necessary information from the current choice point frame,
	 * discard it, and then returns <code>p</code>.
	 */
	public Operation trust(Operation p) {
		final ChoicePointFrame top = stack.top;
		top.restore.accept(top,this);
		logger.trust(p, top);
		trail.unwind(top.tr);
		stack.delete();
		return p;
	}

	private Term makeStreamProperty(SymbolTerm _mode, SymbolTerm io, SymbolTerm _alias, SymbolTerm _type) {
		Term[] mode  = {_mode};
		Term[] alias = {_alias};
		Term[] type  = {_type};

		Term t = Nil;
		t = new ListTerm(new StructureTerm(SYM_MODE_1,  mode ), t);
		t = new ListTerm(io, t);
		t = new ListTerm(new StructureTerm(SYM_ALIAS_1, alias), t);
		t = new ListTerm(new StructureTerm(SYM_TYPE_1,  type ), t);
		return t;
	}

	/** Returns the current time stamp of choice point frame. */
	public final long getCPFTimeStamp() { return CPFTimeStamp; }

	/** Returns the value of Prolog implementation flag: <code>bounded</code>. */
	public boolean isBounded() { return bounded; }

	/** Returns the value of Prolog implementation flag: <code>max_integer</code>. */
	public int getMaxInteger() { return maxInteger; }

	/** Returns the value of Prolog implementation flag: <code>min_integer</code>. */
	public int getMinInteger() { return minInteger; }

	/** Returns the value of Prolog implementation flag: <code>integer_rounding_function</code>. */
	public String getIntegerRoundingFunction() { return integerRoundingFunction; }

	/** Returns the value of Prolog implementation flag: <code>char_conversion</code>. */
	public String getCharConversion() { return charConversion; }
	/** Sets the value of Prolog implementation flag: <code>char_conversion</code>. */
	public void setCharConversion(String mode) { charConversion = mode;}

	/** Returns the value of Prolog implementation flag: <code>debug</code>. */
	public String getDebug() { return debug; }
	/** Sets the value of Prolog implementation flag: <code>debug</code>. */
	public void setDebug(String mode) { debug = mode;}

	/** Returns the value of Prolog implementation flag: <code>max_arity</code>. */
	public int getMaxArity() { return maxArity; }

	/** Returns the value of Prolog implementation flag: <code>unknown</code>. */
	public String getUnknown() { return unknown; }
	/** Sets the value of Prolog implementation flag: <code>unknown</code>. */
	public void setUnknown(String mode) { unknown = mode;}

	/** Returns the value of Prolog implementation flag: <code>double_quotes</code>. */
	public String getDoubleQuotes() { return doubleQuotes; }
	/** Sets the value of Prolog implementation flag: <code>double_quotes</code>. */
	public void setDoubleQuotes(String mode) { doubleQuotes = mode;}

	/** Returns the value of Prolog implementation flag: <code>print_stack_trace</code>. */
	public String getPrintStackTrace() { return "on"; /*return printStackTrace;*/ }
	/** Sets the value of Prolog implementation flag: <code>print_stack_trace</code>. */
	public void setPrintStackTrace(String mode) { printStackTrace = mode;}

	/** Returns the value of <code>exception</code>. This is used in <code>catch/3</code>. */
	public Term getException() { return exception; }
	/** Sets the value of <code>exception</code>. This is used in <code>throw/1</code>. */
	public void setException(Term t) { exception = t;}

	/** Returns the value of <code>startRuntime</code>. This is used in <code>statistics/2</code>. */
	public long getStartRuntime() { return startRuntime; }

	/** Returns the value of <code>previousRuntime</code>. This is used in <code>statistics/2</code>. */
	public long getPreviousRuntime() { return previousRuntime; }
	/** Sets the value of <code>previousRuntime</code>. This is used in <code>statistics/2</code>. */
	public void setPreviousRuntime(long t) { previousRuntime = t; }

	/** Returns the standard input stream. */
	public PushbackReader  getUserInput() { return userInput; }
	/** Returns the standard output stream. */
	public PrintWriter     getUserOutput() { return userOutput; }
	/** Returns the standard error stream. */
	public PrintWriter     getUserError() { return userError; }

	/** Returns the current input stream. */
	public PushbackReader  getCurrentInput() { return currentInput; }
	/** Sets the current input stream to <code>in</code>. */
	public void            setCurrentInput(PushbackReader in) { currentInput = in; }

	/** Returns the current output stream. */
	public PrintWriter     getCurrentOutput() { return currentOutput; }
	/** Sets the current output stream to <code>out</code>. */
	public void            setCurrentOutput(PrintWriter out) { currentOutput = out; }

	/** Returns the stream manager. */
	public HashtableOfTerm getStreamManager() { return streamManager; }

	/** Returns the hash manager. */
	public HashtableOfTerm getHashManager() { return hashManager; }

//	public final Operation exec(Operation code){
//		try {
//			logger.beforeExec(code);
//			return code.exec(this);
//		} catch (RuntimeException t){
//			throw logger.execThrows(t);
//		}
//	}

	public Object getExternalData(String key){
		return externalData.get(key);
	}

	public void setExternalData(String key, Object value){
		externalData.put(key, value);
	}

	public PrologLogger getLogger() {
		return logger;
	}
}
