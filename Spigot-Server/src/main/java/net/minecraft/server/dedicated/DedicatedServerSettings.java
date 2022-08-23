package net.minecraft.server.dedicated;

import java.io.IOException;
import net.minecraft.core.IRegistryCustom;

// CraftBukkit start
import java.io.File;
import joptsimple.OptionSet;
// CraftBukkit end

public class DedicatedServerSettings {
    private final DedicatedServerProperties properties;

    // CraftBukkit start
    public DedicatedServerSettings(IRegistryCustom iregistrycustom) {
        try {
            this.properties = new DedicatedServerProperties("server.properties", iregistrycustom);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // CraftBukkit end
    }

    public DedicatedServerProperties getProperties() {
        return this.properties;
    }
}
