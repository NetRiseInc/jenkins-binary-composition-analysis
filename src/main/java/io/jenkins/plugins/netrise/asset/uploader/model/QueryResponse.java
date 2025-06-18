package io.jenkins.plugins.netrise.asset.uploader.model;

import java.util.List;

/**
 * Query response instance
 * */
public record QueryResponse<T>(List<QueryError> errors, T data) {
}
