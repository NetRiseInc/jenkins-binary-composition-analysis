package io.jenkins.plugins.netrise.asset.uploader.api;

import hudson.ProxyConfiguration;

import java.net.URI;
import java.net.http.HttpRequest;

/**
 * Authorise and make requests to the NetRise API with a proxy support using a preconfigured HttpRequestBuilder instance
 * */
public class ProxyClient extends Client {

    /**
     * Configure the API Client
     *
     * @param tokenUri The authentication url
     * @param organization Organization ID
     * @param clientId Client ID
     * @param clientSecret Client Secret
     * @param audience Audience
     * */
    public ProxyClient(URI tokenUri, String organization, String clientId, String clientSecret, String audience) {
        super(tokenUri, organization, clientId, clientSecret, audience);
    }

    @Override
    protected HttpRequest.Builder getRequestBuilder(URI uri) {
        return ProxyConfiguration.newHttpRequestBuilder(uri);
    }
}
