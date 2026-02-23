package me.cipher.existence.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ClientHallucinationManager {
    private static Component currentMessage = null;
    private static long expireTick = 0;

    public static void showMessage(Component message, int durationTicks) {
        if (Minecraft.getInstance().level == null) return;
        expireTick = Minecraft.getInstance().level.getGameTime() + durationTicks;
        currentMessage = message;
    }

    public static void tick() {
        if (currentMessage != null && Minecraft.getInstance().level != null) {
            if (Minecraft.getInstance().level.getGameTime() >= expireTick) {
                currentMessage = null;
            }
        }
    }

    public static Component getCurrentMessage() {
        return currentMessage;
    }
}