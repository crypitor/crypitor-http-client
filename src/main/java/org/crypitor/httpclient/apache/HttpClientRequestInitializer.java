/*
 *     This file is part of BeowulfJ (formerly known as 'Beowulf-Java-Api-Wrapper')
 *
 *     BeowulfJ is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     BeowulfJ is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.crypitor.httpclient.apache;


import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.crypitor.httpclient.HttpHandler;

import java.io.IOException;

public class HttpClientRequestInitializer {

    public static void initialize(HttpRequestBase request) throws IOException {
        request.setConfig(RequestConfig.custom()
                .setConnectTimeout(HttpHandler.getConnectTimeout())
                .setConnectionRequestTimeout(HttpHandler.getRequestTimeout())
                .build());
    }
}
