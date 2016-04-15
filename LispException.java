// AUTHOR:  ENAS YUNIS
public class LispException extends Exception {
    public LispException(String message) {
        super(message);
    }

    public LispException() {
        super();
    }

    public LispException(Throwable throwable) {
        super(throwable);
    }

    public LispException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
