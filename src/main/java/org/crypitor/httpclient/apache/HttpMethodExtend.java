package org.crypitor.httpclient.apache;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

import java.net.URI;

final class HttpMethodExtend extends HttpEntityEnclosingRequestBase {
    private final String methodName;

    public HttpMethodExtend(String methodName, URI uri) {
        if (methodName == null)
            throw new NullPointerException();
        this.methodName = methodName;
        this.setURI(uri);
    }

    public String getMethod() {
        return this.methodName;
    }
}
