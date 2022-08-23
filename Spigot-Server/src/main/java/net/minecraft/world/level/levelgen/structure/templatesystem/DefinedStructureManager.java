package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.FileUtils;
import net.minecraft.ResourceKeyInvalidException;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTCompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.packs.resources.IResource;
import net.minecraft.server.packs.resources.IResourceManager;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.storage.Convertable;
import net.minecraft.world.level.storage.SavedFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DefinedStructureManager {

    private static final Logger LOGGER = LogManager.getLogger();
    private final Map<MinecraftKey, DefinedStructure> b = Maps.newConcurrentMap(); // SPIGOT-5287
    private final DataFixer c;
    private IResourceManager d;

    public DefinedStructureManager(IResourceManager iresourcemanager, DataFixer datafixer) {
        this.d = iresourcemanager;
        this.c = datafixer;
    }

    public DefinedStructure a(MinecraftKey minecraftkey) {
        DefinedStructure definedstructure = this.b(minecraftkey);

        if (definedstructure == null) {
            definedstructure = new DefinedStructure();
            this.b.put(minecraftkey, definedstructure);
        }

        return definedstructure;
    }

    @Nullable
    public DefinedStructure b(MinecraftKey minecraftkey) {
        return (DefinedStructure) this.b.computeIfAbsent(minecraftkey, (minecraftkey1) -> {
            DefinedStructure definedstructure = this.f(minecraftkey1);

            return definedstructure != null ? definedstructure : this.e(minecraftkey1);
        });
    }

    public void a(IResourceManager iresourcemanager) {
        this.d = iresourcemanager;
        this.b.clear();
    }

    @Nullable
    private DefinedStructure e(MinecraftKey minecraftkey) {
        MinecraftKey minecraftkey1 = new MinecraftKey(minecraftkey.getNamespace(), "structures/" + minecraftkey.getKey() + ".nbt");

        try {
            IResource iresource = this.d.a(minecraftkey1);
            Throwable throwable = null;

            DefinedStructure definedstructure;

            try {
                definedstructure = this.a(iresource.b());
            } catch (Throwable throwable1) {
                throwable = throwable1;
                throw throwable1;
            } finally {
                if (iresource != null) {
                    if (throwable != null) {
                        try {
                            iresource.close();
                        } catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    } else {
                        iresource.close();
                    }
                }

            }

            return definedstructure;
        } catch (FileNotFoundException filenotfoundexception) {
            return null;
        } catch (Throwable throwable3) {
            DefinedStructureManager.LOGGER.error("Couldn't load structure {}: {}", minecraftkey, throwable3.toString());
            return null;
        }
    }

    @Nullable
    private DefinedStructure f(MinecraftKey minecraftkey) {
        return null;
    }

    private DefinedStructure a(InputStream inputstream) throws IOException {
        NBTTagCompound nbttagcompound = NBTCompressedStreamTools.a(inputstream);

        return this.a(nbttagcompound);
    }

    public DefinedStructure a(NBTTagCompound nbttagcompound) {
        if (!nbttagcompound.hasKeyOfType("DataVersion", 99)) {
            nbttagcompound.setInt("DataVersion", 500);
        }

        DefinedStructure definedstructure = new DefinedStructure();

        definedstructure.b(GameProfileSerializer.a(this.c, DataFixTypes.STRUCTURE, nbttagcompound, nbttagcompound.getInt("DataVersion")));
        return definedstructure;
    }

    public void d(MinecraftKey minecraftkey) {
        this.b.remove(minecraftkey);
    }
}
