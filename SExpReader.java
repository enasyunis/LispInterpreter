// AUTHOR:  ENAS YUNIS

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.IOException;

public class SExpReader {
	private static Reader in;
	private static int nextChar;


	private static void initNextChar() {
		nextChar = (int) '\n';  // never been read before - treat as newline
	}
	private static boolean isEOS() {
		return (nextChar == -1);
	}
	private static void terminateIfEOS() throws IOException {
		if (isEOS()) {
			throw new IOException("Unexpected End of Input Stream");
		}
	} 

	public static void initialize() throws IOException {
		in = new BufferedReader(new InputStreamReader(System.in));
		initNextChar();
	}

	public static SExp input() throws LispException, IOException {
		char c = (char) nextChar; 

		// skip all the leading/trailing white spaces
		while (!isEOS() && IOHelper.isWhiteSpace(c)) {
			nextChar = in.read();
			c = (char) nextChar;
		}
		terminateIfEOS();

		if (IOHelper.isLegalStart(c)) {
			StringBuilder sb = new StringBuilder();
			sb.append(c);
			if (c == '(') {
				matchNonAtomic(sb); // reads in until matching )
				sb.append("\n"); // for Tokenizer correctness
				initNextChar(); // init NextChar for next input call correctness
				//System.out.println("Calling getNonAtomic on: *" + sb + "*");
				return getNonAtomic(new Tokenizer(new StringReader(sb.toString())));
			} else {
				matchAtomic(sb); // fills in nextChar with any valid separator including EOS
				//System.out.println("Calling getAtomic on: *" + sb + "*");
				return getAtomic(sb.toString());
			}
		} else {
			initNextChar(); // init NextChar for next input call correctness
			throw new LispException("Illegal Character - Expecting ( A-Z a-z +- 0-9 but found: " + c);
		} 
	} 

	/**
	 * Creates an Atomic SExp out of the String s 
	 */
	private static SExp getAtomic(String s) throws LispException, IOException { 
		s = s.toUpperCase();
		SExp e = SExp.atomic(s);
		return e;
	}

	// STATES: (), (car), (car . cdr), (car cadr), (car cadr caddr) where any car/cdr can be atomic or nonatomic

	/**
	 * Always called on a fully matched StringBuilder with an ending new line and onle single internal white spaces 
	 * consumes both ( and )
	 */
	private static SExp getNonAtomic(Tokenizer tn) throws LispException, IOException { 

		tn.nextToken(); // consuming "("
		String t = tn.getToken(); // not consumed
		if (t.equals(")")) { // working on the () case which equals NIL and not a list
			tn.nextToken(); // consuming ")"
			return SExp.NIL();
		}

		SExp car;
		SExp cdr;

		// Work on CAR
		if (t.equals("(")) {
			car = getNonAtomic(tn); // will consume both ( and )
		} else {
			car = getAtomic(t); 
			tn.nextToken(); // consuming the Atomic
		}

		// Work on CDR
		t = tn.getToken(); // not consumed
		if (t.equals(".")) { // working on case (car . cdr)
			tn.nextToken(); // consuming "."

			// what comes next has to be an atomic cdr or a nonatomic cdr only
			t = tn.getToken(); 
			if (t.equals("(")) {
				cdr = getNonAtomic(tn); // will consume both ( and )
			} else {
				cdr = getAtomic(t);
				tn.nextToken(); // consuming the Atomic
			}

		} else { // else separator is space - list format
			cdr = getInList(tn); // will not consume an ending ")"
		}


		if (! tn.getToken().equals(")")) {
			throw new LispException("Expected end of paranthesis instead of: " + tn.getToken());
		} 
		tn.nextToken(); // consuming the last ")"

		return SExp.cons(car, cdr);
	}

	private static SExp getInList(Tokenizer tn) throws LispException, IOException {
		if (tn.getToken().equals(")")) { // does not consume the ending )
			return SExp.NIL();
		} 

		SExp car, cdr; 

		if (tn.getToken().equals("(")) { 
			car = getNonAtomic(tn); // consumes all the way through ()
			cdr = getInList(tn);
		} else { 
			car = getAtomic(tn.nextToken()); // consuming the current token
			cdr = getInList(tn);
		}

		return SExp.cons(car, cdr);
	}


	/** 
	 * builds into StringBuilder until matches ) - put extra '\n' in nextChar
	 * expects ( to already be inserted in sb
	 * allows a max of one space between tokens
	 * does not error on bad input
	 */
	private static void matchNonAtomic(StringBuilder sb) throws IOException {
		char c;
		boolean done = false;
		while (! done) {

			nextChar = in.read();
			c = (char) nextChar; 

			terminateIfEOS();
	
			if (IOHelper.isWhiteSpace(c)) {
				sb.append(' ');
				// skip the rest of the white spaces
				while (!isEOS() && IOHelper.isWhiteSpace(c)) {
					nextChar = in.read();
					c = (char) nextChar; 
				} 
				terminateIfEOS();
			} 
			sb.append(c);
			if (c == '(') { // interior ()
				matchNonAtomic(sb);
			} else if (c == ')') {
				done = true;
			}
		}
	}

	/** 
	 * builds into StringBuilder until allowed breakers ., (, ), \s, EOS - fills in nextChar
	 * does not error on bad input
	 * expects the first character to have already been inserted in StringBuilder
	 */
	private static void matchAtomic(StringBuilder sb) throws IOException {
		char c;
		boolean done = false;
		while (! done) {
			nextChar = in.read();
			c = (char) nextChar; 
			if (isEOS() || IOHelper.isLegalSeparator(c)) { // valid separators
				done = true;
			} else {
				sb.append(c);
			}
		}
	}
}
