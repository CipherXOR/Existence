package me.cipher.existence.network;

import net.minecraft.network.FriendlyByteBuf;

public class StressSyncPacket {
    public final int load;

    public StressSyncPacket(int load) {
        this.load = load;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(load);
    }

    public static StressSyncPacket decode(FriendlyByteBuf buf) {
        return new StressSyncPacket(buf.readInt());
    }
}