// mc-dev import
package net.minecraft.network.protocol.game;

import java.io.IOException;
import java.util.UUID;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.chat.ChatMessageType;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutChat implements Packet<PacketListenerPlayOut> {

    private IChatBaseComponent a;
    public net.md_5.bungee.api.chat.BaseComponent[] components; // Spigot
    private ChatMessageType b;
    private UUID c;

    public PacketPlayOutChat() {}

    public PacketPlayOutChat(IChatBaseComponent ichatbasecomponent, ChatMessageType chatmessagetype, UUID uuid) {
        this.a = ichatbasecomponent;
        this.b = chatmessagetype;
        this.c = uuid;
    }

    @Override
    public void a(PacketDataSerializer packetdataserializer) throws IOException {
        this.a = packetdataserializer.h();
        this.b = ChatMessageType.a(packetdataserializer.readByte());
        this.c = packetdataserializer.k();
    }

    @Override
    public void b(PacketDataSerializer packetdataserializer) throws IOException {
        // Spigot start
        if (components != null) {
            packetdataserializer.a(net.md_5.bungee.chat.ComponentSerializer.toString(components));
        } else {
            packetdataserializer.a(this.a);
        }
        // Spigot end
        packetdataserializer.writeByte(this.b.a());
        packetdataserializer.a(this.c);
    }

    public void a(PacketListenerPlayOut packetlistenerplayout) {
        packetlistenerplayout.a(this);
    }

    public boolean c() {
        return this.b == ChatMessageType.SYSTEM || this.b == ChatMessageType.GAME_INFO;
    }

    public ChatMessageType d() {
        return this.b;
    }

    @Override
    public boolean a() {
        return true;
    }
}
