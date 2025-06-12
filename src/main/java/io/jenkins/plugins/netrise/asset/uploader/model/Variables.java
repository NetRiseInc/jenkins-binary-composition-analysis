package io.jenkins.plugins.netrise.asset.uploader.model;

/**
 * Query variables instance
 * */
public class Variables<T> {
    private final T args;

    public Variables(T args) {
        this.args = args;
    }

    public T getArgs() {
        return args;
    }
}
