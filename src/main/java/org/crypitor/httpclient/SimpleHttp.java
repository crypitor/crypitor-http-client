package org.crypitor.httpclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.mime.MIME;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.util.EntityUtils;
import org.crypitor.httpclient.apache.CustomHttpHeaders;
import org.crypitor.httpclient.apache.HttpClientRequestInitializer;
import org.crypitor.httpclient.apache.HttpRequestFactory;
import org.crypitor.httpclient.exception.CrypitorCommunicationException;
import org.crypitor.httpclient.exception.CrypitorResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Future;

public class SimpleHttp {
    private static final Logger logger = LoggerFactory.getLogger(HttpHandler.class);

    private CloseableHttpClient client;
    private CloseableHttpAsyncClient asyncClient;
    private static ObjectMapper objectMapper = new ObjectMapper();

    public SimpleHttp(CloseableHttpClient client, CloseableHttpAsyncClient asyncClient) {
        this.client = client;
        this.asyncClient = asyncClient;
    }

    public ResponseObject sendRequest(String requestMethod,
                                      RequestObject requestObject,
                                      URI endpointUri,
                                      List<Header> headers)
            throws CrypitorCommunicationException, CrypitorResponseException {
        try {
            HttpRequestBase httpRequestBase = buildRequest(requestMethod, requestObject, endpointUri, headers);

            CloseableHttpResponse response = client.execute(httpRequestBase);
            return handleResponse(response);

        } catch (HttpResponseException e) {
            logger.error(e.getMessage());
            throw new CrypitorResponseException(e.getStatusCode(), e.getMessage());
        } catch (NoHttpResponseException e) {
            logger.error(e.getMessage());
            throw new CrypitorResponseException(HttpStatus.SC_REQUEST_TIMEOUT, e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            throw new CrypitorCommunicationException("A problem occured while processing the request.", e);
        }
    }

    public Future<HttpResponse> sendRequestAsync(String requestMethod,
                                                 RequestObject requestObject,
                                                 URI endpointUri,
                                                 List<Header> headers,
                                                 final FutureCallback<ResponseObject> futureCallback)
            throws CrypitorCommunicationException, CrypitorResponseException {
        try {
            HttpRequestBase httpRequestBase = buildRequest(requestMethod, requestObject, endpointUri, headers);

            return asyncClient.execute(httpRequestBase, new FutureCallback<HttpResponse>() {
                @Override
                public void completed(HttpResponse httpResponse) {
                    try {
                        ResponseObject responseObject = handleResponse(httpResponse);
                        futureCallback.completed(responseObject);
                    } catch (HttpResponseException e) {
                        futureCallback.failed(new CrypitorResponseException(e.getStatusCode(), e.getMessage()));
                    } catch (IOException e) {
                        futureCallback.failed(new CrypitorCommunicationException("A problem occured while processing the request.", e));
                    } catch (Exception e) {
                        futureCallback.failed(e);
                    }
                }

                @Override
                public void failed(Exception e) {
                    futureCallback.failed(e);
                }

                @Override
                public void cancelled() {
                    futureCallback.cancelled();
                }
            });

        } catch (HttpResponseException e) {
            logger.error(e.getMessage());
            throw new CrypitorResponseException(e.getStatusCode(), e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            throw new CrypitorCommunicationException("A problem occured while processing the request.", e);
        }
    }

    public static HttpRequestBase buildRequest(String requestMethod, RequestObject requestObject, URI endpointUri, List<Header> headers) throws IOException {
        String requestPayload = "";
        if (requestObject != null) {
            requestPayload = objectMapper.writeValueAsString(requestObject);
        }
        HttpRequestBase httpRequestBase = HttpRequestFactory.buildRequest(requestMethod, endpointUri, requestPayload);
        if (headers == null) {
            headers = CustomHttpHeaders.custom().build();
        }
        for (Header header : headers) {
            httpRequestBase.setHeader(header);
        }
        HttpClientRequestInitializer.initialize(httpRequestBase);
        return httpRequestBase;
    }

    public static ResponseObject handleResponse(HttpResponse response) throws IOException {
        int status = response.getStatusLine().getStatusCode();
        String responseJSON = EntityUtils.toString(response.getEntity(), MIME.UTF8_CHARSET);

        if (status >= 200 && status < 300 && responseJSON != null) {
            ResponseObject responseObject = new ResponseObject(HttpHandler.getObjectMapper().readTree(responseJSON));
            responseObject.setStatus(status);
            return responseObject;
        } else {
            throw new HttpResponseException(status, responseJSON);
        }
    }
}
