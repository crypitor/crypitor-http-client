package org.crypitor.httpclient;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.MessageConstraints;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.codecs.DefaultHttpRequestWriterFactory;
import org.apache.http.impl.nio.codecs.DefaultHttpResponseParser;
import org.apache.http.impl.nio.codecs.DefaultHttpResponseParserFactory;
import org.apache.http.impl.nio.conn.ManagedNHttpClientConnectionFactory;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.LineParser;
import org.apache.http.nio.NHttpMessageParser;
import org.apache.http.nio.NHttpMessageParserFactory;
import org.apache.http.nio.NHttpMessageWriterFactory;
import org.apache.http.nio.conn.ManagedNHttpClientConnection;
import org.apache.http.nio.conn.NHttpConnectionFactory;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.reactor.SessionInputBuffer;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.util.CharArrayBuffer;
import org.crypitor.httpclient.apache.CustomHttpHeaders;
import org.crypitor.httpclient.exception.CrypitorCommunicationException;
import org.crypitor.httpclient.exception.CrypitorResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.CodingErrorAction;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;

/**
 * @author trongcauta
 * @time 10:33 AM
 * @date 5/24/19
 */
public class HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(HttpHandler.class);

    private static final String POST = "POST";
    private static final String GET = "GET";
    private static final String PUT = "PUT";
    private static final String DELETE = "DELETE";
    private static final int MAX_CONNECTION_TOTAL = 1000;
    private static final int MAX_CONNECTION_PER_ROOT = 200;

    private static HttpHandler instance = null;
    /**
     * A preconfigured mapper instance used for de-/serialization of Json
     * objects.
     */
    private static ObjectMapper mapper = getObjectMapper();

    /**
     * The http used to send requests.
     */
    private SimpleHttp http;
    private SimpleHttp https;

    private static int connectTimeout = 3000;
    private static int requestTimeout = 2000;

    /**
     * Initialize the Connection Handler.
     */
    private HttpHandler() {
        // Create a new connection
        try {
            initializeNewClient();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getConnectTimeout() {
        return connectTimeout;
    }

    public static int getRequestTimeout() {
        return requestTimeout;
    }

    public static int getMaxConnectionTotal() {
        return MAX_CONNECTION_TOTAL;
    }

    public static int getMaxConnectionPerRoot() {
        return MAX_CONNECTION_PER_ROOT;
    }

    /**
     * Get a preconfigured Jackson Object Mapper instance.
     *
     * @return The object mapper.
     */
    public static ObjectMapper getObjectMapper() {
        if (mapper == null) {
            mapper = new ObjectMapper();

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

            mapper.setDateFormat(simpleDateFormat);
            mapper.setTimeZone(TimeZone.getTimeZone("GMT"));
            mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            SimpleModule simpleModule = new SimpleModule("BooleanAsString", new Version(1, 0, 0, null, null, null));
            simpleModule.addSerializer(Boolean.class, new BooleanSerializer());
            simpleModule.addSerializer(boolean.class, new BooleanSerializer());

            mapper.registerModule(simpleModule);
        }

        return mapper;
    }

    /**
     * Initialize a new <code>http</code> by selecting one of the configured
     * endpoints.
     *
     * @throws CrypitorCommunicationException If no {@link SimpleHttp} implementation for the given
     *                                        schema is available.
     */
    private void initializeNewClient() {
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setMaxConnTotal(MAX_CONNECTION_TOTAL)
                .setMaxConnPerRoute(MAX_CONNECTION_PER_ROOT)
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();

        CloseableHttpAsyncClient asyncClient = null;
        try {
            asyncClient = createHttpAsyncClient();
        } catch (Exception e) {
            e.printStackTrace();
            asyncClient = HttpAsyncClientBuilder.create()
                    .setMaxConnTotal(MAX_CONNECTION_TOTAL)
                    .setMaxConnPerRoute(MAX_CONNECTION_PER_ROOT)
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();

        }
        asyncClient.start();

        http = new SimpleHttp(httpClient, asyncClient);

        httpClient = HttpClientBuilder.create()
                .setMaxConnTotal(MAX_CONNECTION_TOTAL)
                .setMaxConnPerRoute(MAX_CONNECTION_PER_ROOT)
                .build();
        https = new SimpleHttp(httpClient, asyncClient);
    }

    @SuppressWarnings("Duplicates")
    private CloseableHttpAsyncClient createHttpAsyncClient() throws IOReactorException {
        IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
                .setIoThreadCount(Runtime.getRuntime().availableProcessors())
                .setConnectTimeout(connectTimeout)
                .setSoTimeout(connectTimeout)
                .build();
        ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(ioReactorConfig);

        NHttpMessageWriterFactory<HttpRequest> requestWriterFactory = new DefaultHttpRequestWriterFactory();
        NHttpMessageParserFactory<HttpResponse> responseParserFactory = new DefaultHttpResponseParserFactory() {

            @Override
            public NHttpMessageParser<HttpResponse> create(
                    final SessionInputBuffer buffer,
                    final MessageConstraints constraints) {
                LineParser lineParser = new BasicLineParser() {

                    @Override
                    public Header parseHeader(final CharArrayBuffer buffer) {
                        try {
                            return super.parseHeader(buffer);
                        } catch (ParseException ex) {
                            return new BasicHeader(buffer.toString(), null);
                        }
                    }

                };
                return new DefaultHttpResponseParser(
                        buffer, lineParser, DefaultHttpResponseFactory.INSTANCE, constraints);
            }

        };
        NHttpConnectionFactory<ManagedNHttpClientConnection> connFactory = new ManagedNHttpClientConnectionFactory(
                requestWriterFactory, responseParserFactory, HeapByteBufferAllocator.INSTANCE);

        Registry<SchemeIOSessionStrategy> sessionStrategyRegistry = RegistryBuilder.<SchemeIOSessionStrategy>create()
                .register("http", NoopIOSessionStrategy.INSTANCE)
                .register("https", SSLIOSessionStrategy.getDefaultStrategy())
                .build();

        // Create connection configuration
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setMalformedInputAction(CodingErrorAction.IGNORE)
                .setUnmappableInputAction(CodingErrorAction.IGNORE)
                .setCharset(Consts.UTF_8)
                .build();

        PoolingNHttpClientConnectionManager connManager = new PoolingNHttpClientConnectionManager(
                ioReactor, connFactory, sessionStrategyRegistry);

        connManager.setDefaultConnectionConfig(connectionConfig);
        connManager.setMaxTotal(MAX_CONNECTION_TOTAL);
        connManager.setDefaultMaxPerRoute(MAX_CONNECTION_PER_ROOT);

        // Create global request configuration
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setExpectContinueEnabled(true)
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(connectTimeout)
                .setConnectionRequestTimeout(connectTimeout)
                .build();
        return HttpAsyncClients.custom()
                .setConnectionManager(connManager)
                .setDefaultRequestConfig(defaultRequestConfig)
                .build();
    }

    /**
     * Perform a request to the web socket API whose response will automatically
     * get transformed into the given object.
     *
     * @param requestObject         A request object that contains all needed parameters.
     * @param targetClass           The type the response should be transformed to.
     * @param <T>                   The type that should be returned.
     * @param enableSslVerification ssl verification mode
     * @return The server response transformed into a list of given objects.
     * @throws org.crypitor.httpclient.exception.CrypitorTimeoutException   If the server was not able to answer the request in the given
     *                                                                      time
     * @throws CrypitorCommunicationException                               If there is a connection problem.
     * @throws org.crypitor.httpclient.exception.CrypitorTransformException If the BeowulfJ is unable to transform the JSON response into a
     *                                                                      Java object.
     * @throws CrypitorResponseException                                    If the Server returned an error object.
     */
    public <T> T performRequestAndHandleError(String requestMethod,
                                              RequestObject requestObject,
                                              Class<T> targetClass,
                                              String request_uri,
                                              List<Header> headers,
                                              boolean enableSslVerification)
            throws CrypitorCommunicationException, CrypitorResponseException, URISyntaxException {
        URI uri = new URI(request_uri);
        SimpleHttp httpContext = enableSslVerification && uri.getScheme().equals("https") ? https : http;
        ResponseObject rawJsonResponse = httpContext.sendRequest(requestMethod, requestObject, uri, headers);
        logger.debug("Received " + rawJsonResponse);

        if (rawJsonResponse.isError()) {
            throw rawJsonResponse.handleError();
        } else {
            // HANDLE NORMAL RESPONSE
            return rawJsonResponse.handleResult(targetClass);
        }
    }

    public <T> CompletableFuture<T> performRequestAndHandleErrorAsync
            (String requestMethod,
             RequestObject requestObject,
             Class<T> targetClass,
             String request_uri,
             List<Header> headers,
             boolean enableSslVerification) {

        return AsyncHandler.run(() ->
                performRequestAndHandleError(requestMethod,
                        requestObject,
                        targetClass,
                        request_uri,
                        headers,
                        enableSslVerification));
    }

    public <T> CompletableFuture<T> performRequestAndHandleErrorAsyncWithCallback
            (String requestMethod,
             RequestObject requestObject,
             Class<T> targetClass,
             String request_uri,
             List<Header> headers,
             boolean enableSslVerification,
             AsyncCallbackHandler<T> callback) {

        return AsyncHandler.runWithCallback(() ->
                performRequestAndHandleError(requestMethod,
                        requestObject,
                        targetClass,
                        request_uri,
                        headers,
                        enableSslVerification), callback);
    }

    public <T> void performHttpAsyncRequestWithCallback
            (String requestMethod,
             RequestObject requestObject,
             Class<T> targetClass,
             String request_uri,
             List<Header> headers,
             boolean enableSslVerification,
             final AsyncCallbackHandler<T> callback)
            throws URISyntaxException, CrypitorCommunicationException, CrypitorResponseException {
        URI uri = new URI(request_uri);
        SimpleHttp httpContext = enableSslVerification && uri.getScheme().equals("https") ? https : http;
        httpContext.sendRequestAsync(requestMethod, requestObject, uri, headers, new FutureCallback<ResponseObject>() {
            @Override
            public void completed(ResponseObject responseObject) {
                try {
                    if (responseObject.isError()) {
                        callback.failed(responseObject.handleError());
                    } else {
//                        JavaType expectedResultType = mapper.getTypeFactory().constructType(targetClass);
                        callback.completed(responseObject.handleResult(targetClass));
                    }
                } catch (Exception e) {
                    callback.failed(e);
                }
            }

            @Override
            public void failed(Exception e) {
                callback.failed(e);
            }

            @Override
            public void cancelled() {
                callback.failed(new CrypitorCommunicationException("Http request has been canceled. lol =)))"));
            }
        });
    }

    public <T> T doPost(RequestObject requestObject, Class<T> targetClass, String request_uri, List<Header> headers)
            throws URISyntaxException, CrypitorCommunicationException, CrypitorResponseException {
        logger.info(String.format("Start sending POST: %s\n\tContent: %s", request_uri, requestObject.toJson()));
        return performRequestAndHandleError(POST, requestObject, targetClass, request_uri, headers, true);
    }

    public <T> CompletableFuture<T> doPostAsync(RequestObject requestObject, Class<T> targetClass, String request_uri, List<Header> headers) {
        logger.info(String.format("Start sending POST Async: %s\n\tContent: %s", request_uri, requestObject.toJson()));
        return performRequestAndHandleErrorAsync(POST, requestObject, targetClass, request_uri, headers, true);
    }

    public <T> CompletableFuture<T> doPostAsyncWithCallback(RequestObject requestObject, Class<T> targetClass, String request_uri, List<Header> headers, AsyncCallbackHandler<T> callback) {
        logger.info(String.format("Start sending POST Async with callback: %s\n\tContent: %s", request_uri, requestObject.toJson()));
        return performRequestAndHandleErrorAsyncWithCallback(POST, requestObject, targetClass, request_uri, headers, true, callback);
    }

    public <T> void doPostHttpAsyncWithCallback(RequestObject requestObject, Class<T> targetClass, String request_uri, List<Header> headers, AsyncCallbackHandler<T> callback)
            throws URISyntaxException, CrypitorCommunicationException, CrypitorResponseException {
        performHttpAsyncRequestWithCallback(POST, requestObject, targetClass, request_uri, headers, true, callback);
    }

    public <T> T doGet(Class<T> targetClass, String request_uri, List<Header> headers)
            throws URISyntaxException, CrypitorCommunicationException, CrypitorResponseException {
        logger.info(String.format("Start sending GET: %s", request_uri));
        return performRequestAndHandleError(GET, null, targetClass, request_uri, headers, true);
    }

    public <T> T doPut(RequestObject requestObject, Class<T> targetClass, String request_uri, List<Header> headers)
            throws URISyntaxException, CrypitorCommunicationException, CrypitorResponseException {
        logger.info(String.format("Start sending DELETE: %s\n\tContent: %s", request_uri, requestObject.toJson()));
        return performRequestAndHandleError(PUT, requestObject, targetClass, request_uri, headers, true);
    }

    public <T> T doDelete(Class<T> targetClass, String request_uri, List<Header> headers)
            throws URISyntaxException, CrypitorCommunicationException, CrypitorResponseException {
        logger.info(String.format("Start sending POST: %s", request_uri));
        return performRequestAndHandleError(DELETE, null, targetClass, request_uri, headers, true);
    }


    public static HttpHandler getInstance() {
        if (instance == null)
            instance = new HttpHandler();
        return instance;
    }

    public static void main(String[] args) throws CrypitorCommunicationException, CrypitorResponseException, URISyntaxException {
        ResponseObject responseObject = HttpHandler.getInstance().doGet(ResponseObject.class, "https://webhook.site/ca1453e3-b2aa-422f-8f9a-d4ec2c764e29", CustomHttpHeaders.custom().build());
        System.out.println(responseObject.getRawJsonResponse());
    }
}
