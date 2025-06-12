package io.jenkins.plugins.netrise.asset.uploader.model;

/**
 * Asset upload response data instance
 * */
public record AssetUploadResponse(String uploadId, String assetId, Boolean uploaded) {
}
