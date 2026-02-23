package me.cipher.existence.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public class HallucinationDoorPacket {
    public final BlockPos pos;
    public final int durationTicks;

    public HallucinationDoorPacket(BlockPos pos, int durationTicks) {
        this.pos = pos;
        this.durationTicks = durationTicks;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(durationTicks);
    }

    public static HallucinationDoorPacket decode(FriendlyByteBuf buf) {
        return new HallucinationDoorPacket(buf.readBlockPos(), buf.readInt());
    }
}