package io.jenkins.plugins.netrise.asset.uploader.model;

/**
 * Asset submit input data instance
 * */
public record SubmitAssetInput(String name, String model, String version, String manufacturer) {
}
