package me.cipher.existence.network;

import dev.architectury.networking.NetworkManager;
import me.cipher.existence.Existence;
import me.cipher.existence.client.ClientHallucinationDoorManager;
import me.cipher.existence.client.ClientHallucinationManager;
import me.cipher.existence.client.ClientStressManager;
import me.cipher.existence.client.VisiblePlayerManager;
import me.cipher.existence.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public class ExistenceNetwork {
    public static final ResourceLocation VISIBLE_PLAYER_ID = new ResourceLocation(Existence.MOD_ID, "visible_player");
    public static final ResourceLocation STRESS_SYNC_ID = new ResourceLocation(Existence.MOD_ID, "stress_sync");
    public static final ResourceLocation HALLUCINATION_MESSAGE_ID = new ResourceLocation(Existence.MOD_ID, "hallucination_message");
    public static final ResourceLocation HALLUCINATION_DOOR_ID = new ResourceLocation(Existence.MOD_ID, "hallucination_door");
    public static final ResourceLocation HALLUCINATION_SOUND_ID = new ResourceLocation(Existence.MOD_ID, "hallucination_sound");

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

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, HALLUCINATION_MESSAGE_ID, (buf, context) -> {
            HallucinationMessagePacket packet = HallucinationMessagePacket.decode(buf);
            context.queue(() -> ClientHallucinationManager.showMessage(packet.message, packet.durationTicks));
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, HALLUCINATION_DOOR_ID, (buf, context) -> {
            HallucinationDoorPacket packet = HallucinationDoorPacket.decode(buf);
            context.queue(() -> ClientHallucinationDoorManager.addDoor(packet.pos, packet.durationTicks));
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, HALLUCINATION_SOUND_ID, (buf, context) -> {
            HallucinationSoundPacket.decode(buf);
            context.queue(() -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.playSound(ModSounds.RECORDER_HORROR2.get(), 1.0F, 1.0F);
                }
            });
        });
    }
}