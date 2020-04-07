package org.crypitor.httpclient.apache;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import java.util.ArrayList;
import java.util.List;

public class CustomHttpHeaders {
    private static final String USER_AGENT = "Crypitor-HTTP-Client/1.0.0";
    private List<Header> headers;

    public static CustomHttpHeaders custom() {
        CustomHttpHeaders customHttpHeaders = new CustomHttpHeaders();
        customHttpHeaders.headers = new ArrayList<>();
        customHttpHeaders.addHeader("user-agent", USER_AGENT);
        customHttpHeaders.addHeader("Content-Type", "application/json");
        return customHttpHeaders;
    }

    public CustomHttpHeaders setAuthorization(String authorization) {
        Header header = new BasicHeader("authorization", authorization);
        this.headers.add(header);
        return this;
    }

    public CustomHttpHeaders addHeader(String key, String value) {
        Header header = new BasicHeader(key, value);
        return this.addHeader(header);
    }

    public CustomHttpHeaders addHeader(Header header) {
        this.headers.add(header);
        return this;
    }

    public List<Header> build() {
        return headers;
    }
}
