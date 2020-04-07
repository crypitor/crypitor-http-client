package org.crypitor.httpclient.exception;

/**
 * @author trongcauta
 * @time 10:37 AM
 * @date 5/24/19
 */
public class CrypitorResponseException extends Exception {
    /**
     * Generated serial version uid.
     */
    private static final long serialVersionUID = 147694337695115012L;
    /**
     * The error code.
     */
    private final Integer code;
    /**
     * The error data.
     */
    /**
     * Create a new {@link CrypitorResponseException} instance.
     *
     * @param message The error message to set.
     */
    public CrypitorResponseException(String message) {
        super(message);
        this.code = null;
    }

    /**
     * Create a new {@link CrypitorResponseException} instance.
     *
     * @param message The error message to set.
     * @param cause   The cause of this response exception.
     */
    public CrypitorResponseException(String message, Throwable cause) {
        super(message, cause);
        this.code = null;
    }

    /**
     * Create a new {@link CrypitorResponseException} instance.
     *
     * @param code    The error code to set.
     * @param message The error message to set.
     */
    public CrypitorResponseException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Get the error code.
     *
     * @return The code if its available, <code>null</code> otherwise.
     */
    public Integer getCode() {
        return code;
    }

}
