package net.gotzi.spigot.logging;

import net.gotzi.minestrum.api.logging.LogRegistry;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class ServerLogHandler extends Handler {

    private final LogRegistry logRegistry;

    protected ServerLogHandler(LogRegistry logRegistry) {
        this.logRegistry = logRegistry;
    }

    @Override
    public void publish(LogRecord record) {
        this.logRegistry.register(record);
    }

    @SuppressWarnings("unused")
    @Override
    public void flush() {}

    @SuppressWarnings("unused")
    @Override
    public void close() throws SecurityException {}
}
