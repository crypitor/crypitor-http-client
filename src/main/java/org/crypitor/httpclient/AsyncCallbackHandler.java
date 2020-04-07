package org.crypitor.httpclient;

import org.apache.http.concurrent.FutureCallback;

public interface AsyncCallbackHandler<T> extends FutureCallback<T> {
    void call(T result, Throwable exception);
}
