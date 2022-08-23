package net.minecraft.server.dedicated;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import joptsimple.OptionSet;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.level.levelgen.GeneratorSettings;

public class DedicatedServerProperties extends Properties {
    
    public final EnumDifficulty difficulty;
    public final EnumGamemode gamemode;
    
    public final boolean debug;
    
    public final boolean onlineMode;
    
    public final boolean preventProxyConnections;
    
    public final String serverIp;
    
    public final boolean spawnAnimals;
    
    public final boolean spawnNpcs;
    
    public final boolean pvp;
    
    public final boolean allowFlight;
    
    public final String resourcePack;
    
    public final String motd;
    
    public final boolean forceGamemode;
    
    public final boolean enforceWhitelist;
    
    public final String levelName;
    
    public final int serverPort;
    
    public final int maxBuildHeight;
    
    public final Boolean announcePlayerAchievements;
    
    public final boolean enableQuery;
    
    public final int queryPort;
    public final boolean enableRcon;
    
    public final int rconPort;
    
    public final String rconPassword;
    
    public final String resourcePackHash;
    
    public final String resourcePackSha1;
    
    public final boolean hardcore;
    
    public final boolean allowNether;
    
    public final boolean spawnMonsters;
    
    public final boolean snooperEnabled;
    
    public final boolean useNativeTransport;
    
    public final boolean enableCommandBlock;
    
    public final int spawnProtection;
    
    public final int opPermissionLevel;
    
    public final int functionPermissionLevel;
    
    public final long maxTickTime;
    
    public final int rateLimit;
    
    public final int viewDistance;
    
    public final int maxPlayers;
    
    public final int networkCompressionThreshold;
    
    public final boolean broadcastRconToOps;
    
    public final boolean broadcastConsoleToOps;
    
    public final int maxWorldSize;
    
    public final boolean syncChunkWrites;
    
    public final boolean enableJmxMonitoring;
    
    public final boolean enableStatus;
    
    public final int entityBroadcastRangePercentage;
    
    public final String textFilteringConfig;
    
    public final GeneratorSettings generatorSettings;

    private int playerIdleTimeout;

    private boolean whiteList;

    // CraftBukkit start
    public DedicatedServerProperties(String resource, IRegistryCustom iregistrycustom) throws IOException {
        InputStream is = getClass().getResourceAsStream(resource);
        super.load(is);
        
        this.difficulty = EnumDifficulty.valueOf(getString("difficulty")
                .toUpperCase(Locale.ENGLISH));
        this.gamemode = EnumGamemode.valueOf(getProperty("gamemode")
                .toUpperCase(Locale.ENGLISH));

        this.debug = this.getBoolean("debug"); // CraftBukkit
        this.onlineMode = this.getBoolean("online-mode");
        this.preventProxyConnections = this.getBoolean("prevent-proxy-connections");
        this.serverIp = this.getString("server-ip");
        this.spawnAnimals = this.getBoolean("spawn-animals");
        this.spawnNpcs = this.getBoolean("spawn-npcs");
        this.pvp = this.getBoolean("pvp");
        this.allowFlight = this.getBoolean("allow-flight");
        this.resourcePack = this.getString("resource-pack");
        this.motd = this.getString("motd");
        this.forceGamemode = this.getBoolean("force-gamemode");
        this.enforceWhitelist = this.getBoolean("enforce-whitelist");
        this.levelName = this.getString("level-name");
        this.serverPort = this.getInt("server-port");
        this.maxBuildHeight = this.getInt("max-build-height");
        this.announcePlayerAchievements = this.getBoolean("announce-player-achievements");
        this.enableQuery = this.getBoolean("enable-query");
        this.queryPort = this.getInt("query.port");
        this.enableRcon = this.getBoolean("enable-rcon");
        this.rconPort = this.getInt("rcon.port");
        this.rconPassword = this.getString("rcon.password");
        this.resourcePackHash = this.getString("resource-pack-hash");
        this.resourcePackSha1 = this.getString("resource-pack-sha1");
        this.hardcore = this.getBoolean("hardcore");
        this.allowNether = this.getBoolean("allow-nether");
        this.spawnMonsters = this.getBoolean("spawn-monsters");

        this.snooperEnabled = false;
        this.useNativeTransport = this.getBoolean("use-native-transport");
        this.enableCommandBlock = this.getBoolean("enable-command-block");
        this.spawnProtection = this.getInt("spawn-protection");
        this.opPermissionLevel = this.getInt("op-permission-level");
        this.functionPermissionLevel = this.getInt("function-permission-level");
        this.maxTickTime = this.getLong("max-tick-time");
        this.rateLimit = this.getInt("rate-limit");
        this.viewDistance = this.getInt("view-distance");
        this.maxPlayers = this.getInt("max-players");
        this.networkCompressionThreshold = this.getInt("network-compression-threshold");
        this.broadcastRconToOps = this.getBoolean("broadcast-rcon-to-ops");
        this.broadcastConsoleToOps = this.getBoolean("broadcast-console-to-ops");
        this.maxWorldSize = this.getInt("max-world-size");
        this.syncChunkWrites = this.getBoolean("sync-chunk-writes");
        this.enableJmxMonitoring = this.getBoolean("enable-jmx-monitoring");
        this.enableStatus = this.getBoolean("enable-status");
        this.entityBroadcastRangePercentage = this.getInt("entity-broadcast-range-percentage");

        this.textFilteringConfig = this.getString("text-filtering-config");

        this.playerIdleTimeout = this.getInt("player-idle-timeout");
        this.whiteList = this.getBoolean("white-list");
        this.generatorSettings = GeneratorSettings.a(iregistrycustom, this);
    }

    private String getString(String str) {
        return getProperty(str);
    }

    private boolean getBoolean(String str) {
        return Boolean.parseBoolean(getProperty(str));
    }

    public int getInt(String str) {
        return Integer.parseInt(getProperty(str));
    }

    public long getLong(String str) {
        return Long.parseLong(getProperty(str));
    }

    public boolean isWhiteList() {
        return whiteList;
    }

    public void setWhiteList(boolean whiteList) {
        this.whiteList = whiteList;
    }

    public int getPlayerIdleTimeout() {
        return playerIdleTimeout;
    }

    public void setPlayerIdleTimeout(int playerIdleTimeout) {
        this.playerIdleTimeout = playerIdleTimeout;
    }
}
