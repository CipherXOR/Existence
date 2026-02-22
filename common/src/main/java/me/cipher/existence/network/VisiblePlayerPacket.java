package me.cipher.existence.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class VisiblePlayerPacket {
    public final UUID playerUuid;
    public final boolean visible;
    public final int durationTicks;

    public VisiblePlayerPacket(UUID playerUuid, boolean visible, int durationTicks) {
        this.playerUuid = playerUuid;
        this.visible = visible;
        this.durationTicks = durationTicks;
    }

    public static void encode(VisiblePlayerPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.playerUuid);
        buf.writeBoolean(packet.visible);
        if (packet.visible) {
            buf.writeInt(packet.durationTicks);
        }
    }

    public static VisiblePlayerPacket decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        boolean visible = buf.readBoolean();
        int duration = visible ? buf.readInt() : 0;
        return new VisiblePlayerPacket(uuid, visible, duration);
    }
}