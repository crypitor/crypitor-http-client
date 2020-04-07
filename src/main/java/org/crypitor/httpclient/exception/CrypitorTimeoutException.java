package org.crypitor.httpclient.exception;

/**
 * @author trongcauta
 * @time 10:41 AM
 * @date 5/24/19
 */
public class CrypitorTimeoutException extends CrypitorCommunicationException {
    private static final long serialVersionUID = 147694337695115012L;

    public CrypitorTimeoutException(String message) {
        super(message);
    }

    public CrypitorTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
