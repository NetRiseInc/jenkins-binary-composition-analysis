package io.jenkins.plugins.netrise.asset.uploader.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Error instance
 * */
public record Error(String error, @JsonProperty("error_description") String description) {}
