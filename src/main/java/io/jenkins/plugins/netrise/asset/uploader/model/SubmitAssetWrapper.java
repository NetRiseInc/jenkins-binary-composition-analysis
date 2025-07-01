package io.jenkins.plugins.netrise.asset.uploader.model;

public record SubmitAssetWrapper<T> (SubmitWrapper<T> asset) {
    public T getData() {
        return asset() != null ? this.asset().submit() : null;
    }
}

record SubmitWrapper<T> (T submit) {}
