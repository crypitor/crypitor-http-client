package org.crypitor.httpclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.crypitor.httpclient.exception.CrypitorResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author trongcauta
 * @time 10:50 AM
 * @date 5/24/19
 */
public class ResponseObject {
    private static final Logger logger = LoggerFactory.getLogger(ResponseObject.class);


    private static final String ERROR_FIELD_NAME = "error";
    private static final String ERROR_MESSAGE_FIELD_NAME = "description";

    private JsonNode rawJsonResponse;
    private int status;

    public ResponseObject() {
    }

    /**
     * Create a new {@link ResponseObject} instance.
     *
     * @param rawJsonResponse The raw JSON response that should be wrapped by this
     *                        {@link ResponseObject} instance.
     */
    public ResponseObject(JsonNode rawJsonResponse) {
        if (rawJsonResponse == null) {
            rawJsonResponse = NullNode.getInstance();
        }
        this.rawJsonResponse = rawJsonResponse;
    }

    public JsonNode getRawJsonResponse() {
        return rawJsonResponse;
    }

    public void setRawJsonResponse(JsonNode rawJsonResponse) {
        this.rawJsonResponse = rawJsonResponse;
    }

    // #########################################################################
    // ## HANDLE ERRORS ########################################################
    // #########################################################################

    /**
     * Check if the {@link #getRawJsonResponse()} contains an error.
     *
     * @return <code>true</code> if the response contains an error,
     * <code>false</code> otherwise.
     */
    public boolean isError() {
        return (status < 200 || status > 300) || (rawJsonResponse.has(ERROR_FIELD_NAME) && rawJsonResponse.get(ERROR_FIELD_NAME) != null
                && !rawJsonResponse.get(ERROR_FIELD_NAME).isNull());
    }

    /**
     * This method checks if the JSON response wrapped by this
     * {@link ResponseObject} instance has the expected <code>id</code> and
     * will try to generate and throw a {@link CrypitorResponseException} based on
     * the response.
     *
     * @return A {@link CrypitorResponseException} based on the Json response.
     * @throws CrypitorResponseException If the response does not contain the expected <code>id</code>
     *                                   or the response do not contain an error.
     */
    public CrypitorResponseException handleError() throws CrypitorResponseException {
        if (isResponseValid()) {
            if (!isError()) {
                throw new CrypitorResponseException(status,
                        "The result does not contain the required " + ERROR_FIELD_NAME + " field.");
            } else {
                ObjectNode responseAsObject = (ObjectNode) rawJsonResponse;
                return createThrowable(responseAsObject);
            }
        }

        throw new CrypitorResponseException(status, "Tried to generate a throwable out of a unexpected Json structure.");
    }

    /**
     * Create a new {@link CrypitorResponseException} based on the Json response.
     *
     * @param response The response object to transform.
     * @return A {@link CrypitorResponseException} based on the Json response.
     * @throws CrypitorResponseException If the response does not contain the expected
     *                                   <code>id</code>.
     */
    private CrypitorResponseException createThrowable(ObjectNode response) throws CrypitorResponseException {
        JsonNode errorNode = response.get(ERROR_FIELD_NAME);

        if (!errorNode.isObject()) {
            throw new CrypitorResponseException(status, "The response does not have the expected structure.");
        }

        ObjectNode errorObject = (ObjectNode) errorNode;

        Integer errorCode = isFieldNullOrEmpty(ERROR_FIELD_NAME, errorObject) ? null
                : errorObject.get(ERROR_FIELD_NAME).asInt();
        String message = isFieldNullOrEmpty(ERROR_MESSAGE_FIELD_NAME, errorObject) ? null
                : errorObject.get(ERROR_MESSAGE_FIELD_NAME).asText();
        /*JsonNode data = isFieldNullOrEmpty(ERROR_DATA_FIELD_NAME, errorObject) ? null
                : errorObject.get(ERROR_DATA_FIELD_NAME);*/

        return new CrypitorResponseException(errorCode, message);
    }

    /**
     * Check if the JSON response wrapped by this {@link ResponseObject}
     * instance has the expected JSON structure.
     *
     * @return <code>true</code> if this is the case or <code>false</code> if
     * not.
     */
    private boolean isResponseValid() {
        if (rawJsonResponse.isEmpty() || rawJsonResponse.isObject())
            return true;

        logger.warn("The response is not an object.");
        return false;
    }

    /**
     * This method checks if the JSON response wrapped by this
     * {@link ResponseObject} instance has the expected <code>id</code> and
     * will try to transform the JSON into the given <code>type</code>.
     *
     * @param targetClass The type to transform the JSON to.
     * @return A list of of <code>type</code> instances.
     * @throws CrypitorResponseException If the response does not contain the expected <code>id</code>
     *                                   or if the response could not be transformed into the expected
     *                                   <code>type</code>.
     */
    public <T> T handleResult(Class<T> targetClass) throws CrypitorResponseException {
        try {
            if (targetClass == ResponseObject.class)
                return (T) this;
            if (isResponseValid()) {
//            ObjectNode responseAsObject = ObjectNode.class.cast(rawJsonResponse);
                if (targetClass == null) {
                    return null;
                }
                return HttpHandler.getObjectMapper().convertValue(rawJsonResponse, targetClass);
            } else throw new CrypitorResponseException(status, "Response not valid");
        } catch (Exception e) {
            e.printStackTrace();
            throw new CrypitorResponseException(status, "Error while parse response");
        }
    }

    /**
     * Check if the given <code>fieldName</code> has an empty or null value.
     *
     * @param fieldName The field name to check.
     * @param response  The response to check.
     * @return <code>true</code> if the <code>fieldName</code> has an empty or
     * null value.
     */
    public boolean isFieldNullOrEmpty(String fieldName, ObjectNode response) {
        return (response.get(fieldName) == null || response.get(fieldName).isNull());
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
