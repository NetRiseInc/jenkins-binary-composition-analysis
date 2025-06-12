package io.jenkins.plugins.netrise.asset.uploader.api;

import io.jenkins.plugins.netrise.asset.uploader.json.JsonProperty;

/**
 * Token response instance
 * */
public record TokenResponse(
        @JsonProperty("access_token") String accessToken,
        String scope,
        @JsonProperty("expires_in") Long expiresIn,
        @JsonProperty("token_type") String tokenType
) {}
