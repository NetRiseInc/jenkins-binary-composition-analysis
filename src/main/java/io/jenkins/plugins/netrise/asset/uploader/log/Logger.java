package io.jenkins.plugins.netrise.asset.uploader.log;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Logger {

    public static Logger getLogger(String name) {
        return new Logger(name);
    }

    public static Logger getLogger(Class<?> clz) {
        return new Logger(clz != null ? clz.getName() : null);
    }

    private static final List<Logger> loggers = new ArrayList<>();

    public static void addHandler(Handler handler) {
        loggers.forEach(l -> l.addHandler0(handler));
    }

    private final java.util.logging.Logger logger;

    public Logger(String name) {
        logger = java.util.logging.Logger.getLogger(name);
        logger.setLevel(Level.ALL);

        Handler consoleHandler = new ConsoleHandler() {
            {
                setOutputStream(System.out);
            }
        };
        consoleHandler.setLevel(Level.FINE);
        logger.addHandler(consoleHandler);

        loggers.add(this);
    }

    private void addHandler0(Handler handler) {
        logger.addHandler(handler);
    }

    private void log(Level level, Object... msg) {
        logger.log(level, msg != null ? Stream.of(msg).map(Objects::toString).collect(Collectors.joining(" ")) : "null");
    }

    public void debug(Object... msg) {
        log(Level.FINE, msg);
    }

    public void info(Object... msg) {
        log(Level.INFO, msg);
    }

    public void warn(Object... msg) {
        log(Level.WARNING, msg);
    }

    public void error(Object msg, Throwable e) {
        log(Level.SEVERE, msg, e.getLocalizedMessage(), Stream.of(e.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("\n")));
    }

    public void error(Object... msg) {
        log(Level.SEVERE, msg);
    }
}
