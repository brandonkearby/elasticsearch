/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.client.rest.support;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.rest.FailureListener;
import org.elasticsearch.client.rest.HttpClientConfigCallback;
import org.elasticsearch.client.rest.RequestConfigCallback;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * Helps creating a new {@link InternalRestClient}. Allows to set the most common http client configuration options when internally
 * creating the underlying {@link org.apache.http.nio.client.HttpAsyncClient}. Also allows to provide an externally created
 * {@link org.apache.http.nio.client.HttpAsyncClient} in case additional customization is needed.
 */
public class InternalRestClientBuilder {
    public static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 1000;
    public static final int DEFAULT_SOCKET_TIMEOUT_MILLIS = 10000;
    public static final int DEFAULT_MAX_RETRY_TIMEOUT_MILLIS = DEFAULT_SOCKET_TIMEOUT_MILLIS;
    public static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT_MILLIS = 500;
    public static final int DEFAULT_MAX_CONN_PER_ROUTE = 10;
    public static final int DEFAULT_MAX_CONN_TOTAL = 30;

    private static final Header[] EMPTY_HEADERS = new Header[0];

    private final HttpHost[] hosts;
    private boolean contentCompressionEnabled = true;
    private long maxRetryTimeout = DEFAULT_MAX_RETRY_TIMEOUT_MILLIS;
    private TimeValue connectionRequestTimeout = new TimeValue(DEFAULT_CONNECTION_REQUEST_TIMEOUT_MILLIS);
    private TimeValue connectTimeout = new TimeValue(DEFAULT_CONNECT_TIMEOUT_MILLIS);
    private TimeValue socketTimeout = new TimeValue(DEFAULT_SOCKET_TIMEOUT_MILLIS);
    private int maxConnectionsTotal = DEFAULT_MAX_CONN_TOTAL;
    private int maxConnectionsPerRoute = DEFAULT_MAX_CONN_PER_ROUTE;

    private Header[] defaultHeaders = EMPTY_HEADERS;
    private FailureListener failureListener;
    private HttpClientConfigCallback httpClientConfigCallback;
    private RequestConfigCallback requestConfigCallback;
    private String pathPrefix;
    private ByteSizeValue maxResponseSize;
    private HttpHost proxy;
    private Collection<String> proxyPreferredAuthSchemes;
    private Collection<String> targetPreferredAuthSchemes;

    public InternalRestClientBuilder setProxy(HttpHost proxy) {
        this.proxy = proxy;
        return this;
    }

    public InternalRestClientBuilder setProxyPreferredAuthSchemes(String... proxyPreferredAuthSchemes) {
        this.proxyPreferredAuthSchemes = Arrays.asList(proxyPreferredAuthSchemes);
        return this;
    }


    public InternalRestClientBuilder setTargetPreferredAuthSchemes(String... targetPreferredAuthSchemes) {
        this.targetPreferredAuthSchemes = Arrays.asList(targetPreferredAuthSchemes);
        return this;
    }

    public InternalRestClientBuilder setConnectionRequestTimeout(TimeValue connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
        return this;
    }

    public InternalRestClientBuilder setConnectTimeout(TimeValue connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public InternalRestClientBuilder setSocketTimeout(TimeValue socketTimeout) {
        this.socketTimeout = socketTimeout;
        return this;
    }

    public InternalRestClientBuilder setMaxConnectionsTotal(int maxConnectionsTotal) {
        this.maxConnectionsTotal = maxConnectionsTotal;
        return this;
    }

    public InternalRestClientBuilder setMaxConnectionsPerRoute(int maxConnectionsPerRoute) {
        this.maxConnectionsPerRoute = maxConnectionsPerRoute;
        return this;
    }


    /**
     * Creates a new builder instance and sets the hosts that the client will send requests to.
     *
     * @throws NullPointerException if {@code hosts} or any host is {@code null}.
     * @throws IllegalArgumentException if {@code hosts} is empty.
     */
    InternalRestClientBuilder(HttpHost... hosts) {
        Objects.requireNonNull(hosts, "hosts must not be null");
        if (hosts.length == 0) {
            throw new IllegalArgumentException("no hosts provided");
        }
        for (HttpHost host : hosts) {
            Objects.requireNonNull(host, "host cannot be null");
        }
        this.hosts = hosts;
    }

    /**
     * Sets the default request headers, which will be sent along with each request.
     * <p>
     * Request-time headers will always overwrite any default headers.
     *
     * @throws NullPointerException if {@code defaultHeaders} or any header is {@code null}.
     */
    public InternalRestClientBuilder setDefaultHeaders(Header[] defaultHeaders) {
        Objects.requireNonNull(defaultHeaders, "defaultHeaders must not be null");
        for (Header defaultHeader : defaultHeaders) {
            Objects.requireNonNull(defaultHeader, "default header must not be null");
        }
        this.defaultHeaders = defaultHeaders;
        return this;
    }

    /**
     * Sets the content compression enabled or not, current default is true
     * <p>
     */
    public InternalRestClientBuilder setContentCompressionEnabled(boolean contentCompressionEnabled) {
        this.contentCompressionEnabled = contentCompressionEnabled;
        return this;
    }

    /**
     * Sets the {@link FailureListener} to be notified for each request failure
     *
     * @throws NullPointerException if {@code failureListener} is {@code null}.
     */
    public InternalRestClientBuilder setFailureListener(FailureListener failureListener) {
        Objects.requireNonNull(failureListener, "failureListener must not be null");
        this.failureListener = failureListener;
        return this;
    }

    /**
     * Sets the maximum timeout (in milliseconds) to honour in case of multiple retries of the same request.
     * {@link #DEFAULT_MAX_RETRY_TIMEOUT_MILLIS} if not specified.
     *
     * @throws IllegalArgumentException if {@code maxRetryTimeoutMillis} is not greater than 0
     */
    public InternalRestClientBuilder setMaxRetryTimeoutMillis(long maxRetryTimeoutMillis) {
        if (maxRetryTimeoutMillis <= 0) {
            throw new IllegalArgumentException("maxRetryTimeoutMillis must be greater than 0");
        }
        this.maxRetryTimeout = maxRetryTimeoutMillis;
        return this;
    }

    /**
     * Sets the {@link HttpClientConfigCallback} to be used to customize http client configuration
     *
     * @throws NullPointerException if {@code httpClientConfigCallback} is {@code null}.
     */
    public InternalRestClientBuilder setHttpClientConfigCallback(HttpClientConfigCallback httpClientConfigCallback) {
        Objects.requireNonNull(httpClientConfigCallback, "httpClientConfigCallback must not be null");
        this.httpClientConfigCallback = httpClientConfigCallback;
        return this;
    }

    /**
     * Sets the {@link RequestConfigCallback} to be used to customize http client configuration
     *
     * @throws NullPointerException if {@code requestConfigCallback} is {@code null}.
     */
    public InternalRestClientBuilder setRequestConfigCallback(RequestConfigCallback requestConfigCallback) {
        Objects.requireNonNull(requestConfigCallback, "requestConfigCallback must not be null");
        this.requestConfigCallback = requestConfigCallback;
        return this;
    }

    /**
     * Sets the path's prefix for every request used by the http client.
     * <p>
     * For example, if this is set to "/my/path", then any client request will become <code>"/my/path/" + endpoint</code>.
     * <p>
     * In essence, every request's {@code endpoint} is prefixed by this {@code pathPrefix}. The path prefix is useful for when
     * Elasticsearch is behind a proxy that provides a base path; it is not intended for other purposes and it should not be supplied in
     * other scenarios.
     *
     * @throws NullPointerException if {@code pathPrefix} is {@code null}.
     * @throws IllegalArgumentException if {@code pathPrefix} is empty, only '/', or ends with more than one '/'.
     */
    public InternalRestClientBuilder setPathPrefix(String pathPrefix) {
        Objects.requireNonNull(pathPrefix, "pathPrefix must not be null");
        String cleanPathPrefix = pathPrefix;

        if (cleanPathPrefix.startsWith("/") == false) {
            cleanPathPrefix = "/" + cleanPathPrefix;
        }

        // best effort to ensure that it looks like "/base/path" rather than "/base/path/"
        if (cleanPathPrefix.endsWith("/")) {
            cleanPathPrefix = cleanPathPrefix.substring(0, cleanPathPrefix.length() - 1);

            if (cleanPathPrefix.endsWith("/")) {
                throw new IllegalArgumentException("pathPrefix is malformed. too many trailing slashes: [" + pathPrefix + "]");
            }
        }

        if (cleanPathPrefix.isEmpty() || "/".equals(cleanPathPrefix)) {
            throw new IllegalArgumentException("pathPrefix must not be empty or '/': [" + pathPrefix + "]");
        }

        this.pathPrefix = cleanPathPrefix;
        return this;
    }

    /**
     * Creates a new {@link InternalRestClient} based on the provided configuration.
     */
    public InternalRestClient build() {
        if (failureListener == null) {
            failureListener = new FailureListener();
        }
        CloseableHttpAsyncClient httpClient = createHttpClient();
        InternalRestClient restClient = new InternalRestClient(httpClient, maxRetryTimeout, defaultHeaders, hosts, pathPrefix, failureListener, maxResponseSize);
        httpClient.start();
        return restClient;
    }

    private CloseableHttpAsyncClient createHttpClient() {
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
                .setProxy(proxy)
                .setProxyPreferredAuthSchemes(proxyPreferredAuthSchemes)
                .setTargetPreferredAuthSchemes(targetPreferredAuthSchemes)
                .setConnectTimeout((int)connectTimeout.millis())
                .setSocketTimeout((int)socketTimeout.millis())
                .setConnectionRequestTimeout((int)connectionRequestTimeout.millis())
                .setContentCompressionEnabled(contentCompressionEnabled);
        if (requestConfigCallback != null) {
            requestConfigBuilder = requestConfigCallback.customizeRequestConfig(requestConfigBuilder);
        }

        HttpAsyncClientBuilder httpClientBuilder = HttpAsyncClientBuilder.create().setDefaultRequestConfig(requestConfigBuilder.build())
                .setMaxConnPerRoute(maxConnectionsPerRoute)
                .setMaxConnTotal(maxConnectionsTotal);
        if (httpClientConfigCallback != null) {
            httpClientBuilder = httpClientConfigCallback.customizeHttpClient(httpClientBuilder);
        }
        return httpClientBuilder.build();
    }

    public InternalRestClientBuilder setMaxResponseSize(ByteSizeValue maxResponseSize) {
        this.maxResponseSize = maxResponseSize;
        return this;
    }

}
