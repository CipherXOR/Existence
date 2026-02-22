package me.cipher.existence.client;

import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VisiblePlayerManager {
    private static final Map<UUID, Long> VISIBLE_UNTIL = new HashMap<>();

    public static void setVisible(UUID playerUuid, int durationTicks) {
        if (Minecraft.getInstance().level == null) return;
        long currentTime = Minecraft.getInstance().level.getGameTime();
        long expireAt = durationTicks > 0 ? currentTime + durationTicks : Long.MAX_VALUE;
        VISIBLE_UNTIL.put(playerUuid, expireAt);
    }

    public static void setInvisible(UUID playerUuid) {
        VISIBLE_UNTIL.remove(playerUuid);
    }

    public static boolean isVisible(UUID playerUuid) {
        Long expire = VISIBLE_UNTIL.get(playerUuid);
        if (expire == null) return false;
        if (Minecraft.getInstance().level == null) return false;
        long currentTime = Minecraft.getInstance().level.getGameTime();
        if (expire <= currentTime) {
            VISIBLE_UNTIL.remove(playerUuid);
            return false;
        }
        return true;
    }

    public static void clear() {
        VISIBLE_UNTIL.clear();
    }
}