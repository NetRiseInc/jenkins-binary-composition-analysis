package io.jenkins.plugins.netrise.asset.uploader.model;

/**
 * GraphQL query instance
 * */
public record Query<T>(String query, Variables<T> variables) {
}
