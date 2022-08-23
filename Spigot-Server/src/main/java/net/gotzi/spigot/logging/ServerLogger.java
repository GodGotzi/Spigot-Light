package net.gotzi.spigot.logging;

import net.gotzi.minestrum.api.logging.LoggerInterface;

import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class ServerLogger extends Logger implements LoggerInterface {

    protected ServerLogger(String name) {
        super(name, null);

        setUseParentHandlers(false);
        //addHandler();
    }

    @Override
    public void doLog(LogRecord record) {

    }
}
