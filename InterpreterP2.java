// AUTHOR:  ENAS YUNIS
import java.io.IOException;

public class InterpreterP2 {
	public static SExp dList;

	public static void initialize() throws LispException, IOException {
		SExp.initialize();
		dList = SExp.NIL();
		SExpReader.initialize();
	}

	// aList fromat: ((key2.value) (key1.value) ) -- most recent closest to root
	// dList format: ( (F2. ((X Y) . fb)) (F1 . ((X Y) . fb)) )  -- most recent closest to root

	/** 
	 * interpreter[dList]	=	eval[exp, NIL, dList] 
	 * or better:		    =   output[ eval[input[], NIL, dList] ]
	 */
	public static void main(String... args) {
		int eNum=-1;
		try {
			initialize();
			System.out.println("This system will NOT parse an s-expression until it is 'complete'");
			System.out.println("     for atomic, separators are \\s, ., (, )");
			System.out.println("     for non-atomic it is by matching the first ( with its )");
			System.out.println(++eNum + " >> Welcome to CLISP\n");
			while (true) {
				System.out.print(++eNum + " >> ");
				try {
					SExp eIn = SExpReader.input();
					SExp eOut = eval(eIn); // aList always starts as NIL for each top level executable
					SExpWriter.output(eOut);
					System.out.println();
				} catch(LispException e) {
					System.out.println(e.getMessage());
					System.out.println();
				} 
			}
		} catch(Throwable e) {
			System.err.print(++eNum + " >> ");
			System.err.println(e.getMessage());
			System.out.println(eNum + " >> Unrecoverable Failure - Bye");
		}
	}


	/**
	 * First level eval function to allow for DEFUN to be honord.  
	 * INPUT exp=(DEFUN F1 (X Y) fb) = (DEFUN.(F1.((X Y).(fb.NIL))))
	 * ADD TO DLIST (F1 . ((X Y) . fb)) 
	 */
	public static SExp eval(SExp exp) throws LispException {
		if (! exp.isAtom() && SExp.car(exp).isAtom()) {
			SExp e=SExp.car(exp);
			if (e.equals("DEFUN")) {
				// make sure format is right // any car/cdr calls will crash on an error without specifiying it.

				SExp f = SExp.cr(exp, "cadr");
				if (! f.isSym()) {
					throw new LispException("Error: DEFUN Format error, expected function name");
				}

				SExp pList = SExp.cr(exp, "caddr");
				if (! pList.isSymbolList()) {
					throw new LispException("Error: DEFUN Format error, parameters are not in a proper symbolic atomic list format (X Y Z)");
				}

				SExp body = SExp.cr(exp, "cadddr");
				if (body.isNull()) {
					throw new LispException("Error: DEFUN Format error, empty function body!!!");
				}
				
				if (!SExp.cr(exp, "cddddr").isNull()){
					throw new LispException("Error: DEFUN Format error, more arguments than expected for DEFUN");
				}
				
				SExp ffull = SExp.cons(f, SExp.cons(pList, body)); // setup the function format
				dList = SExp.cons(ffull, dList); // update the dList

				System.out.print("Added the function " + f.getString() + " with " + pList.listLength() + " required arguments: ");
				return SExp.T();
			} else if (e.equals("EXIT")) {
				System.out.println("Evaluated EXIT - Bye");
				System.exit(0);				
			}
		}

		return eval(exp, SExp.NIL()); // aList always starts as NIL for each top level executable
	}

	/**
	 * eval[exp, aList, dList] = [
	 * 		atom[exp] 	-->	[	int[exp]		-->	exp
	 *						  |	eq[exp,T]		-->	T												
	 *						  |	eq[exp,NIL]		-->	NIL												
	 *						  |	in[exp,aList]	-->	getVal[exp,aList] % parameter name
	 *						  |	T				--> "unbound variable!"  ]		
	 *	  |	atom[car[exp]] -->
	 *				[	eq[car[exp],QUOTE] 	--> cadr[exp] % car(cdr(expr))
	 *				|	eq[car[exp],COND] 	--> evcon[cdr[exp], aList, dList]
	 *				|	eq[car[exp],DEFUN] 	--> "add to dList (state change!)" at first level eval, otherwise error
	 *				|	T                   --> apply[car[exp],  evlis[cdr[exp],aList,dList],
	 *																		aList, dList]  ] 
	 *				% the aList is a build up of all the functions that have not yet evaluated and build it up as you go… 
	 *	  |	T		-->  "error!"  ] % instead of error they assume it is a lambda function
	 */
	public static SExp eval(SExp exp, SExp aList) throws LispException {
		if (exp.isAtom()) {
			if (exp.isNull() || exp.isT() || exp.isInt()) return exp;
			else if ((in(exp, aList)).isT()) return getVal(exp, aList);
			else throw new LispException("Error - Unbound variable " + exp.getString());
		} else if (SExp.car(exp).isAtom()) {
			if (! exp.isList()) { // all function calls have to be of list format
				throw new LispException("Eval Error - function call requires a list format");
			}

			SExp e = SExp.car(exp);
			if (e.equals("QUOTE")) {
				if (SExp.cdr(exp).isNull() || (! SExp.cr(exp, "cddr").isNull())) {
					throw new LispException("Eval Error - QUOTE takes only one argument");
				}
				return SExp.cr(exp, "cadr");
			} else if (e.equals("COND")) {
				// TODO evcon will take care of the format of COND
				return evcon(SExp.cdr(exp), aList);
			} else if (e.equals("DEFUN")) { // NOT frist level function
				throw new LispException("Error: calling inner level eval on DEFUN");
				//dList = SExp.cons(SExp.cdr(exp), dList);
			} else if (e.equals("EXIT")) {
				System.out.println("Evaluated EXIT - Bye");
				System.exit(0);				
			} else {
				return apply(SExp.car(exp), evlis(SExp.cdr(exp), aList), aList); // f=car(exp), x=evlis
			}
		} // else {
			throw new  LispException (
				"Can only evaluate atomic expressions or predefined functions");
		// }
	}

	/*
		// T, NIL
		// CAR, CDR, CONS, ATOM, EQ, NULL, INT,
		// PLUS, MINUS, TIMES, QUOTIENT, REMAINDER, LESS, GREATER, COND, QUOTE, DEFUN.
	*/

	/**
	 * apply[f, x, aList, dList] = [ %(car (quote (2.3))) => x=((2.3)) x is the list of arguments. If no args, x=NIL and not the list (NIL)
	 * 		atom[f] -->	[	eq[f, CAR]	-->	caar[x];		% why? Returns (2.3) then 2.
	 *					  |	eq[f, CDR]	-->	cdar[x];
	 *					  |	eq[f, CONS]	-->	cons[car[x], cadr[x]]; % (cons 2 3) x =>(2 3) (cons (quote (1.2)) (quote 3) ) => x= ( (1.2) 3 ) 
 	 *					  |	eq[f, ATOM]	-->	atom[car[x]];
	 *					  |	eq[f, NULL]	-->	null[car[x]];
	 *					  |	eq[f, EQ]	-->	eq[car[x], cadr[x]]; 
	 *					  |	T			-->	eval[ cdr[getval[f, dList]], % the cdr is the body of the function % gets value of p1 ((p1 . 42))
	 *											  addpairs[car[getval[f, dList]], x, aList], % new aList
	 *											  dList ];		
	 *					]; // the car is the list of the paramters fro the function, x is the evaluated argument list, aList is the previous definitions from previous incompleted functions.
	 * 		| T	 -->	"error!"; % lambda functions here
	 * ] 
	 *
	 * % Elements on dList of form:  (f . (pList . body) )
	 * % addpairs[pList,x,aList]: returns new a-list check that size of x is the same as the size of pList. ((pList . x) … items concat aList)
	 */
	public static SExp apply(SExp f, SExp x, SExp aList) throws LispException {
		if (f.isAtom()) {
			String s = f.getString(); 
			if (s.matches("C[AD]+R")) { // handles all the formats for all C__R 
				testValidArgCount(s, x, 1);				
				return SExp.cr(SExp.car(x), s); // need to start with the car of x-list
			} else if (s.equals("CONS")) {
				testValidArgCount(s, x, 2);	
				// cons requires ONLY TWO expression
				return SExp.cons(SExp.car(x), SExp.cr(x, "cadr"));
			} else if (s.equals("ATOM")) {
				testValidArgCount(s, x, 1);					
				return SExp.atom(SExp.car(x));
			} else if (s.equals("NULL")) {
				testValidArgCount(s, x, 1);	
				return SExp.nuLL(SExp.car(x));
			} else if (s.equals("INT")) {
				testValidArgCount(s, x, 1);	
				return SExp.iNT(SExp.car(x));
			} else if (s.equals("EQ")) {
				testValidArgCount(s, x, 2);	
				return SExp.eq(SExp.car(x), SExp.cr(x, "cadr"));
			} else if (s.equals("PLUS")) {
				testValidArgCount(s, x, 2);	
				return SExp.plus(SExp.car(x), SExp.cr(x, "cadr"));
			} else if (s.equals("MINUS")) {
				testValidArgCount(s, x, 2);				
				return SExp.minus(SExp.car(x), SExp.cr(x, "cadr"));
			} else if (s.equals("TIMES")) {
				testValidArgCount(s, x, 2);	
				return SExp.times(SExp.car(x), SExp.cr(x, "cadr"));
			} else if (s.equals("QUOTIENT")) {
				testValidArgCount(s, x, 2);	
				return SExp.quotient(SExp.car(x), SExp.cr(x, "cadr"));
			} else if (s.equals("REMAINDER")) {
				testValidArgCount(s, x, 2);	
				return SExp.remainder(SExp.car(x), SExp.cr(x, "cadr"));
			} else if (s.equals("LESS")) {
				testValidArgCount(s, x, 2);	
				return SExp.less(SExp.car(x), SExp.cr(x, "cadr"));
			} else if (s.equals("GREATER")) {
				testValidArgCount(s, x, 2);	
				return SExp.greater(SExp.car(x), SExp.cr(x, "cadr"));
			} else {
				// TODO - getValInList will try to match by arglist size should test matching of params.
				SExp funcVal = getValInFuncList(f, x.listLength(), dList); // funcVal = ((X Y).FB)
				return eval(SExp.cdr(funcVal), addPairs(SExp.car(funcVal), x, aList)); // addPairs will give us a new aList to pass to eval
			}
		} else {
			throw new LispException (
				"Expected a function name and found a non-atomic expression");			
		}
	}

	/* helper function for apply for the pre-defined functions */
	private static void testValidArgCount(String fName, SExp x, int expected) throws LispException {
		int given = x.listLength();
		if (expected != given) {
			throw new LispException("Apply Error - " + fName + " requires " + expected + " paramter(s) but given " + given + " paramter(s)!");			
		}


	}

	/**
	 * Addpairs[pl,argl,alist] = [
	 *	   null[pl] -> alist;
	 * 	 | T->addpairs[cdr[pl], cdr[argl], 
	 *	                cons[ cons[car[pl], car[argl]], alist] ];
	 * ]
     */
	public static SExp addPairs(SExp paramList, SExp argList, SExp aList) throws LispException { // no need to check for argument matching, given it is done in the lookup
		if (paramList.isNull()) { 
			return aList;
		} else {
			return addPairs(
						SExp.cdr(paramList), 
						SExp.cdr(argList), 
						SExp.cons(
							SExp.cons(SExp.car(paramList), SExp.car(argList)), 
							aList));
		}
	}

	/**
 	 * evlis[list, aList, dList] = [ % list is the list of arguments to evaluate by creating ((key . value) (key.value) )
	 *		null[list] 	-->	NIL;
	 *	  |	T	 		-->	cons[ eval[car[list], aList, dList], evlis[cdr[list], aList,dList] ] 
	 * ]
 	 */
	public static SExp evlis(SExp list, SExp aList) throws LispException {
		if (list.isNull()) {
			return SExp.NIL();
		} else {
			return SExp.cons(
						eval(SExp.car(list), aList), 
						evlis(SExp.cdr(list),aList));
		}
	}


	/**
	 * evcon[be, aList, dList]	=  [ % be is of form ((b1 e1) ... (bn en))
	 *		null[be] 					 --> NIL; % better: error!; CHANGE THIS TO ERROR
	 *	  |	eval[caar[be], aList, dList] --> eval[cadar[be], aList, dList];
	 *	  |	T							 --> evcon[cdr[be], aList, dList] ]
 	 * ]
	 */
	public static SExp evcon(SExp be, SExp aList) throws LispException {
		if (be.isNull()) { 
			throw new  LispException ("evcon Error - calling COND without a reachable binary expressions");
		} else if (! SExp.car(be).isList() || (SExp.car(be).listLength() != 2)) {
			throw new  LispException ("evcon Error - encountered COND formatting error");
		} else if ((eval(SExp.cr(be,"caar"), aList)).isT()) {
			return eval(SExp.cr(be,"cadar"), aList);
		} else {
			return evcon(SExp.cdr(be), aList);
		}
	}



	/**
	 * In[p, aL] = [
	 *		null[aL] -> NIL;
	 *	  | eq[p, caar[aL]] -> T;
	 *	  | T -> In[p, cdr[aL]];
	 * ]
	 */
	public static SExp in(SExp param, SExp aList) throws LispException { // In
		if (aList.isNull()) {
			return SExp.NIL();
		} else if (SExp.cr(aList,"caar").equals(param)) {
			return SExp.T();
		} else {
			return in(param, SExp.cdr(aList));
		}
	}

	/**
	 * getVal[p, aL] = [
  	 * 		  null[aL] -> CRASH;
 	 *		| eq[p, caar[aL]] -> cdar[aL]; 
 	 * 		| T -> getVal[p, cdr[aL]];
 	 * ]
	 */
	public static SExp getVal(SExp param, SExp aList) throws LispException { // getVal
		if (aList.isNull()) {
			throw new LispException("Calling getVal on an unbound variable!");
		} else if (SExp.cr(aList,"caar").equals(param)) {
			return SExp.cr(aList,"cdar");
		} else {
			return getVal(param, SExp.cdr(aList));
		}
	}


	/**
	 * getValInFuncList[f, x, dL] = [
  	 * 		  null[dL] -> ERROR;
 	 *		| AND[eq[p, caar[dL]], eq[LENGTH[x], LENGTH[cadar[dL]]]] -> cdar[dL]; 
 	 * 		| T -> getVal[p, cdr[dL]];
 	 * ]
	 */
	public static SExp getValInFuncList(SExp f, int xlen, SExp dL) throws LispException { // getVal
		if (dL.isNull()) {
			throw new LispException("Apply Error - undefined function: " + f.getString() + " with param count: " + xlen);
		} else if (SExp.cr(dL,"caar").equals(f) && (xlen == SExp.cr(dL, "cadar").listLength()) ) {
			return SExp.cr(dL,"cdar");
		} else {
			return getValInFuncList(f, xlen, SExp.cdr(dL));
		}
	}

}
