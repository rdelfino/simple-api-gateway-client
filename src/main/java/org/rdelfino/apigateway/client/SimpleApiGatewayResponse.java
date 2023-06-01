package org.rdelfino.apigateway.client;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.ToString;

import java.io.InputStream;

@Getter
@Builder
@EqualsAndHashCode
@ToString
@AllArgsConstructor
class SimpleApiGatewayResponse implements ApiGatewayResponse {

    ObjectMapper objectMapper;

    final String statusText;
    final int statusCode;
    final boolean successful;
    final InputStream bodyContent;

    @Override
    @SneakyThrows
    public TreeNode readTreeNode() {
        return objectMapper.readTree(getBodyContent());
    }

    @Override
    @SneakyThrows
    public <Payload> Payload readOject(final @NonNull Class<Payload> payloadClass) {
        return objectMapper.readValue(getBodyContent(), payloadClass);
    }

    @Override
    @SneakyThrows
    public <Payload> Payload readOject(final @NonNull TypeReference<Payload> typeReference){
        return objectMapper.readValue(getBodyContent(), typeReference);
    }

    @Override
    @SneakyThrows
    public <Payload> Payload readOject(final @NonNull JavaType valueType){
        return objectMapper.readValue(getBodyContent(), valueType);
    }

    @Override
    public ApiGatewayResponse setObjectMapper(final @NonNull ObjectMapper mapper) {
        this.objectMapper = mapper;
        return this;
    }
}
