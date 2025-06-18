package io.jenkins.plugins.netrise.asset.uploader;

import hudson.model.Run;
import jenkins.model.RunAction2;

public class SimpleAction implements RunAction2 {

    private final String name;
    private final String assetId;
    private transient Run<?, ?> run;

    public SimpleAction(String name, String assetId) {
        this.name = name;
        this.assetId = assetId;
    }

    public String getName() {
        return name;
    }

    public String getAssetId() {
        return assetId;
    }

    @Override
    public void onAttached(Run<?, ?> run) {
        this.run = run;
    }

    @Override
    public void onLoad(Run<?, ?> run) {
        this.run = run;
    }

    public Run<?, ?> getRun() {
        return run;
    }

    @Override
    public String getIconFileName() {
        return "document.png";
    }

    @Override
    public String getDisplayName() {
        return "Netrise Upload Details";
    }

    @Override
    public String getUrlName() {
        return "netriseUploadDetails";
    }
}