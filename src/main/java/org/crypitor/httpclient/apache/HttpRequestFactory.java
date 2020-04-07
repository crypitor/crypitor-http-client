package org.crypitor.httpclient.apache;

import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.net.URI;

/**
 * @author tatrongcau
 * @project crypitor-services
 * @time 2019-06-16
 */
public class HttpRequestFactory {

    public static HttpRequestBase buildRequest(String method, URI uri, String requestPayload) throws IOException {
        if (method.equals("POST")) {
            HttpPost httpPost = new HttpPost();
            httpPost.setURI(uri);
            StringEntity entity = new StringEntity(requestPayload, ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);
            return httpPost;
        } else if (method.equals("PUT")) {
            HttpPut httpPut = new HttpPut();
            httpPut.setURI(uri);
            StringEntity entity = new StringEntity(requestPayload, ContentType.APPLICATION_JSON);
            httpPut.setEntity(entity);
            return new HttpPut(uri);
        } else if (method.equals("DELETE")) {
            return new HttpDelete(uri);
        } else if (method.equals("GET")) {
            return new HttpGet(uri);
        } else if (method.equals("HEAD")) {
            return new HttpHead(uri);
        } else if (method.equals("TRACE")) {
            return new HttpTrace(uri);
        } else {
            return new HttpMethodExtend(method, uri);
        }
    }
}
