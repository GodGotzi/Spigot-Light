package net.minecraft.world.level.storage;

import com.mojang.datafixers.DataFixer;
import java.io.File;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTCompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.EntityHuman;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// CraftBukkit start
import java.io.FileInputStream;
import java.io.InputStream;
import net.minecraft.server.level.EntityPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
// CraftBukkit end

public class WorldNBTStorage {

    private static final Logger LOGGER = LogManager.getLogger();
    protected final DataFixer a;

    public WorldNBTStorage(DataFixer datafixer) {
        this.a = datafixer;
    }

    @Deprecated
    public void save(EntityHuman entityhuman) {
        /*
        if (org.spigotmc.SpigotConfig.disablePlayerDataSaving) return; // Spigot
        try {
            NBTTagCompound nbttagcompound = entityhuman.save(new NBTTagCompound());
            File file = File.createTempFile(entityhuman.getUniqueIDString() + "-", ".dat", this.playerDir);

            NBTCompressedStreamTools.a(nbttagcompound, file);
            File file1 = new File(this.playerDir, entityhuman.getUniqueIDString() + ".dat");
            File file2 = new File(this.playerDir, entityhuman.getUniqueIDString() + ".dat_old");

            SystemUtils.a(file1, file, file2);
        } catch (Exception exception) {
            WorldNBTStorage.LOGGER.warn("Failed to save player data for {}", entityhuman.getDisplayName().getString());
        }
        */
    }

}
