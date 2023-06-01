package org.rdelfino.apigateway.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpResponse;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

class ApiGatewayClientTest {

    @Mock
    SignedRequestFactory mockRequestFactory;
    @Mock
    SdkHttpClient mockHttpClient;

    @Captor
    ArgumentCaptor<ApiGatewayRequest> apiGatewayRequestArgumentCaptor;

    @Captor
    ArgumentCaptor<HttpExecuteRequest> httpExecuteRequestArgumentCaptor;

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

        ObjectMapper mapper = ApiGatewayRequest.getDefaultMapperConfiguration();

        Something value = new Something();
        value.setA(15);
        value.setB("test123");

        boolean successful = true;
        int statusCode = 200;
        String statusText = "OK";

        ApiGatewayClient client = scenario(mapper.writeValueAsString(value), successful, statusCode, statusText);

        ApiGatewayRequest testRequest = ApiGatewayRequest.builder("localhost")
                .build();
        ApiGatewayResponse response = client.invoke(
                testRequest
        );

        assertNotNull(response);

        assertEquals(statusCode, response.getStatusCode());
        assertEquals(statusText, response.getStatusText());
        assertEquals(successful, response.isSuccessful());

        final Something bodyContent = response.readOject(Something.class);
        assertEquals(value, bodyContent);

        verify(mockRequestFactory).create(testRequest);
        verify(mockHttpClient).prepareRequest(httpExecuteRequestArgumentCaptor.capture());

        HttpExecuteRequest actualRequest = httpExecuteRequestArgumentCaptor.getValue();

        assertTrue(mocSignedRequest == actualRequest.httpRequest());
    }

    @Test
    @SneakyThrows
    public void testInvokeNoContent() {
        String message = null;
        boolean successful = true;
        int statusCode = 200;
        String statusText = "OK";

        ApiGatewayClient client = scenario(message, successful, statusCode, statusText);

        ApiGatewayRequest testRequest = ApiGatewayRequest.builder("localhost")
                .build();
        ApiGatewayResponse response = client.invoke(
                testRequest
        );

        final String bodyCnotent = Buffer.from(response.getBodyContent()).asString(StandardCharsets.UTF_8);
        assertEquals("", bodyCnotent);
    }

    private ApiGatewayClient scenario(String message, boolean successful, int statusCode, String statusText) {
        doReturn(mocSignedRequest)
            .when(mockRequestFactory)
                .create(any(ApiGatewayRequest.class));

        doAnswer(invocation -> {
            HttpExecuteRequest request = (HttpExecuteRequest)invocation.getArgument(0);

            ExecutableHttpRequest mockExecutableRequest = Mockito.mock(ExecutableHttpRequest.class);

            HttpExecuteResponse mockExecuteResponse = Mockito.mock(HttpExecuteResponse.class);

            doReturn(mockExecuteResponse)
                .when(mockExecutableRequest)
                    .call();

            doReturn(
                Optional.ofNullable(message)
                    .map(s-> new ByteArrayInputStream(message.getBytes()))
                    .map(AbortableInputStream::create)
            )
                .when(mockExecuteResponse)
                    .responseBody();

            SdkHttpResponse mockHhttpResponse = Mockito.mock(SdkHttpResponse.class);

            doReturn(mockHhttpResponse)
                .when(mockExecuteResponse)
                    .httpResponse();

            doReturn(successful)
                .when(mockHhttpResponse)
                    .isSuccessful();

            doReturn(statusCode)
                .when(mockHhttpResponse)
                .statusCode();

            doReturn(Optional.ofNullable(statusText))
                    .when(mockHhttpResponse)
                    .statusText();

            return mockExecutableRequest;

        })
            .when(mockHttpClient)
            .prepareRequest(any(HttpExecuteRequest.class));

        return new ApiGatewayClient(mockRequestFactory,mockHttpClient);
    }
}