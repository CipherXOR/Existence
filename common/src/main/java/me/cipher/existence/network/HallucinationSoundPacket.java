package me.cipher.existence.network;

import net.minecraft.network.FriendlyByteBuf;

public class HallucinationSoundPacket {
    public HallucinationSoundPacket() {}

    public void encode(FriendlyByteBuf buf) {}

    public static HallucinationSoundPacket decode(FriendlyByteBuf buf) {
        return new HallucinationSoundPacket();
    }
}