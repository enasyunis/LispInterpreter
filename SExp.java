// AUTHOR:  ENAS YUNIS
import java.util.HashMap;
public class SExp {

	// STATIC DATA
	private static SExp T, NIL; // faster than lookup
	private static HashMap<String, SExp> symList; // still add T,NIL here for backward lookups
	public static void initialize() throws LispException {
		symList = new HashMap<String, SExp>();
		
		// added for safety on rest of system
		NIL = new SExp();
		NIL._type=Type.NIL;
		symList.put("NIL", NIL);

		// added for safety on rest of system
		T = new SExp();
		T._type=Type.T;
		symList.put("T", T);

		symList.put("CAR", SExp.atomic("CAR"));
		symList.put("CDR", SExp.atomic("CDR"));
		
		symList.put("CONS", SExp.atomic("CONS"));
	
		symList.put("DEFUN", SExp.atomic("DEFUN"));
		symList.put("QUOTE", SExp.atomic("QUOTE"));
		symList.put("COND", SExp.atomic("COND"));
		
		symList.put("EXIT", SExp.atomic("EXIT"));
		
		symList.put("ATOM", SExp.atomic("ATOM"));
		symList.put("NULL", SExp.atomic("NULL"));
		symList.put("INT", SExp.atomic("INT"));

		symList.put("EQ", SExp.atomic("EQ"));
		symList.put("GREATER", SExp.atomic("GREATER"));
		symList.put("LESS", SExp.atomic("LESS"));

		symList.put("REMAINDER", SExp.atomic("REMAINDER"));
		symList.put("QUOTIENT", SExp.atomic("QUOTIENT"));
		symList.put("TIMES", SExp.atomic("TIMES"));
		symList.put("MINUS", SExp.atomic("MINUS"));
		symList.put("PLUS", SExp.atomic("PLUS"));
	}



	// INSTANCE DATA
	private enum Type {
		T, NIL, INT, SYM, EXP
	}
	private Type _type;

	private int _int;
	private String _symbol;
	public SExp _car;
	public SExp _cdr;

	/**
	 * Creates a new atomic s-expression from the provided string or look it up
	 */
	public static SExp atomic(String s) throws LispException { 

		SExp e;
		if (s.matches("[-+]?\\d+")) {
			e = new SExp();
			e._type=Type.INT;
			if (s.charAt(0) =='+') { // java errors for uniary + sign!!!
				s = s.substring(1);
			}
			try {
				e._int = Integer.parseInt(s);
			} catch(NumberFormatException nfe) {
				throw new LispException("Not a legal Integer in Lisp: " + s);
			}
		} else if (s.matches("[A-Z][A-Z0-9]*")) {
			if (s.length() > 10) {
				throw new LispException("Illegal Symbol (length > 10): " + s);
			}

			if (symList.containsKey(s)) { // if exists look it up
				e = symList.get(s); 
			} else { // othewise create new and add to symList
				e = new SExp();
				e._type=Type.SYM;
				e._symbol = s;
				symList.put(s, e);
			}
		} else {
			throw new LispException("Not a valid Atomic Expression: " + s);
		}	
		return e;
	}
 
	private SExp() {} // hidden for now

	// Following functionality eases abstrction
	public boolean isSymbolList() { // see if I have (A B C) [valid param list]
		if (_type == Type.NIL) {
			return true;
		} else if (_type == Type.EXP) {
			if (_car._type != Type.SYM) // not symbol list
				return false;
			return _cdr.isSymbolList();
		}
		return false;
	}
	public int listLength() { // return the number of car elements, assumes proper list
		if (_type == Type.EXP) {
			return _cdr.listLength()+1;
		}
		return 0;
	}
	public boolean isList() {
		if (_type == Type.EXP) {
			if (_cdr._type == Type.NIL)
				return true;
			return _cdr.isList();
		}
		return false;
	}

	public boolean isAtom() {
		return _type!=Type.EXP;

	}
	public boolean isNull() {
		return _type==Type.NIL;
	}
	public boolean isT() {
		return _type==Type.T;
	}	
	public boolean isInt() {
		return _type==Type.INT;
	}
	public boolean isSym() {
		return _type==Type.SYM;
	}

	// used by ATOMIC OUTPUT
	public String getString() throws LispException {
		if (_type == Type.EXP) {
			throw new LispException ("Can not call getString on a non-atmoic expression");
		}

		if (_type == Type.INT)
			return Integer.toString(_int);
		if (_type == Type.SYM)
			return _symbol;
		return _type.toString();

	}

	public boolean equals(String s) {
		return ((_type == Type.SYM) && (_symbol.equals(s)));
	}

 	// used by the symList functionality
	public boolean equals(SExp e) throws LispException { // atomic level only (EQ function)
		if (e._type == _type) {
			if (_type == Type.INT) {
				return e._int == _int;
			}
			if (_type == Type.SYM) {
				return e._symbol.equals(_symbol);
			}
			 
			return true; // cases NIL and T
		}
		return false;
	}

	// T
	public static SExp T() {
		return T;
	}

	// NIL
	public static SExp NIL() {
		return NIL;
	}

	// CAR
	public static SExp car(SExp e) throws LispException {
		if (e._type != Type.EXP) {
			throw new LispException("Calling CAR on the atomic expression " +e.getString());
		}
		return e._car;
	}


	// CDR
	public static SExp cdr(SExp e) throws LispException {
		if (e._type != Type.EXP) {
			throw new LispException("Calling CDR on the atomic expression " +e.getString());
		}
		return e._cdr;
	}

	// all possible c___r out there - the limit is 8 (due to length constraint on atomic symbol sexpressions)
	public static SExp cr(SExp e, String cmds) throws LispException {
		char c;
		for(int i=cmds.length()-2; i>0; --i) { // skipping the c and r
			c = cmds.charAt(i);
			if ((c=='a')||(c=='A')) {
				e = car(e);
			} else if ((c=='d')||(c=='D')) {
				e = cdr(e);
			} else {
				throw new LispException("Calling CR using unknown char " +c);				
			}
		}
		return e;
	}


	// CONS
	public static SExp cons(SExp car, SExp cdr) {
		SExp e = new SExp();
		e._type=Type.EXP;
		e._car = car;
		e._cdr = cdr;
		return e;
	}	

	// NULL
	public static SExp nuLL(SExp e) {
		return (e.isNull()?T:NIL);
	}

	// ATOM
	public static SExp atom(SExp e) {
		return (e.isAtom()?T:NIL);
	}

	// INT
	public static SExp iNT(SExp e) {
		return (e.isInt()?T:NIL);
	}

	// EQ
	public static SExp eq(SExp s1, SExp s2)  throws LispException  {
		if ((s1._type == Type.EXP) || (s2._type == Type.EXP)) {
			throw new LispException ("Calling EQ on non-atomic SExpression(s)");
		}
		return (s1.equals(s2)?T:NIL);
	}

	// PLUS
	public static SExp plus(SExp s1, SExp s2) throws LispException {
		if (! s1.isInt() || ! s2.isInt()) {
			throw new LispException("Calling PLUS on non-numeric SExpression(s)");
		}		
		long l = s1._int + s2._int;
		if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE) {
			throw new LispException("Calling PLUS cauases an overflow");
		}
		SExp e = new SExp();
		e._type=Type.INT;
		e._int = (int) l;
		return e;
	}

	// MINUS
	public static SExp minus(SExp s1, SExp s2) throws LispException {
		if (! s1.isInt() || ! s2.isInt()) {
			throw new LispException("Calling MINUS on non-numeric SExpression(s)");
		}
		long l = s1._int - s2._int;
		if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE) {
			throw new LispException("Calling MINUS cauases an overflow");
		}		
		SExp e = new SExp();
		e._type=Type.INT;
		e._int = (int) l;
		return e;
	}

	// TIMES
	public static SExp times(SExp s1, SExp s2) throws LispException {
		if (! s1.isInt() || ! s2.isInt()) {
			throw new LispException("Calling TIMES on non-numeric SExpression(s)");
		}
		long l = s1._int * s2._int;
		if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE) {
			throw new LispException("Calling TIMES cauases an overflow");
		}
		SExp e = new SExp();
		e._type=Type.INT;
		e._int = (int) l;
		return e;
	}

	// QUOTIENT
	public static SExp quotient(SExp s1, SExp s2) throws LispException {
		if (! s1.isInt() || ! s2.isInt()) {
			throw new LispException("Calling QUOTIENT on non-numeric SExpression(s)");
		}
		if (s2._int == 0) {
			throw new LispException("Calling QUOTIENT on a zero denominator");
		}
		SExp e = new SExp();
		e._type=Type.INT;
		e._int = s1._int / s2._int;
		return e;
	}		

	// REMAINDER
	public static SExp remainder(SExp s1, SExp s2) throws LispException {
		if (! s1.isInt() || ! s2.isInt()) {
			throw new LispException("Calling REMAINDER on non-numeric SExpression(s)");
		} 
		if (s2._int == 0) {
			throw new LispException("Calling REMAINDER on a zero denominator");
		}
		SExp e = new SExp();
		e._type=Type.INT;
		e._int = s1._int % s2._int;
		return e;
	}

	// LESS
	public static SExp less(SExp s1, SExp s2) throws LispException {
		if (! s1.isInt() || ! s2.isInt()) {
			throw new LispException("Calling LESS on non-numeric SExpression(s)");
		}
		return (s1._int < s2._int)?T:NIL;
	}	

	// GREATER
	public static SExp greater(SExp s1, SExp s2) throws LispException {
		if (! s1.isInt() || ! s2.isInt()) {
			throw new LispException("Calling GREATER on non-numeric SExpression(s)");
		}
		return (s1._int > s2._int)?T:NIL;
	}

}
