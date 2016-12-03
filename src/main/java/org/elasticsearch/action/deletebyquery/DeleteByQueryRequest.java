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

package org.elasticsearch.action.deletebyquery;

import com.google.common.base.Charsets;
import org.apache.http.HttpEntity;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.ElasticsearchGenerationException;
import org.elasticsearch.Version;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ActionRestRequest;
import org.elasticsearch.action.support.QuerySourceBuilder;
import org.elasticsearch.action.support.replication.IndicesReplicationOperationRequest;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.util.UriBuilder;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.rest.RestRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.elasticsearch.action.ValidateActions.addValidationError;

/**
 * A request to delete all documents that matching a specific query. Best created with
 * {@link org.elasticsearch.client.Requests#deleteByQueryRequest(String...)}.
 * <p/>
 * <p>The request requires the source to be set either using {@link #source(QuerySourceBuilder)},
 * or {@link #source(byte[])}.
 *
 * @see DeleteByQueryResponse
 * @see org.elasticsearch.client.Requests#deleteByQueryRequest(String...)
 * @see org.elasticsearch.client.Client#deleteByQuery(DeleteByQueryRequest)
 */
public class DeleteByQueryRequest extends IndicesReplicationOperationRequest<DeleteByQueryRequest> {

    private BytesReference source;
    private boolean sourceUnsafe;

    private String[] types = Strings.EMPTY_ARRAY;
    @Nullable
    private String routing;

    /**
     * Constructs a new delete by query request to run against the provided indices. No indices means
     * it will run against all indices.
     */
    public DeleteByQueryRequest(String... indices) {
        this.indices = indices;
    }

    public DeleteByQueryRequest() {
    }

    /**
     * Copy constructor that creates a new delete by query request that is a copy of the one provided as an argument.
     * The new request will inherit though headers and context from the original request that caused it.
     */
    public DeleteByQueryRequest(ActionRequest originalRequest) {
        super(originalRequest);
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = super.validate();
        if (source == null) {
            validationException = addValidationError("source is missing", validationException);
        }
        return validationException;
    }

    /**
     * The source to execute.
     */
    BytesReference source() {
        if (sourceUnsafe) {
            source = source.copyBytesArray();
        }
        return source;
    }

    /**
     * The source to execute.
     */
    public DeleteByQueryRequest source(QuerySourceBuilder sourceBuilder) {
        this.source = sourceBuilder.buildAsBytes(Requests.CONTENT_TYPE);
        this.sourceUnsafe = false;
        return this;
    }

    /**
     * The source to execute. It is preferable to use either {@link #source(byte[])}
     * or {@link #source(QuerySourceBuilder)}.
     */
    public DeleteByQueryRequest source(String query) {
        this.source = new BytesArray(query.getBytes(Charsets.UTF_8));
        this.sourceUnsafe = false;
        return this;
    }

    /**
     * The source to execute in the form of a map.
     */
    @SuppressWarnings("unchecked")
    public DeleteByQueryRequest source(Map source) {
        try {
            XContentBuilder builder = XContentFactory.contentBuilder(Requests.CONTENT_TYPE);
            builder.map(source);
            return source(builder);
        } catch (IOException e) {
            throw new ElasticsearchGenerationException("Failed to generate [" + source + "]", e);
        }
    }

    public DeleteByQueryRequest source(XContentBuilder builder) {
        this.source = builder.bytes();
        this.sourceUnsafe = false;
        return this;
    }

    /**
     * The source to execute.
     */
    public DeleteByQueryRequest source(byte[] source) {
        return source(source, 0, source.length, false);
    }

    /**
     * The source to execute.
     */
    public DeleteByQueryRequest source(byte[] source, int offset, int length, boolean unsafe) {
        this.source = new BytesArray(source, offset, length);
        this.sourceUnsafe = unsafe;
        return this;
    }

    public DeleteByQueryRequest source(BytesReference source, boolean unsafe) {
        this.source = source;
        this.sourceUnsafe = unsafe;
        return this;
    }

    /**
     * The types of documents the query will run against. Defaults to all types.
     */
    String[] types() {
        return this.types;
    }

    /**
     * A comma separated list of routing values to control the shards the search will be executed on.
     */
    public String routing() {
        return this.routing;
    }

    /**
     * A comma separated list of routing values to control the shards the search will be executed on.
     */
    public DeleteByQueryRequest routing(String routing) {
        this.routing = routing;
        return this;
    }

    /**
     * The routing values to control the shards that the search will be executed on.
     */
    public DeleteByQueryRequest routing(String... routings) {
        this.routing = Strings.arrayToCommaDelimitedString(routings);
        return this;
    }

    /**
     * The types of documents the query will run against. Defaults to all types.
     */
    public DeleteByQueryRequest types(String... types) {
        this.types = types;
        return this;
    }

    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        sourceUnsafe = false;
        source = in.readBytesReference();
        routing = in.readOptionalString();
        types = in.readStringArray();
    }

    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeBytesReference(source);
        out.writeOptionalString(routing);
        out.writeStringArray(types);
    }

    @Override
    public String toString() {
        String sSource = "_na_";
        try {
            sSource = XContentHelper.convertToJson(source, false);
        } catch (Exception e) {
            // ignore
        }
        return "[" + Arrays.toString(indices) + "][" + Arrays.toString(types) + "], source[" + sSource + "]";
    }


    @Override
    public ActionRestRequest getActionRestRequest(Version version) {
        if (version.id >= Version.V_5_0_0_ID) {
            return new ActionRequestV5(this);
        }
        else {
            return new LegacyActionRestRequest(this);
        }
    }

    private static class LegacyActionRestRequest implements ActionRestRequest {
        private DeleteByQueryRequest request;

        LegacyActionRestRequest(DeleteByQueryRequest deleteByQueryRequest) {
            request = deleteByQueryRequest;
        }

        @Override
        public RestRequest.Method getMethod() {
            return RestRequest.Method.DELETE;
        }

        @Override
        public String getEndPoint() {
            UriBuilder uriBuilder = UriBuilder.newBuilder()
                    .csv(request.indices())
                    .csv(request.types())
                    .slash("_query");
            return uriBuilder.build();
        }

        @Override
        public HttpEntity getEntity() throws IOException {
            return new NStringEntity(XContentHelper.convertToJson(request.source, false), StandardCharsets.UTF_8);
        }

        @Override
        public Map<String, String> getParams() {
            return MapBuilder.<String, String>newMapBuilder().putIfNotNull("routing", request.routing()).map();
        }

        @Override
        public HttpEntity getBulkEntity() throws IOException {
            return ActionRequest.EMPTY_ENTITY;
        }

    }

    private static class ActionRequestV5 implements ActionRestRequest {
        private DeleteByQueryRequest request;

        ActionRequestV5(DeleteByQueryRequest deleteByQueryRequest) {
            request = deleteByQueryRequest;
        }

        @Override
        public RestRequest.Method getMethod() {
            return RestRequest.Method.POST;
        }

        @Override
        public String getEndPoint() {
            UriBuilder uriBuilder = UriBuilder.newBuilder()
                    .csv(request.indices())
                    .csv(request.types())
                    .slash("_delete_by_query");
            return uriBuilder.build();
        }

        @Override
        public HttpEntity getEntity() throws IOException {
            return new NStringEntity(XContentHelper.convertToJson(request.source, false), StandardCharsets.UTF_8);
        }

        @Override
        public Map<String, String> getParams() {
            return MapBuilder.<String, String>newMapBuilder().putIfNotNull("routing", request.routing()).map();
        }

        @Override
        public HttpEntity getBulkEntity() throws IOException {
            return ActionRequest.EMPTY_ENTITY;
        }
    }

}
