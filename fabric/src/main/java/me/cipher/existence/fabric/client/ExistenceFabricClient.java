package me.cipher.existence.fabric.client;

import me.cipher.existence.client.ClientStressManager;
import me.cipher.existence.client.render.GhostEntityRenderer;
import me.cipher.existence.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.ChatFormatting;
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
        // ClientGuiEvent.RENDER_HUD.register((guiGraphics, partialTick) -> StressHUD.render(guiGraphics));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null || client.player == null) return;
            int load = ClientStressManager.getLoad();

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
    }
}