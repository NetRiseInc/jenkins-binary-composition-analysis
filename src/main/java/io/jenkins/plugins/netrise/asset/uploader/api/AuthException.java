package io.jenkins.plugins.netrise.asset.uploader.api;

/**
 * Authentication exception
 * */
public class AuthException extends ClientException {
    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
