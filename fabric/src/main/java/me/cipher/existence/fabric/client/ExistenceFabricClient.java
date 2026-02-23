package me.cipher.existence.fabric.client;

import dev.architectury.event.events.client.ClientGuiEvent;
import me.cipher.existence.client.ClientHallucinationDoorManager;
import me.cipher.existence.client.ClientHallucinationManager;
import me.cipher.existence.client.ClientStressManager;
import me.cipher.existence.client.render.GhostEntityRenderer;
import me.cipher.existence.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;

import java.util.Random;

public final class ExistenceFabricClient implements ClientModInitializer {
    private static final Random RANDOM = new Random();
    private static int messageCooldown = 0;
    private static int hallucinationCooldown = 0;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.GHOST.get(), GhostEntityRenderer::new);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null || client.player == null) return;
            int load = ClientStressManager.getLoad();
            ClientHallucinationManager.tick();
            ClientHallucinationDoorManager.tick();
            if (load > 50 && RANDOM.nextInt(100) == 0) {
                client.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                        client.player.getX() + (RANDOM.nextDouble() - 0.5) * 20,
                        client.player.getY() + RANDOM.nextDouble() * 5,
                        client.player.getZ() + (RANDOM.nextDouble() - 0.5) * 20,
                        0, 0.1, 0);
            }

            if (load > 80) {
                if (messageCooldown <= 0 && RANDOM.nextInt(300) == 0) {
                    client.player.displayClientMessage(
                            Component.translatable("message.existence.see_it")
                                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC),
                            false
                    );
                    messageCooldown = 2400;
                }
            }
            if (messageCooldown > 0) messageCooldown--;
            if (hallucinationCooldown > 0) {
                hallucinationCooldown--;
            }
        });
        ClientGuiEvent.RENDER_HUD.register((guiGraphics, partialTick) -> {
            Component msg = ClientHallucinationManager.getCurrentMessage();
            if (msg != null) {
                Minecraft mc = Minecraft.getInstance();
                int screenWidth = mc.getWindow().getGuiScaledWidth();
                int screenHeight = mc.getWindow().getGuiScaledHeight();
                float textWidth = mc.font.width(msg);
                float targetWidth = screenWidth * 0.8f;
                float scale = targetWidth / textWidth;
                scale = Math.min(scale, 10.0f);
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(screenWidth / 2.0, screenHeight / 2.0, 0);
                guiGraphics.pose().scale(scale, scale, 1.0f);
                int textWidthPx = mc.font.width(msg);
                int textHeightPx = mc.font.lineHeight;
                guiGraphics.drawString(mc.font, msg,
                        -textWidthPx / 2,
                        -textHeightPx / 2,
                        0xFFFFFFFF,
                        true);
                guiGraphics.pose().popPose();
            }
        });
    }
}