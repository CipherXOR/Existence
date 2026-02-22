package me.cipher.existence.server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServerVisibleManager {
    private static final Map<UUID, Map<UUID, Long>> VISIBLE_MAP = new HashMap<>();
    private static long currentTick = 0;

    public static void tick(MinecraftServer server) {
        currentTick = server.getTickCount();
        VISIBLE_MAP.values().forEach(map -> map.entrySet().removeIf(entry -> entry.getValue() <= currentTick));
        VISIBLE_MAP.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public static void setVisible(ServerPlayer viewer, ServerPlayer target, int durationTicks) {
        long expireAt = currentTick + durationTicks;
        VISIBLE_MAP.computeIfAbsent(viewer.getUUID(), k -> new HashMap<>()).put(target.getUUID(), expireAt);
    }

    public static boolean isVisible(UUID viewer, UUID target) {
        if (viewer.equals(target)) return true;
        Map<UUID, Long> map = VISIBLE_MAP.get(viewer);
        if (map == null) return false;
        Long expire = map.get(target);
        return expire != null && expire > currentTick;
    }

    public static long getExpireTick(UUID viewer, UUID target) {
        Map<UUID, Long> map = VISIBLE_MAP.get(viewer);
        if (map == null) return -1;
        return map.getOrDefault(target, -1L);
    }
}