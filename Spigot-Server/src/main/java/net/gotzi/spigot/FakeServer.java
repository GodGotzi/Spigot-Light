package net.gotzi.spigot;

import joptsimple.OptionSet;
import net.gotzi.minestrum.api.server.logging.ServerLogManagerInterface;
import net.gotzi.spigot.data.ServerData;
import net.gotzi.spigot.logging.ServerLogManager;
import org.bukkit.craftbukkit.Main;
import org.fusesource.jansi.AnsiConsole;

import java.io.File;

public class FakeServer {

    private ServerData data;

    private final ServerLogManagerInterface logManager;

    public FakeServer(ServerData data) {
        this.data = data;
        this.logManager = new ServerLogManager();

        //TODO compute data
    }

    public void run() {
        //TODO run and handle with data

        OptionSet options = null;
        // Do you love Java using + and ! as string based identifiers? I sure do!
        String path = new File(".").getAbsolutePath();
        if (path.contains("!") || path.contains("+")) {
            System.err.println("Cannot run server in a directory with ! or + in the pathname. Please rename the affected folders and try again.");
            return;
        }

        float javaVersion = Float.parseFloat(System.getProperty("java.class.version"));
        if (javaVersion > 60.0) {
            System.err.println("Unsupported Java detected (" + javaVersion + "). Only up to Java 16 is supported.");
            return;
        }

        try {
            if (options.has("nojline")) {
                System.setProperty("user.language", "en");
                Main.useJline = false;
            }

            if (Main.useJline) {
                AnsiConsole.systemInstall();
            } else {
                // This ensures the terminal literal will always match the jline implementation
                System.setProperty(jline.TerminalFactory.JLINE_TERMINAL, jline.UnsupportedTerminal.class.getName());
            }

            if (options.has("noconsole")) {
                Main.useConsole = false;
            }

            System.out.println("Loading libraries, please wait...");

            net.minecraft.server.Main.main(options);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void stop() {

    }

    public ServerLogManagerInterface getLogManager() {
        return logManager;
    }

    public ServerData getData() {
        return data;
    }
}