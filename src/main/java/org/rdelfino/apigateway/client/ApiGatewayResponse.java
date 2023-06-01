package org.rdelfino.apigateway.client;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;

/**
 * Interface for the response returned by the API Gateway service.
 */
public interface ApiGatewayResponse {

    /**
     * Returns the HTTP status text returned by the service.
     *
     * <p>If this was not provided by the service, empty will be returned.</p>
     * @return HTTP status text returned by the service.
     */
    String getStatusText();

    /**
     * Returns the HTTP status code (eg. 200, 404, etc.) returned by the service.
     *
     * <p>This will always be positive.</p>
     * @return HTTP status code returned by the service.
     */
    int getStatusCode();

    /**
     * If we get back any 2xx status code, then we know we should treat the service call as successful.
     * @return true if the service call was successful, false otherwise.
     */
    boolean isSuccessful();

    /**
     * Obtains the HTTP response body content
     *
     * @return the InputStream containing the response body content
     */
    InputStream getBodyContent();

    /**
     * Parses a JSON TreeNode from the HTTP response body content using the ObjectMapper provided in the request
     *
     * @return TreeNode parsed from the HTTP response body content
     */
    TreeNode readTreeNode();


    /**
     * Parses an Object from the HTTP response body content using the ObjectMapper provided in the request
     *
     * @param typeReference TypeReference describing the type of the object to be parsed
     * @param <Payload> expected type of the object to be parsed
     * @return Payload parsed from the HTTP response body content
     */
    <Payload> Payload readOject(TypeReference<Payload> typeReference);

    /**
     * Parses an Object from the HTTP response body content using the ObjectMapper provided in the request
     *
     * @param valueType JavaType describing the type of the object to be parsed
     * @param <Payload> expected type of the object to be parsed
     * @return Payload parsed from the HTTP response body content
     */
    <Payload> Payload readOject(JavaType valueType);

    /**
     * Parses an Object from the HTTP response body content using the ObjectMapper provided in the request
     *
     * @param payloadClass Class of the object to be parsed
     * @param <Payload> expected type of the object to be parsed
     * @return Payload parsed from the HTTP response body content
     */
    <Payload> Payload readOject(Class<Payload> payloadClass);

    /**
     * Obtains the ObjectMapper provided in the request, used to parse responses
     * @return the ObjectMapper instance used to parse responses
     */
    ObjectMapper getObjectMapper();

    /**
     * Replaces the ObjectMapper used for deserialization of responses
     * @param objectMapper the new ObjectMapper instance to be used
     * @return this ApiGatewayResponse
     */
    ApiGatewayResponse setObjectMapper(ObjectMapper objectMapper);
}
