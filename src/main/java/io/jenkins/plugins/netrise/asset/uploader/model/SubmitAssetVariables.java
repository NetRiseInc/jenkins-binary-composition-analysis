package io.jenkins.plugins.netrise.asset.uploader.model;

/**
 * Asset submit query variables instance (extra field)
 * */
public class SubmitAssetVariables<T> extends Variables<T> {
    private final String fileName;

    public SubmitAssetVariables(T args, String fileName) {
        super(args);
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
