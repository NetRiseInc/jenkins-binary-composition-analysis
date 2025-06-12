package io.jenkins.plugins.netrise.asset.uploader.model;

import io.jenkins.plugins.netrise.asset.uploader.json.JsonProperty;

/**
 * Error instance
 * */
public record Error(String error, @JsonProperty("error_description") String description) {}
