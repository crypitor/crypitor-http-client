package org.crypitor.httpclient.exception;

/**
 * @author trongcauta
 * @time 10:40 AM
 * @date 5/24/19
 */
public class CrypitorTransformException extends CrypitorCommunicationException {
    private static final long serialVersionUID = -1405676306096463952L;

    public CrypitorTransformException(String message) {
        super(message);
    }

    public CrypitorTransformException(String message, Throwable cause) {
        super(message, cause);
    }
}
