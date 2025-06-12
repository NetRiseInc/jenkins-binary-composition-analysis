package io.jenkins.plugins.netrise.asset.uploader.log;

import java.io.OutputStream;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

public class JobLoggerHandler extends StreamHandler {
    public JobLoggerHandler(OutputStream out) {
        super(out, new SimpleFormatter());
    }
}
