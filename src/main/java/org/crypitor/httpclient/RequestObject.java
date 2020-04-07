package org.crypitor.httpclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author trongcauta
 * @time 10:50 AM
 * @date 5/24/19
 */
public class RequestObject {
    private static final Gson gson = new GsonBuilder().create();

    /**
     * @return The json representation of this object.
     */
    public String toJson() {
        return gson.toJson(this);
    }
}
