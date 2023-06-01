package org.rdelfino.apigateway.client;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.regions.Region;

import java.io.ByteArrayInputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

class SignedRequestFactoryTest {

    @Mock
    AwsCredentialsProvider mockCredentialsProvider;

    @BeforeEach
    @SneakyThrows
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this).close();


        doReturn(new AwsCredentials(){
            @Override
            public String accessKeyId() {
                return "123";
            }

            @Override
            public String secretAccessKey() {
                return "456";
            }
        }).when(mockCredentialsProvider).resolveCredentials();
    }

    @Test
    @SneakyThrows
    public void testCreate() {
        String content = "payload";

        ApiGatewayRequest request = scenario(content);

        SignedRequestFactory factory = SignedRequestFactory.builder()
                .signingRegion(Region.US_WEST_2)
                .credentialsProvider(mockCredentialsProvider)
            .build();

        SdkHttpFullRequest sdkHttpFullRequest = factory.create(request);

        verify(mockCredentialsProvider).resolveCredentials();

        assertNotNull(sdkHttpFullRequest);
        assertEquals(request.getHost(), sdkHttpFullRequest.host());
        assertEquals(request.getPort(), sdkHttpFullRequest.port());
        assertEquals(request.getMethod(), sdkHttpFullRequest.method().name());
        assertEquals(request.getPath(), sdkHttpFullRequest.encodedPath());
        assertEquals(request.queryParameters, sdkHttpFullRequest.rawQueryParameters());
        assertEquals(request.headers.get("h1"), sdkHttpFullRequest.headers().get("h1"));
        assertNotNull(sdkHttpFullRequest.contentStreamProvider().orElse(null));
    }

    @Test
    @SneakyThrows
    public void testCreateNoContent() {
        ApiGatewayRequest request = scenario(null);

        SignedRequestFactory factory = SignedRequestFactory.builder()
                .signingRegion(Region.US_WEST_2)
                .credentialsProvider(mockCredentialsProvider)
                .build();

        SdkHttpFullRequest sdkHttpFullRequest = factory.create(request);

        verify(mockCredentialsProvider).resolveCredentials();

        assertNotNull(sdkHttpFullRequest);
        assertEquals(request.getHost(), sdkHttpFullRequest.host());
        assertEquals(request.getPort(), sdkHttpFullRequest.port());
        assertEquals(request.getMethod(), sdkHttpFullRequest.method().name());
        assertEquals(request.getPath(), sdkHttpFullRequest.encodedPath());
        assertEquals(request.queryParameters, sdkHttpFullRequest.rawQueryParameters());
        assertEquals(request.headers.get("h1"), sdkHttpFullRequest.headers().get("h1"));
        assertNull(sdkHttpFullRequest.contentStreamProvider().orElse(null));
    }

    private ApiGatewayRequest scenario(String content) {
        return ApiGatewayRequest
                .builder("localhost")
                .setPort(444)
                .setMethod("PUT")
                .setPath("/path")
                .addQueryParameter("q1", "v1")
                .addHeader("h1", "v1")
                .setPayload(
                    Optional.ofNullable(content)
                        .map(s->new ByteArrayInputStream(s.getBytes()))
                        .orElse(null)
                )
            .build();
    }

}