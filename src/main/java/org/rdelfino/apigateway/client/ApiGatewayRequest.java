package org.rdelfino.apigateway.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.ToString;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A pojo representing an API Gateway request.
 */
@Getter
@ToString
@EqualsAndHashCode
public class ApiGatewayRequest {

    private static ObjectMapper DEFAULT_MAPPER = new ObjectMapper();
    static {
        DEFAULT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        DEFAULT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Configures the default ObjectMapper to use for serialization/deserialization
     * @param mapper the new ObjectMapper configuration to use for serialization/deserialization
     */
    public static void configureDefaultMapper(final @NonNull ObjectMapper mapper) {
        DEFAULT_MAPPER = mapper.copy();
    }

    /**
     * Gets the default ObjectMapper configuration used for serialization/deserialization
     * @return copy of default ObjectMapper configuration used for serialization/deserialization
     */
    public static ObjectMapper getDefaultMapperConfiguration() {
        return DEFAULT_MAPPER.copy();
    }

    final ObjectMapper mapper;

    final String host;

    final int port;

    final String method;

    final String path;

    final String apiKey;

    final Map<String, List<String>> queryParameters;

    final Map<String, List<String>> headers;

    final InputStream payload;

    /**
     * All args constructor
     * @param mapper ObjectMapper to use for serialization/deserialization
     * @param host host of  API gateay endpoint
     * @param port port of API gateay endpoint
     * @param method HTTP method
     * @param path path of API gateay endpoint
     * @param queryParameters HTTP request query parameters
     * @param headers headers HTTP request headers
     * @param payload payload content body payload
     */
    public ApiGatewayRequest(
            final @NonNull ObjectMapper mapper,
            final @NonNull String host,
            final int port,
            final @NonNull String method,
            final @NonNull String path,
            final String apiKey,
            final @NonNull Map<String, List<String>> queryParameters,
            final @NonNull Map<String, List<String>> headers,
            final InputStream payload
    ) {
        this.mapper = mapper;
        this.host = host;
        this.port = port;
        this.method = method;
        this.path = path;
        this.apiKey = apiKey;
        this.queryParameters = queryParameters;
        this.headers = headers;
        this.payload = payload;
    }

    /**
     * Creates a new Builder using this requst as a template
     * @return newly created Builder
     */
    Builder toBuilder() {
        return new ApiGatewayRequest.Builder()
            .setMapper(getMapper())
            .setHost(getHost())
            .setPort(getPort())
            .setMethod(getMethod())
            .setPath(getPath())
            .setQueryParameters(getQueryParameters())
            .setHeaders(getHeaders())
            .setPayload(getPayload());
    }

    static Map<String, List<String>> deepCopy(Map<String, List<String>> map) {
        final Map<String, List<String>> ret = new HashMap<>();
        map.forEach((name, values)->{
            ret.put(name, new ArrayList<>(values));
        });
        return ret;
    }

    /**
     * Creates a Builder with the host of the API Gateway endpoint
     * @param host the host of the API Gateway endpoint
     * @return newly created Builder
     */
    public static Builder builder(
           final @NonNull String host
    ) {
        return builder(host, 443);
    }

    /**
     * Creates a Builder with the host and port of the API Gateway endpoint
     * @param host the host of the API Gateway endpoint
     * @param port the port of the API Gateway endpoint
     * @return newly created Builder
     */
    public static Builder builder(
            final @NonNull String host,
            final int port
    ) {
        return builder(host, port, "/");
    }

    /**
     * Creates a Builder with the host and port of the API Gateway.
     * @param host the host of the API Gateway endpoint
     * @param path the path of the API Gateway endpoint
     * @return newly created Builder
     */
    public static Builder builder(
            final @NonNull String host,
            final @NonNull String path
    ) {
        return builder(host, 443, path);
    }

    /**
     * Creates a Builder with the host, port and path of the API Gateway endpoint.
     * @param host the host of the API Gateway endpoint
     * @param port the port of the API Gateway endpoint
     * @param path the path of the API Gateway endpoint
     * @return newly created Builder
     */
    public static Builder builder(
        final @NonNull String host,
        final int port,
        final @NonNull String path
    ) {
        return new Builder()
            .setHost(host)
            .setPort(port)
            .setPath(path);
    }

    /**
     * Creates a Builder obtaining the host, port, path, and query parameters of the API Gateway endpoint from an URL
     * @param url the URL of the API Gateway endpoint
     * @return newly created Builder
     */
    public static Builder builder(
        final @NonNull URL url
    ) {
        return builder(URI.create(url.toString()));
    }

    /**
     * Creates a Builder obtaining the host, port, path, and query parameters of the API Gateway endpoint from an URI
     * @param uri the URI of the API Gateway endpoint
     * @return newly created Builder
     */
    public static Builder builder(
        final @NonNull URI uri
    ) {
        return new Builder().setUri(uri);
    }

    /**
     * Builder to create ApiGatewayRequest instances
     */
    @Getter
    @ToString
    public static class Builder {

        private ObjectMapper mapper = DEFAULT_MAPPER;

        private String host;
        private int port = 443;
        private String method = "GET";
        private String path = "/";
        private String apiKey;
        private Map<String, List<String>> queryParameters = new HashMap<>();
        private Map<String, List<String>> headers = new HashMap<>();
        private InputStream payload = null;

        Builder() {
        }

        /**
         * Sets the ObjectMapper to use for JSON serialization/deserialization
         * @param mapper the ObjectMapper to use
         * @return this Builder
         */
        public Builder setMapper(@NonNull ObjectMapper mapper) {
            this.mapper = mapper;
            return this;
        }

        /**
         * Resets the ObjectMapper to the default
         * @return this Builder
         */
        public Builder resetMapper() {
            this.mapper = DEFAULT_MAPPER;
            return this;
        }

        /**
         * Sets the host to use for the request
         * @param host host to send the request to
         * @return this Builder
         */
        public Builder setHost(final @NonNull String host) {
            this.host = host;
            return this;
        }

        /**
         * Sets the port to use for the request
         * @param port port to send the request to
         * @return this Builder
         */
        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        /**
         * Sets the request method
         * @param method GET , POST, PUT, PATCH, DELETE, HEAD, OPTIONS, TRACE
         * @return this Builder
         */
        public Builder setMethod(final @NonNull String method) {
            this.method = method;
            return this;
        }

        /**
         * Sets the request path
         * @param path request path
         * @return this Builder
         */
        public Builder setPath(final @NonNull String path) {
            this.path = path;
            return this;
        }

        /**
         * Sets the API Key to use for the request
         * @param apiKey
         * @return
         */
        public Builder setApiKey(final String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * Sets the request query parameters
         * @param parameters HTTP query parameters
         * @return this Builder
         */
        public Builder setQueryParameters(
            final @NonNull Map<String, List<String>> parameters
        ) {
            this.queryParameters = deepCopy(parameters);
            return this;
        }

        /**
         * Adds a query parameter to the request
         * @param name parameter name
         * @param value parameter value
         * @return this Builder
         */
        public Builder addQueryParameter(
                final @NonNull String name,
                final @NonNull String value
        ) {
            return addQueryParameter(name, Arrays.asList(value));
        }

        /**
         * Adds a query parameter to the request
         * @param name parameter name
         * @param values parameter values
         * @return this Builder
         */
        public Builder addQueryParameter(
                final @NonNull String name,
                final @NonNull List<String> values
        ) {
            this.queryParameters.computeIfAbsent(name, k -> new ArrayList<>()).addAll(values);
            return this;
        }

        /**
         * Sets a query parameter to the request
         * @param name parameter name
         * @param value parameter value
         * @return this Builder
         */
        public Builder setQueryParameter(
                final @NonNull String name,
                final @NonNull String value
        ) {
            return setQueryParameter(name, Arrays.asList(value));
        }

        /**
         * Sets a query parameter to the request
         * @param name parameter name
         * @param values parameter values
         * @return this Builder
         */
        public Builder setQueryParameter(
                final @NonNull String name,
                final @NonNull List<String> values
        ) {
            this.queryParameters.put(name, new ArrayList<>(values));
            return this;
        }

        /**
         * Removes a query parameter from the request
         * @param name parameter name
         * @return this Builder
         */
        public Builder removeQueryParameter(
                final @NonNull String name
        ) {
            this.queryParameters.remove(name);
            return this;
        }

        /**
         * Sets the headers in the request
         * @param headers headers to set in the request
         * @return this Builder
         */
        public Builder setHeaders(
                final @NonNull Map<String, List<String>> headers
        ) {
            this.headers = deepCopy(headers);
            return this;
        }

        /**
         * Adds a header to the request
         * @param name header name
         * @param value header value
         * @return this Builder
         */
        public Builder addHeader(
                final @NonNull String name,
                final @NonNull String value
        ) {
            return addHeader(name, Arrays.asList(value));
        }

        /**
         * Adds a header to the request
         * @param name header name
         * @param values header values
         * @return this Builder
         */
        public Builder addHeader(
                final @NonNull String name,
                final @NonNull List<String> values
        ) {
            this.headers.computeIfAbsent(name, k -> new ArrayList<>()).addAll(values);
            return this;
        }

        /**
         * Sets a header in the request
         * @param name header name
         * @param value header value
         * @return this Builder
         */
        public Builder setHeader(
                final @NonNull String name,
                final @NonNull String value
        ) {
            return setHeader(name, Arrays.asList(value));
        }

        /**
         * Sets a header in the request
         * @param name header name
         * @param values header values
         * @return this Builder
         */
        public Builder setHeader(
                final @NonNull String name,
                final @NonNull List<String> values
        ) {
            this.headers.put(name, new ArrayList<>(values));
            return this;
        }

        /**
        `* Removes a header from the request
         * @param name header name
         * @return this Builder
         */
        public Builder removeHeader(
                final @NonNull String name
        ) {
            this.headers.remove(name);
            return this;
        }

        /**
         * Sets the request payload
         * @param payload InputStream containing the request payload
         * @return this Builder
         */
        public Builder setPayload(InputStream payload) {
            this.payload = payload;
            return this;
        }

        /**
         * Sets the request payload, serializing it to JSON
         * @param payload object to serialize to JSON
         * @param <Payload> type of the object to serialize to JSON
         * @return this Builder
         */
        public <Payload> Builder setJsonPayload(Payload payload) {
            if (payload != null) {
                setHeader("Content-Type", "application/json");
            }

            setPayload(serialize(payload));

            return this;
        }

        /**
         * Sets the request payload, serializing it to JSON
         * @param payload JSON TreeNode to serialize
         * @return this Builder
         */
        public Builder setTreeNodePayload(TreeNode payload) {

            if (payload != null) {
                setHeader("Content-Type", "application/json");
            }

            setPayload(serialize(payload));

            return this;
        }

        @SneakyThrows
        private <Payload> ByteArrayInputStream serialize(Payload payload) {
            return (payload == null) ? null :
                new ByteArrayInputStream(mapper.writeValueAsBytes(payload));
        }

        /**
         * Sets host, port, path and query parameters from a URL
         * @param url URL to obtain host, port, path and query parameters from
         * @return this Builder
         */
        public Builder setUrl(final @NonNull URL url) {
            return setUri(URI.create(url.toString()));
        }

        /**
         * Sets host, port, path and query parameters from a URI
         * @param uri URI to obtain host, port, path and query parameters from
         * @return this Builder
         */
        public Builder setUri(final @NonNull URI uri) {
            return this
                .setHost(uri.getHost())
                .setPort(uri.getPort())
                .setPath(uri.getPath())
                .setQueryParameters(
                    Optional.ofNullable(uri.getRawQuery())
                        .map(query -> SdkHttpUtils.uriParams(uri))
                        .orElseGet(() -> new HashMap<>())
                );
        }

        /**
         * Copies the values from another ApiGatewayRequest
         * @param request ApiGatewayRequest to copy from
         * @return this Builder
         */
        public Builder copyFrom(final @NonNull ApiGatewayRequest request) {
            return setMapper(request.getMapper())
                    .setHost(request.getHost())
                    .setPort(request.getPort())
                    .setMethod(request.getMethod())
                    .setPath(request.getPath())
                    .setQueryParameters(request.getQueryParameters())
                    .setHeaders(request.getHeaders())
                    .setPayload(request.getPayload());
        }

        /**
         * Builds an ApiGatewayRequest
         * @return an ApiGatewayRequest
         */
        public ApiGatewayRequest build() {
            return new ApiGatewayRequest(
                mapper,
                getHost(),
                getPort(),
                getMethod(),
                getPath(),
                getApiKey(),
                getQueryParameters(),
                getHeaders(),
                getPayload()
            );
        }
    }
}
