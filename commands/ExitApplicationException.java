public class ExitApplicationException extends RuntimeException {
    public ExitApplicationException() {
        super("Exit requested");
    }
}
