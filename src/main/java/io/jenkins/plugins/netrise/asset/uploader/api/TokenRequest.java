package io.jenkins.plugins.netrise.asset.uploader.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Token request instance
 * */
public record TokenRequest(
        String organization,
        @JsonProperty("client_id") String clientId,
        @JsonProperty("client_secret") String clientSecret,
        @JsonProperty("grant_type") String grantType,
        String audience
) {
    @Override
    public String toString() {
        return String.format("AuthRequest[clientId=%s, grandType=%s]", clientId, grantType);
    }
}