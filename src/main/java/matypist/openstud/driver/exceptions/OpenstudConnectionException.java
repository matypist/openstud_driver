package matypist.openstud.driver.exceptions;

public class OpenstudConnectionException extends Exception {
    public OpenstudConnectionException(String message) {
        super(message);
    }

    public OpenstudConnectionException(Exception e) {
        super(e);
    }
}
