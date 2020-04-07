package org.crypitor.httpclient.exception;

/**
 * @author trongcauta
 * @time 10:35 AM
 * @date 5/24/19
 */
public class CrypitorCommunicationException extends Exception {
    private static final long serialVersionUID = -3389735550453652555L;

    public CrypitorCommunicationException() {
        super();
    }

    public CrypitorCommunicationException(String message) {
        super(message);
    }

    public CrypitorCommunicationException(Throwable cause) {
        super("Sorry, an error occurred while processing your request.", cause);
    }

    public CrypitorCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
