package net.minecraft.server.dedicated;

import java.util.function.UnaryOperator;
import net.minecraft.core.IRegistryCustom;

// CraftBukkit start
import java.io.File;
import joptsimple.OptionSet;
// CraftBukkit end

public class DedicatedServerSettings {

    private DedicatedServerProperties properties;

    // CraftBukkit start
    public DedicatedServerSettings(IRegistryCustom iregistrycustom, OptionSet optionset) {
        this.properties = DedicatedServerProperties.load(iregistrycustom, getClass().getClassLoader()
                .getResourceAsStream("server.properties"), optionset);
        // CraftBukkit end
    }

    public DedicatedServerProperties getProperties() {
        return this.properties;
    }

    public DedicatedServerSettings setProperty(UnaryOperator<DedicatedServerProperties> unaryoperator) {
        this.properties = unaryoperator.apply(this.properties);
        return this;
    }
}
