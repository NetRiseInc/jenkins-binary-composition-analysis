package io.jenkins.plugins.netrise.asset.uploader.service;

import io.jenkins.plugins.netrise.asset.uploader.api.Client;
import io.jenkins.plugins.netrise.asset.uploader.log.Logger;
import io.jenkins.plugins.netrise.asset.uploader.model.*;

import java.net.URI;
import java.nio.file.Path;

/**
 * Service to upload asset.
 * Further may contain other functionality.
 * */
public class UploadService {
    private static final Logger log = Logger.getLogger(UploadService.class);

    public static final int UPLOAD_RETRY_NUMBER = 3;
    public static final int UPLOAD_STATUS_CHECK_TIMEOUT = 5 * 1000; // 5 seconds
    public static final int UPLOAD_CHECK_STATUS_MAX_NUMBER = 10;

    private final Client client;
    private final URI uri;

    /**
     * Configure the service and underlying API Client
     *
     * @param baseUri The endpoint
     * @param tokenUri The authentication url
     * @param clientId Client ID
     * @param clientSecret Client Secret
     * @param audience Audience
     * */
    public UploadService(URI baseUri, URI tokenUri, String organization, String clientId, String clientSecret, String audience) {
        client = new Client(tokenUri, organization, clientId, clientSecret, audience);
        uri = baseUri;
    }

    /**
     * Upload asset to the API with metadata
     *
     * @param file The path to the file
     * @param input Asset metadata
     *
     * @return Asset ID if upload is successful
     * */
    public String upload(Path file, SubmitAssetInput input) {
        log.debug("Start file uploading...");
        SubmitAssetResponse response = client.post(uri,
                new Query<>(Queries.SUBMIT_ASSET_QUERY, new SubmitAssetVariables<>(
                        input, file.getFileName().toString()))).asJson(SubmitAssetResponse.class);

        log.debug("Obtained uploadId / uploadUrl:", response.uploadId(), "/", response.uploadUrl());
        URI uploadUri = URI.create(response.uploadUrl());
        int uploadStatus = uploadFile(uploadUri, file);

        int uploadRetry = 0;
        while (uploadStatus != 200 && uploadRetry++ < UPLOAD_RETRY_NUMBER) {
            uploadStatus = uploadFile(uploadUri, file);
        }

        if (uploadStatus != 200) {
            log.error("Couldn't upload the file to the server", file, ". Upload URL:", uploadUri);
            throw new UploadException("Couldn't upload the file to the server");
        }

        long retry = 0;
        log.debug("Check if uploading is finished:", response.uploadId());

        while (true) {
            AssetUploadResponse uploadResponse = client.post(uri,
                            new Query<>(Queries.ASSET_UPLOAD_QUERY, new Variables<>(
                                    new AssetUploadInput(response.uploadId()))))
                    .asJson(AssetUploadResponse.class);
            boolean uploaded = Boolean.TRUE.equals(uploadResponse.uploaded());
            retry ++;
            if (uploaded) {
                log.debug("The file is uploaded. Asset ID:", uploadResponse.assetId());
                return uploadResponse.assetId();
            } else if (retry > UPLOAD_CHECK_STATUS_MAX_NUMBER) {
                throw new UploadException("Couldn't check the upload status after " + UPLOAD_CHECK_STATUS_MAX_NUMBER + " tries");
            } else {
                log.debug(retry, "retry check if file is uploaded", response.uploadId());
                try {
                    Thread.sleep(UPLOAD_STATUS_CHECK_TIMEOUT);
                } catch (InterruptedException e) {
                    log.error("File upload status check is failed: " + uploadResponse.assetId(), e);
                    throw new UploadException(e.getLocalizedMessage());
                }
            }
        }
    }

    protected int uploadFile(URI uploadUri, Path path) {
        return client.upload(uploadUri, path).getStatusCode();
    }

}
