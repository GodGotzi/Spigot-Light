package org.bukkit.craftbukkit;

import com.mojang.authlib.GameProfile;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.players.WhiteListEntry;
import net.minecraft.stats.ServerStatisticManager;
import net.minecraft.world.level.storage.WorldNBTStorage;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.Statistic;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

@SerializableAs("Player")
public class CraftOfflinePlayer implements OfflinePlayer, ConfigurationSerializable {
    private final GameProfile profile;
    private final CraftServer server;

    protected CraftOfflinePlayer(CraftServer server, GameProfile profile) {
        this.server = server;
        this.profile = profile;
    }

    public GameProfile getProfile() {
        return profile;
    }

    @Override
    public boolean isOnline() {
        return getPlayer() != null;
    }

    @Deprecated
    @Override
    public String getName() {
        return "";
    }

    @Override
    public UUID getUniqueId() {
        return profile.getId();
    }

    public Server getServer() {
        return server;
    }

    @Override
    public boolean isOp() {
        return server.getHandle().isOp(profile);
    }

    @Override
    public void setOp(boolean value) {
        if (value == isOp()) {
            return;
        }

        if (value) {
            server.getHandle().addOp(profile);
        } else {
            server.getHandle().removeOp(profile);
        }
    }

    @Override
    public boolean isBanned() {
        if (getName() == null) {
            return false;
        }

        return server.getBanList(BanList.Type.NAME).isBanned(getName());
    }

    public void setBanned(boolean value) {
        if (getName() == null) {
            return;
        }

        if (value) {
            server.getBanList(BanList.Type.NAME).addBan(getName(), null, null, null);
        } else {
            server.getBanList(BanList.Type.NAME).pardon(getName());
        }
    }

    @Override
    public boolean isWhitelisted() {
        return server.getHandle().getWhitelist().isWhitelisted(profile);
    }

    @Override
    public void setWhitelisted(boolean value) {
        if (value) {
            server.getHandle().getWhitelist().add(new WhiteListEntry(profile));
        } else {
            server.getHandle().getWhitelist().remove(profile);
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();

        result.put("UUID", profile.getId().toString());

        return result;
    }

    public static OfflinePlayer deserialize(Map<String, Object> args) {
        // Backwards comparability
        if (args.get("name") != null) {
            return Bukkit.getServer().getOfflinePlayer((String) args.get("name"));
        }

        return Bukkit.getServer().getOfflinePlayer(UUID.fromString((String) args.get("UUID")));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[UUID=" + profile.getId() + "]";
    }

    @Override
    public Player getPlayer() {
        return server.getPlayer(getUniqueId());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof OfflinePlayer)) {
            return false;
        }

        OfflinePlayer other = (OfflinePlayer) obj;
        if ((this.getUniqueId() == null) || (other.getUniqueId() == null)) {
            return false;
        }

        return this.getUniqueId().equals(other.getUniqueId());
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (this.getUniqueId() != null ? this.getUniqueId().hashCode() : 0);
        return hash;
    }

    @Override
    public long getFirstPlayed() {
        return -1;
    }

    @Deprecated
    @Override
    public long getLastPlayed() {
        return -1;
    }

    @Deprecated
    @Override
    public boolean hasPlayedBefore() {
        return false;
    }

    @Deprecated
    @Override
    public Location getBedSpawnLocation() {
        return null;
    }

    public void setMetadata(String metadataKey, MetadataValue metadataValue) {
        server.getPlayerMetadata().setMetadata(this, metadataKey, metadataValue);
    }

    public List<MetadataValue> getMetadata(String metadataKey) {
        return server.getPlayerMetadata().getMetadata(this, metadataKey);
    }

    public boolean hasMetadata(String metadataKey) {
        return server.getPlayerMetadata().hasMetadata(this, metadataKey);
    }

    public void removeMetadata(String metadataKey, Plugin plugin) {
        server.getPlayerMetadata().removeMetadata(this, metadataKey, plugin);
    }

    private ServerStatisticManager getStatisticManager() {
        return server.getHandle().getStatisticManager(getUniqueId(), getName());
    }

    @Override
    public void incrementStatistic(Statistic statistic) {
        if (isOnline()) {
            getPlayer().incrementStatistic(statistic);
        } else {
            ServerStatisticManager manager = getStatisticManager();
            CraftStatistic.incrementStatistic(manager, statistic);
            manager.save();
        }
    }

    @Override
    public void decrementStatistic(Statistic statistic) {
        if (isOnline()) {
            getPlayer().decrementStatistic(statistic);
        } else {
            ServerStatisticManager manager = getStatisticManager();
            CraftStatistic.decrementStatistic(manager, statistic);
            manager.save();
        }
    }

    @Override
    public int getStatistic(Statistic statistic) {
        if (isOnline()) {
            return getPlayer().getStatistic(statistic);
        } else {
            return CraftStatistic.getStatistic(getStatisticManager(), statistic);
        }
    }

    @Override
    public void incrementStatistic(Statistic statistic, int amount) {
        if (isOnline()) {
            getPlayer().incrementStatistic(statistic, amount);
        } else {
            ServerStatisticManager manager = getStatisticManager();
            CraftStatistic.incrementStatistic(manager, statistic, amount);
            manager.save();
        }
    }

    @Override
    public void decrementStatistic(Statistic statistic, int amount) {
        if (isOnline()) {
            getPlayer().decrementStatistic(statistic, amount);
        } else {
            ServerStatisticManager manager = getStatisticManager();
            CraftStatistic.decrementStatistic(manager, statistic, amount);
            manager.save();
        }
    }

    @Override
    public void setStatistic(Statistic statistic, int newValue) {
        if (isOnline()) {
            getPlayer().setStatistic(statistic, newValue);
        } else {
            ServerStatisticManager manager = getStatisticManager();
            CraftStatistic.setStatistic(manager, statistic, newValue);
            manager.save();
        }
    }

    @Override
    public void incrementStatistic(Statistic statistic, Material material) {
        if (isOnline()) {
            getPlayer().incrementStatistic(statistic, material);
        } else {
            ServerStatisticManager manager = getStatisticManager();
            CraftStatistic.incrementStatistic(manager, statistic, material);
            manager.save();
        }
    }

    @Override
    public void decrementStatistic(Statistic statistic, Material material) {
        if (isOnline()) {
            getPlayer().decrementStatistic(statistic, material);
        } else {
            ServerStatisticManager manager = getStatisticManager();
            CraftStatistic.decrementStatistic(manager, statistic, material);
            manager.save();
        }
    }

    @Override
    public int getStatistic(Statistic statistic, Material material) {
        if (isOnline()) {
            return getPlayer().getStatistic(statistic, material);
        } else {
            return CraftStatistic.getStatistic(getStatisticManager(), statistic, material);
        }
    }

    @Override
    public void incrementStatistic(Statistic statistic, Material material, int amount) {
        if (isOnline()) {
            getPlayer().incrementStatistic(statistic, material, amount);
        } else {
            ServerStatisticManager manager = getStatisticManager();
            CraftStatistic.incrementStatistic(manager, statistic, material, amount);
            manager.save();
        }
    }

    @Override
    public void decrementStatistic(Statistic statistic, Material material, int amount) {
        if (isOnline()) {
            getPlayer().decrementStatistic(statistic, material, amount);
        } else {
            ServerStatisticManager manager = getStatisticManager();
            CraftStatistic.decrementStatistic(manager, statistic, material, amount);
            manager.save();
        }
    }

    @Override
    public void setStatistic(Statistic statistic, Material material, int newValue) {
        if (isOnline()) {
            getPlayer().setStatistic(statistic, material, newValue);
        } else {
            ServerStatisticManager manager = getStatisticManager();
            CraftStatistic.setStatistic(manager, statistic, material, newValue);
            manager.save();
        }
    }

    @Override
    public void incrementStatistic(Statistic statistic, EntityType entityType) {
        if (isOnline()) {
            getPlayer().incrementStatistic(statistic, entityType);
        } else {
            ServerStatisticManager manager = getStatisticManager();
            CraftStatistic.incrementStatistic(manager, statistic, entityType);
            manager.save();
        }
    }

    @Override
    public void decrementStatistic(Statistic statistic, EntityType entityType) {
        if (isOnline()) {
            getPlayer().decrementStatistic(statistic, entityType);
        } else {
            ServerStatisticManager manager = getStatisticManager();
            CraftStatistic.decrementStatistic(manager, statistic, entityType);
            manager.save();
        }
    }

    @Override
    public int getStatistic(Statistic statistic, EntityType entityType) {
        if (isOnline()) {
            return getPlayer().getStatistic(statistic, entityType);
        } else {
            return CraftStatistic.getStatistic(getStatisticManager(), statistic, entityType);
        }
    }

    @Override
    public void incrementStatistic(Statistic statistic, EntityType entityType, int amount) {
        if (isOnline()) {
            getPlayer().incrementStatistic(statistic, entityType, amount);
        } else {
            ServerStatisticManager manager = getStatisticManager();
            CraftStatistic.incrementStatistic(manager, statistic, entityType, amount);
            manager.save();
        }
    }

    @Override
    public void decrementStatistic(Statistic statistic, EntityType entityType, int amount) {
        if (isOnline()) {
            getPlayer().decrementStatistic(statistic, entityType, amount);
        } else {
            ServerStatisticManager manager = getStatisticManager();
            CraftStatistic.decrementStatistic(manager, statistic, entityType, amount);
            manager.save();
        }
    }

    @Override
    public void setStatistic(Statistic statistic, EntityType entityType, int newValue) {
        if (isOnline()) {
            getPlayer().setStatistic(statistic, entityType, newValue);
        } else {
            ServerStatisticManager manager = getStatisticManager();
            CraftStatistic.setStatistic(manager, statistic, entityType, newValue);
            manager.save();
        }
    }
}
