package io.jenkins.plugins.netrise.asset.uploader.service;

/**
 * Upload service exception
 * */
public class UploadException extends RuntimeException {
    public UploadException(String message) {
        super(message);
    }

    public UploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
