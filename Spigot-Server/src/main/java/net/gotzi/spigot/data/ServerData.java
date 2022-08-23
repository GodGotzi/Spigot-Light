package net.gotzi.spigot.data;

import java.util.UUID;

public class ServerData {

    private final UUID uuid;
    private final GameData gameData;
    private final Object[] objects;

    public ServerData(
            UUID uuid,
            GameData gameData,
            Object... objects
    ) {
        this.uuid = uuid;
        this.gameData = gameData;
        this.objects = objects;
    }

    public GameData getGameData() {
        return gameData;
    }

    public Object[] getObjects() {
        return objects;
    }

    public UUID getID() {
        return uuid;
    }
}
