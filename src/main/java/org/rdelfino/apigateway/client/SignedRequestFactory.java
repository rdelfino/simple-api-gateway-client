package org.rdelfino.apigateway.client;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.internal.SignerConstant;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.auth.signer.params.SignerChecksumParams;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Factory for creating signed requests for the ApiGatewayClient
 */
@Builder
@RequiredArgsConstructor
class SignedRequestFactory {

    private static final Aws4Signer SIGNER = Aws4Signer.create();

    private static final SignerChecksumParams SIGNER_CHECKSUM_PARAMS =
        SignerChecksumParams.builder()
            .algorithm(Algorithm.SHA256)
            .isStreamingRequest(false)
            .checksumHeaderName(SignerConstant.X_AMZ_CONTENT_SHA256)
        .build();

    @Getter
    final @NonNull Region signingRegion;

    @Getter
    final @NonNull AwsCredentialsProvider credentialsProvider;

    /**
     * Create a signed request for the given ApiGatewayRequest.
     * @param apiGatewayRequest ApiGatewayRequest
     * @return signed SdkHttpFullRequest instance
     */
    SdkHttpFullRequest create(ApiGatewayRequest apiGatewayRequest) {

        final Buffer payloadBuffer = Optional.ofNullable(apiGatewayRequest.getPayload())
                .map(Buffer::from)
                .orElse(null);

        final ContentStreamProvider contentStreamProvider = payloadBuffer == null ? null : payloadBuffer::asInputStream;

        final SdkHttpFullRequest.Builder builder = SdkHttpFullRequest.builder()
                .protocol("https")
                .method(SdkHttpMethod.fromValue(apiGatewayRequest.getMethod()))
                .host(apiGatewayRequest.getHost())
                .port(apiGatewayRequest.getPort())
                .encodedPath(apiGatewayRequest.getPath())
                .contentStreamProvider(contentStreamProvider)
                .headers(
                    finalHeaders(apiGatewayRequest, payloadBuffer)
                )
                .rawQueryParameters(
                    nonNull(apiGatewayRequest.getQueryParameters())
                );

        return SIGNER.sign(
            builder.build(),
            Aws4SignerParams.builder()
                .signingName("execute-api")
                .signingRegion(signingRegion)
                .awsCredentials(credentialsProvider.resolveCredentials())
                .checksumParams(SIGNER_CHECKSUM_PARAMS)
            .build()
        );
    }

    private Map<String, List<String>> finalHeaders(
        final ApiGatewayRequest apiGatewayRequest,
        final Buffer payloadBuffer
    ) {
        final Map<String, List<String>> result = new HashMap<>(nonNull(apiGatewayRequest.getHeaders()));

        if (payloadBuffer != null) {
            result.put("Content-Length", List.of(String.valueOf(payloadBuffer.size())));
        }

        final String apiKey = apiGatewayRequest.getApiKey();
        if (apiKey != null) {
            result.put("x-api-key", List.of(apiKey));
        }

        return result;
    }

    private ContentStreamProvider contentProvider(final ApiGatewayRequest parameters) {
        return Optional.ofNullable(parameters.getPayload())
                .map(Buffer::from)
                .map(buffer->(ContentStreamProvider)buffer::asInputStream)
                .orElse(null);
    }

    private Map<String, List<String>> nonNull(final Map<String, List<String>> map) {
        return Optional.ofNullable(map)
            .orElseGet(Collections::emptyMap);
    }
}
