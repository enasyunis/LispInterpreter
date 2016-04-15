// AUTHOR:  ENAS YUNIS

public class SExpWriter {
	
	public static void output(SExp e) throws LispException {
		StringBuilder sb = new StringBuilder();
		toStringNotList(e, sb);
		System.out.println(sb);
	}


	// called when we want an atom or a new list to be created
	private static void toStringNotList(SExp e, StringBuilder sb) throws LispException { 
		if (! e.isAtom()) { // expression 
			sb.append("( ");	
			if (e.isList()) { // last cdr is NIL (called for every car, but only once for all cdr)
			   	toStringInList(e, sb);	
			} else { // last cdr is not NIL
				toStringNotList(SExp.car(e), sb);
				sb.append(" . ");
				toStringNeverList(SExp.cdr(e), sb);
			}	
			sb.append(" )");
		} else {
			sb.append(e.getString());
		}
	}

	// called when we know that cdr of the expression is NEVER a list format
	private static void toStringNeverList(SExp e, StringBuilder sb) throws LispException {
		if (! e.isAtom()) { // expression 
			sb.append("( ");	
			toStringNotList(SExp.car(e), sb);
			sb.append(" . ");
			toStringNeverList(SExp.cdr(e), sb);
			sb.append(" )");
		} else {
			sb.append(e.getString());
		}
	}

	// called when we want to append to an existing list
	private static void toStringInList(SExp e, StringBuilder sb) throws LispException { 
		toStringNotList(SExp.car(e), sb); // car always starts a new list if it was an expression

		if (! SExp.cdr(e).isNull()) { // at NIL do not print :)
			if (! SExp.cdr(e).isAtom()) { // Expression - continue list format
				sb.append(" ");
				toStringInList(SExp.cdr(e), sb);
			} else {
				throw new LispException("In toStringInList yet cdr is a non-null atom!!!");
			}				
		}
	}
}
