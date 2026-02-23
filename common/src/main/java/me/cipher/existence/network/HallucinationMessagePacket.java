package me.cipher.existence.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

public class HallucinationMessagePacket {
    public final Component message;
    public final int durationTicks;

    public HallucinationMessagePacket(Component message, int durationTicks) {
        this.message = message;
        this.durationTicks = durationTicks;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeComponent(message);
        buf.writeInt(durationTicks);
    }

    public static HallucinationMessagePacket decode(FriendlyByteBuf buf) {
        return new HallucinationMessagePacket(buf.readComponent(), buf.readInt());
    }
}