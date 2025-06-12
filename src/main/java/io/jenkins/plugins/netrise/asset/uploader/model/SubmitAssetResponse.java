package io.jenkins.plugins.netrise.asset.uploader.model;

/**
 * Asset submit response data instance
 * */
public record SubmitAssetResponse(String uploadId, String uploadUrl) {
}
