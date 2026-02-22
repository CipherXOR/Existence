package me.cipher.existence.network;

import dev.architectury.networking.NetworkManager;
import me.cipher.existence.Existence;
import me.cipher.existence.client.ClientStressManager;
import me.cipher.existence.client.VisiblePlayerManager;
import net.minecraft.resources.ResourceLocation;

public class ExistenceNetwork {
    public static final ResourceLocation VISIBLE_PLAYER_ID = new ResourceLocation(Existence.MOD_ID, "visible_player");
    public static final ResourceLocation STRESS_SYNC_ID = new ResourceLocation(Existence.MOD_ID, "stress_sync");

    public static void init() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, VISIBLE_PLAYER_ID, (buf, context) -> {
            VisiblePlayerPacket packet = VisiblePlayerPacket.decode(buf);
            context.queue(() -> {
                if (packet.visible) {
                    VisiblePlayerManager.setVisible(packet.playerUuid, packet.durationTicks);
                } else {
                    VisiblePlayerManager.setInvisible(packet.playerUuid);
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, STRESS_SYNC_ID, (buf, context) -> {
            StressSyncPacket packet = StressSyncPacket.decode(buf);
            context.queue(() -> ClientStressManager.setLoad(packet.load));
        });
    }
}