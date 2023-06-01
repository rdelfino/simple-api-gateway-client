package org.rdelfino.apigateway.client;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class ApiGatewayAsyncClientTest {

    @Mock
    SignedRequestFactory mockRequestFactory;

    @Mock
    SdkAsyncHttpClient mockHttpClient;

    @Captor
    ArgumentCaptor<AsyncExecuteRequest> httpExecuteRequestArgumentCaptor;

    @Mock
    SdkHttpFullRequest mocSignedRequest;

    @BeforeEach
    @SneakyThrows
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this).close();
    }

    @Test
    @SneakyThrows
    public void testnvoke() {

        Something value = new Something();
        value.setA(24);
        value.setB("AppleBee!");

        ObjectMapper defaultMapperConfiguration = ApiGatewayRequest.getDefaultMapperConfiguration();
        String message = defaultMapperConfiguration.writeValueAsString(value);
        boolean successful = true;
        int statusCode = 200;
        String statusText = "OK";

        ApiGatewayAsyncClient client = scenario(message, successful, statusCode, statusText);

        ApiGatewayRequest testRequest = ApiGatewayRequest.builder("localhost")
                .build();

        ApiGatewayResponse response = client.invoke(
                testRequest
        ).join();

        assertNotNull(response);

        assertEquals(statusCode, response.getStatusCode());
        assertEquals(statusText, response.getStatusText());
        assertEquals(successful, response.isSuccessful());

        final TreeNode bodyContent = response.readTreeNode();
        assertEquals(value, defaultMapperConfiguration.treeToValue(bodyContent, Something.class));

        verify(mockRequestFactory).create(testRequest);
        verify(mockHttpClient).execute(httpExecuteRequestArgumentCaptor.capture());

        AsyncExecuteRequest actualRequest = httpExecuteRequestArgumentCaptor.getValue();

        assertTrue(mocSignedRequest == actualRequest.request());
    }

    @Test
    @SneakyThrows
    public void testInvokeNoContent() {
        String message = null;
        boolean successful = true;
        int statusCode = 200;
        String statusText = "OK";

        ApiGatewayAsyncClient client = scenario(message, successful, statusCode, statusText);

        ApiGatewayRequest testRequest = ApiGatewayRequest.builder("localhost")
                .build();
        ApiGatewayResponse response = client.invoke(
                testRequest
        ).join();

        final String bodyCnotent = Buffer.from(response.getBodyContent()).asString(StandardCharsets.UTF_8);
        assertEquals("", bodyCnotent);
    }

    private ApiGatewayAsyncClient scenario(String responseData, boolean successful, int statusCode, String statusText) {
        doReturn(mocSignedRequest)
            .when(mockRequestFactory)
                .create(any(ApiGatewayRequest.class));

        doAnswer(invocation -> {

            byte[] bytes = responseData == null ?  new byte[0] : responseData.getBytes(StandardCharsets.UTF_8);

            AsyncExecuteRequest request = invocation.getArgument(0);

            SdkAsyncHttpResponseHandler responseHandler = request.responseHandler();

            Map<String, List<String>> headers = new HashMap<>();
            headers.put("Content-Length", List.of(String.valueOf(bytes.length)));

            responseHandler.onHeaders(
                SdkHttpResponse.builder()
                    .statusCode(statusCode)
                    .statusText(statusText)
                    .headers(headers)
                .build()
            );

            Subscription mockSubscription = Mockito.mock(Subscription.class);

            Publisher<ByteBuffer> publisher = subscriber -> {

                subscriber.onSubscribe(mockSubscription);
                subscriber.onNext(ByteBuffer.wrap(bytes));
                subscriber.onComplete();
            };

            responseHandler.onStream(publisher);

            return CompletableFuture.completedFuture(null);

        })
            .when(mockHttpClient)
                .execute(any(AsyncExecuteRequest.class));

        return new ApiGatewayAsyncClient(mockRequestFactory, mockHttpClient);
    }
}
