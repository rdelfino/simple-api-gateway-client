# simple-api-gateway-client
Simple AWS Api Gateway Client

This is a simple Java client for Amazon API Gateway endpoints built around AWS SDK 2.x. It is useful when you don't 
necessarily want to generate a [strongly-typed SDK](https://aws.amazon.com/blogs/developer/api-gateway-java-sdk), such 
as when prototyping or scripting.

The client supports both synchronous and asynchronous request/response processing models. While the client offers easy 
[Jackson JSON](https://github.com/FasterXML/jackson) processing capabilities, it does not make any assumptions about 
the wire format of your requests and responses. You are free to parse response bodies as you see fit, and the raw HTTP 
response data is included in the 
wrapped responses.

## Features
* AWS SigV4 request signing. Supports APIs authenticated with IAM auth using standard AWSCredentialsProvider interface
* API Keys
* Custom headers
* Jackson JSON support
  * sensible ObjectMapper default configuration
* Compatibility with existing AWS SDK 2.x client configuration

## Install

### Maven
```
<dependency>
  <groupId>io.github.rdelfino</groupId>
  <artifactId>simple-api-gateway-client</artifactId>
  <version>1.0.0</version>
</dependency>
```

Library dependencies:
* org.projectlombok:lombok: 1.18.0+
* com.fasterxml.jackson.core:jackson-core: 2.0.0+
* com.fasterxml.jackson.corejackson-databind: 2.0.0+
* software.amazon.awssdk:http-client-spi: 2.4.0+
* software.amazon.awssdk:auth: 2.4.0+

### From source
```bash
git clone https://github.com/rdelfino/simple-aws-api-gateway-client.git

# Optional:
cd simple-aws-api-gateway-client

mvn install

```

## Examples

### Synchronous client

```java

///////////////
// Initialization
//

AwsCredentialsProvider credentialsProvider = obtainCredentialsProvider();

SdkHttpClient httpClient = obtainHttpClient();
    // ex:  ApacheHttpClient.builder().socketTimeout(Duration.ofSeconds(10)).build();

ApiGatewayClient client = ApiGatewayClient.builder()
    .httpClient(httpClient)
    .credentialsProvider(credentialsProvider)
    .signingRegion(Region.US_WEST_2)
    .build();

///////////////
// usage
//

Item item = new Item();

ApiGatewayRequest request =
    ApiGatewayRequest.builder(URI.create("https://api.example.com/v1/items"))
        .setMethod("PUT")
        .setQueryParameter("id", "123")
        .setJsonPayload(item)
    build();

ApiGatewayResponse response = client.invoke(request);

if (response.getStatusCode() == 200) {
    Item createdItem = response.readObject(Item.class);
    // . . .
}
else{
    System.out.println("Error: "+ response.getStatusText());
    // . . .
}

```

### Asynchronous client 

```java

///////////////
// Initialization
//

AwsCredentialsProvider credentialsProvider = obtainCredentialsProvider();

SdkAsyncHttpClient httpClient = obtainAsyncHttpClient();
    // ex:  NettyNioAsyncHttpClient.builder().connectionTimeout(Duration.ofSeconds(10)).build();

ApiGatewayAsyncClient client = ApiGatewayAsyncClient.builder()
    .httpClient(httpClient)
    .credentialsProvider(credentialsProvider)
    .signingRegion(Region.US_WEST_2)
    .build();

///////////////
// usage
//

Item item = new Item();

ApiGatewayRequest request =
    ApiGatewayRequest.builder(URI.create("https://api.example.com/v1/items"))
        .setMethod("PUT")
        .setQueryParameter("id", "123")
        .setJsonPayload(item)
    build();

CompletableFuture<ApiGatewayResponse> futureResponse = client.invoke(request);

ApiGatewayResponse response = futureResponse.join();

if (response.getStatusCode() == 200) {
    Item createdItem = response.readObject(Item.class);
    // . . .
}
else{
    System.out.println("Error: "+ response.getStatusText());
    // . . .
}

```
