package net.gotzi.spigot.logging;

import net.gotzi.minestrum.api.registry.history.History;
import net.gotzi.minestrum.api.server.logging.ServerLogManagerInterface;

import java.util.Map;

public class ServerLogManager implements ServerLogManagerInterface {

    private static ServerLogManager serverLogManager = new ServerLogManager();


    public ServerLogManager() {

    }

    @Override
    public Map<Long, History<String>> getHistories() {
        return null;
    }


}
