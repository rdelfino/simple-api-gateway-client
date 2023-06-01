package org.rdelfino.apigateway.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.regions.Region;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * ApiGatewayAsyncClient is a client for the AWS API Gateway that uses the asynchronous request processing model
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
 *     SdkAsyncHttpClient httpClient = obtainAsyncHttpClient();
 *         // ex:  NettyNioAsyncHttpClient.builder().connectionTimeout(Duration.ofSeconds(10)).build();
 *
 *     ApiGatewayAsyncClient client = ApiGatewayAsyncClient.builder()
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
 *     CompletableFuture<ApiGatewayResponse> futureResponse = client.invoke(request);
 *
 *     ApiGatewayResponse response = futureResponse.join();
 *
 *     Item createdItem = response.readObject(Item.class);
 *
 * </pre>
 */
public class ApiGatewayAsyncClient extends SignedRequestsHandler {

    private static final List<String> EMPTY_CONTENT_LENGTH = List.of("-1");

    @Getter
    final SdkAsyncHttpClient httpClient;

    @Builder
    ApiGatewayAsyncClient(
        final @NonNull SdkAsyncHttpClient httpClient,
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

    ApiGatewayAsyncClient(
            final SignedRequestFactory signedRequestFactory,
            final @NonNull SdkAsyncHttpClient httpClient
    ) {
        super(signedRequestFactory);
        this.httpClient = httpClient;
    }

    /**
     * Invoke the API Gateway asynchronously
     * @param apiGatewayRequest ApiGatewayRequest
     * @return CompletableFuture from which to obtain the response
     */
    public CompletableFuture<ApiGatewayResponse> invoke(
        final @NonNull ApiGatewayRequest apiGatewayRequest
    ) {

        final SdkHttpFullRequest request = signedRequestFactory.create(apiGatewayRequest);

        final AsyncRequestBody payloadBody =
                AsyncRequestBody.fromByteBuffer(
                    request.contentStreamProvider()
                        .map(ContentStreamProvider::newStream)
                        .map(Buffer::from)
                        .map(Buffer::asByteBuffer)
                        .orElseGet(() -> ByteBuffer.wrap(new byte[0]))
                );

        final AsyncResponseHandler responseHandler =
                new AsyncResponseHandler(apiGatewayRequest.getMapper());

        final AsyncExecuteRequest executeRequest =
            AsyncExecuteRequest.builder()
                .fullDuplex(false)
                .request(request)
                .responseHandler(responseHandler)
                .requestContentPublisher(
                    new SdkHttpContentPublisher() {
                        @Override
                        public Optional<Long> contentLength() {
                            return payloadBody.contentLength();
                        }

                        @Override
                        public void subscribe(Subscriber<? super ByteBuffer> s) {
                            payloadBody.subscribe(s);
                        }
                    }
                )
            .build();

        return httpClient.execute(executeRequest)
                .thenCombine(
                    responseHandler.future,
                    (v, response) -> response
                );
    }

    private static class AsyncResponseHandler implements SdkAsyncHttpResponseHandler, Subscriber<ByteBuffer> {

        final ObjectMapper mapper;

        AsyncResponseHandler(final ObjectMapper mapper) {
            this.mapper = mapper;
        }

        private String statusText;
        private int statusCode;
        private Map<String, List<String>> headers;
        private boolean isSuccessful;
        private Subscription subscription;

        int remainingBytes = -1;

        private Buffer buffer = new Buffer(256);

        final CompletableFuture<ApiGatewayResponse> future = new CompletableFuture<>();

        @Override
        public void onSubscribe(final Subscription subscription) {
            this.subscription = subscription;
            if (remainingBytes > 0) {
                subscription.request(remainingBytes);
            }
        }

        @Override
        public void onHeaders(SdkHttpResponse sdkHttpResponse) {
            this.statusText = sdkHttpResponse.statusText().orElse(null);
            this.statusCode = sdkHttpResponse.statusCode();
            this.isSuccessful = sdkHttpResponse.isSuccessful();
            this.headers = sdkHttpResponse.headers();

            this.remainingBytes = Integer.parseInt(
                headers.getOrDefault("Content-Length", EMPTY_CONTENT_LENGTH).get(0)
            );
        }

        @Override
        public void onStream(Publisher<ByteBuffer> contentPublisher) {
            contentPublisher.subscribe(this);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            remainingBytes -= byteBuffer.limit();
            buffer.write(byteBuffer.array(), 0, byteBuffer.limit());
            if (remainingBytes > 0) {
                subscription.request(remainingBytes);
            }
        }

        @Override
        public void onError(Throwable error) {
            future.completeExceptionally(error);
        }

        @Override
        public void onComplete() {
            future.complete(
                SimpleApiGatewayResponse.builder()
                    .objectMapper(mapper)
                    .statusText(statusText)
                    .statusCode(statusCode)
                    .successful(isSuccessful)
                    .bodyContent(buffer.asInputStream())
                .build()
            );
        }
    }
}
