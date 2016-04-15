// AUTHOR:  ENAS YUNIS
public class IOHelper {
	
	public static boolean isSingleToken(char c) {
		return ((c=='(') || (c==')') || (c=='.'));
	}
	public static boolean isAlpha(char c) {
		return (((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z')));
	}
	public static boolean isNumeric(char c) {
		return ((c >= '0') &&  (c <= '9'));
	}
	public static boolean isAlphaNumeric(char c) {
		return (isAlpha(c) || isNumeric(c));
	}
	public static boolean isStartNumeric(char c) {
		return ((c == '+') || (c == '-') || isNumeric(c));
	}
	public static boolean isWhiteSpace(char c) {
		return ((c == ' ') || (c == '\t') || (c == '\n') || (c == '\f') || (c == '\r'));
	}
	public static boolean isLegalStart(char c) {
		return ((c=='(') || isStartNumeric(c) || isAlpha(c));
	}
	public static boolean isLegalSeparator(char c) {
		return (isSingleToken(c) || isWhiteSpace(c));
	}
}
