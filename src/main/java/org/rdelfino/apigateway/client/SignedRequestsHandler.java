package org.rdelfino.apigateway.client;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
class SignedRequestsHandler {

    final @NonNull SignedRequestFactory signedRequestFactory;

    /**
     * Obtains the signing region
     * @return signing region
     */
    public Region getSigningRegion() {
        return signedRequestFactory.getSigningRegion();
    }

    /**
     * Obtains the credentials provider
     * @return AwsCredentialsProvider
     */
    public AwsCredentialsProvider getCredentialsProvider() {
        return signedRequestFactory.getCredentialsProvider();
    }
}
