// AUTHOR:  ENAS YUNIS
import java.io.Reader;
import java.io.IOException;

// returns: { . (  ) [A-Z][A-Z0-9]* [0-9]+  } only - spaces are assumed between the different tokens otherwise   

// this system requires ANY symbol after ) or an atomic expression to return it. 
public class Tokenizer {
	private Reader in;
	
	private String token=null;
	private char next;

	public Tokenizer(Reader reader) throws LispException, IOException {
		in = reader;
		next = (char) in.read(); // read the first ever char.
		readToken();
	}

	private void readToken() throws LispException, IOException {
		boolean done = false;
		if (next == '\n') { // finished - no more items to read
			token = "\n";
		} else {
			if (IOHelper.isWhiteSpace(next)) { // can only have 1 white space at any given time.
				next = (char) in.read();			
			} // Fall down to reading the next token type

			if (IOHelper.isSingleToken(next)) { 
				// return that to be the new token, read in next
				token = Character.toString(next);
				next = (char) in.read();
			} else if (IOHelper.isLegalStart(next)) {
				StringBuilder sb = new StringBuilder();
				while (! IOHelper.isLegalSeparator(next)) {
					sb.append(next);
					next = (char) in.read();
				}
				token = sb.toString();
			} else {
				// return illegal characters in system call
				throw new LispException("Illegal Character Read: " + next);
			}
		}
	}

	public String getToken() { return token; }
	public String nextToken() throws LispException, IOException  { String temp = token; readToken(); return temp;}
}
