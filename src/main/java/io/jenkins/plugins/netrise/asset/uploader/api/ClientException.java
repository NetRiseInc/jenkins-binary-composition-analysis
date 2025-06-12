package io.jenkins.plugins.netrise.asset.uploader.api;

/**
 * API Client exception
 * */
public class ClientException extends RuntimeException {
    private String description;

    public ClientException(String message) {
        super(message);
    }

    public ClientException(String message, String description) {
        super(message);
        this.description = description;
    }

    public ClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getDescription() {
        return description;
    }
}
