package org.rdelfino.apigateway.client;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;

/**
 * ApiGatewayClient is a client for the AWS API Gateway that uses the synchronous request processing model
 *
 * Example:
 * <pre>
 *
 *     ///////////////
 *     // Initialization
 *     //
 *
 *     AwsCredentialsProvider credentialsProvider = obtainCredentialsProvider();
 *
 *     SdkHttpClient httpClient = obtainHttpClient();
 *         // ex:  ApacheHttpClient.builder().socketTimeout(Duration.ofSeconds(10)).build();
 *
 *     ApiGatewayClient client = ApiGatewayClient.builder()
 *         .httpClient(httpClient)
 *         .credentialsProvider(credentialsProvider)
 *         .signingRegion(Region.US_WEST_2)
 *         .build();
 *
 *     ///////////////
 *     // usage
 *     //
 *
 *     Item item = new Item();
 *
 *     ApiGatewayRequest request =
 *         ApiGatewayRequest.builder(URI.create("https://api.example.com/v1/items"))
 *             .setMethod("PUT")
 *             .setQueryParameter("id", "123")
 *             .setJsonPayload(item)
 *         build();
 *
 *     ApiGatewayResponse response = client.invoke(request);
 *
 *     Item createdItem = response.readObject(Item.class);
 *
 * </pre>
 */
public class ApiGatewayClient extends SignedRequestsHandler {

    @Getter
    final SdkHttpClient httpClient;

    @Builder
    ApiGatewayClient(
        final @NonNull SdkHttpClient httpClient,
        final AwsCredentialsProvider credentialsProvider,
        final Region region
    ) {
        this(
            SignedRequestFactory.builder()
                .signingRegion(region)
                .credentialsProvider(credentialsProvider)
            .build(),
            httpClient
        );
    }

    ApiGatewayClient(
            final SignedRequestFactory signedRequestFactory,
            final @NonNull SdkHttpClient httpClient
    ) {
        super(signedRequestFactory);
        this.httpClient = httpClient;
    }

    /**
     * Invokes the API Gateway with a request
     *
     * @param apiGatewayRequest The request to send to the API Gateway endpoint
     *
     * @return The response from the API Gateway endpoint
     * @throws IOException if the request cannot be sent
     */
    public ApiGatewayResponse invoke(
        @NonNull ApiGatewayRequest apiGatewayRequest
    ) throws IOException {

        final SdkHttpFullRequest sdkRequest = signedRequestFactory.create(apiGatewayRequest);

        final ExecutableHttpRequest executableHttpRequest =
                httpClient.prepareRequest(
                    HttpExecuteRequest.builder()
                        .request(sdkRequest)
                        .contentStreamProvider(
                            sdkRequest.contentStreamProvider()
                                .orElse(null)
                        )
                    .build()
                );

        final HttpExecuteResponse executeResponse = executableHttpRequest.call();

        final SdkHttpResponse httpResponse = executeResponse.httpResponse();

        final Buffer buffer = new Buffer(512);
        executeResponse.responseBody()
            .ifPresent(buffer::transferFrom);

        return SimpleApiGatewayResponse.builder()
                .objectMapper(apiGatewayRequest.getMapper())
                .bodyContent(buffer.asInputStream())
                .successful(httpResponse.isSuccessful())
                .statusCode(httpResponse.statusCode())
                .statusText(httpResponse.statusText().orElse(null))
            .build();
    }
}
