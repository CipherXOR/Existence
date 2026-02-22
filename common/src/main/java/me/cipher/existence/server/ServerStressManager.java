package me.cipher.existence.server;

import io.netty.buffer.Unpooled;
import me.cipher.existence.entity.GhostEntity;
import me.cipher.existence.network.ExistenceNetwork;
import me.cipher.existence.network.StressSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ServerStressManager {
    private static final Map<UUID, Double> LOAD_MAP = new HashMap<>();
    private static final Map<UUID, BlockPos> LAST_GHOST_POS = new HashMap<>();
    private static final Map<UUID, Boolean> SAFE_ZONE_CACHE = new HashMap<>();
    private static final Map<UUID, Integer> SAFE_ZONE_COOLDOWN = new HashMap<>();
    private static int syncCounter = 0;
    private static final double RATE = 0.01;
    private static final int SYNC_INTERVAL = 20;
    private static final double HUNGER_RATE = 0.01;
    private static final double RAIN_RATE = 0.01;
    private static final double SWIM_RATE = 0.01;
    private static final double ATTACK_PENALTY = 1.0;
    private static final double BAD_FOOD_PENALTY = 2.0;
    private static final double HURT_PENALTY = 1.0;
    private static final double SLEEP_REWARD = -30.0;
    private static final double HEALTHY_FOOD_REWARD = -1.0;
    private static final double ADVANCEMENT_REWARD = -5.0;

    public static void tick(MinecraftServer server) {
        syncCounter++;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            double load = getLoadDouble(uuid);

            if (player.level().getMaxLocalRawBrightness(player.blockPosition()) < 7) {
                load += RATE;
            } else {
                load -= RATE;
            }

            if (isAlone(player, server)) {
                load += RATE;
            }

            List<GhostEntity> ghosts = player.level().getEntitiesOfClass(GhostEntity.class, player.getBoundingBox().inflate(16));
            if (!ghosts.isEmpty()) {
                load += 5 * RATE;
                GhostEntity nearest = ghosts.get(0);
                LAST_GHOST_POS.put(uuid, nearest.blockPosition());
            }

            int cooldown = SAFE_ZONE_COOLDOWN.getOrDefault(uuid, 0);
            boolean inSafe;
            if (cooldown <= 0) {
                inSafe = isInSafeZone(player);
                SAFE_ZONE_CACHE.put(uuid, inSafe);
                SAFE_ZONE_COOLDOWN.put(uuid, 20);
            } else {
                inSafe = SAFE_ZONE_CACHE.getOrDefault(uuid, false);
                SAFE_ZONE_COOLDOWN.put(uuid, cooldown - 1);
            }
            if (inSafe) {
                load -= 3 * RATE;
            }

            BlockPos lastPos = LAST_GHOST_POS.get(uuid);
            if (lastPos != null && player.distanceToSqr(lastPos.getX(), lastPos.getY(), lastPos.getZ()) < 25) {
                load += 2 * RATE;
            }

            if (isHungry(player)) {
                load += HUNGER_RATE;
            }

            if (isRainingOn(player)) {
                load += RAIN_RATE;
            }

            if (player.isSwimming() || player.isInWater()) {
                load += SWIM_RATE;
            }

            load = Math.min(100, Math.max(0, load));
            setLoadDouble(uuid, load);

            if (syncCounter % SYNC_INTERVAL == 0) {
                syncLoad(player);
            }
        }
    }

    private static boolean isAlone(ServerPlayer player, MinecraftServer server) {
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == player) continue;
            if (ServerVisibleManager.isVisible(player.getUUID(), other.getUUID()) && player.distanceToSqr(other) < 1024) {
                return false;
            }
        }
        return true;
    }

    private static boolean isInSafeZone(ServerPlayer player) {
        BlockPos pos = player.blockPosition();
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    BlockState state = player.level().getBlockState(pos.offset(dx, dy, dz));
                    if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH) ||
                            state.is(Blocks.LANTERN) || state.is(Blocks.SOUL_LANTERN) ||
                            state.is(Blocks.CAMPFIRE) || state.is(BlockTags.BEDS)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isHungry(ServerPlayer player) {
        FoodData food = player.getFoodData();
        return food.getFoodLevel() < 6;
    }

    private static boolean isRainingOn(ServerPlayer player) {
        Level level = player.level();
        if (!level.isRaining()) return false;
        BlockPos pos = player.blockPosition();
        return level.canSeeSky(pos) && !level.isRainingAt(pos);
    }

    public static void onAttackEntity(ServerPlayer attacker, LivingEntity target) {
        if (target instanceof Monster) {
            addLoad(attacker.getUUID(), -ATTACK_PENALTY);
        }
        else if (target instanceof Animal || target instanceof Villager || target instanceof Player) {
            addLoad(attacker.getUUID(), ATTACK_PENALTY);
        }
        else {
            addLoad(attacker.getUUID(), ATTACK_PENALTY);
        }
    }

    public static void onEatBadFood(ServerPlayer player) {
        addLoad(player.getUUID(), BAD_FOOD_PENALTY);
    }

    public static void onHurt(ServerPlayer player) {
        addLoad(player.getUUID(), HURT_PENALTY);
    }

    public static void onSleep(ServerPlayer player) {
        addLoad(player.getUUID(), SLEEP_REWARD);
    }

    public static void onEatHealthyFood(ServerPlayer player) {
        addLoad(player.getUUID(), HEALTHY_FOOD_REWARD);
    }

    public static void onAdvancement(ServerPlayer player) {
        addLoad(player.getUUID(), ADVANCEMENT_REWARD);
    }

    public static void addLoad(UUID uuid, double delta) {
        double load = getLoadDouble(uuid) + delta;
        setLoadDouble(uuid, Math.min(100, Math.max(0, load)));
    }

    public static void addAcuteEvent(UUID player) {
        double load = getLoadDouble(player) + 20.0;
        setLoadDouble(player, Math.min(100, Math.max(0, load)));
    }

    public static void addKillEvent(UUID player) {
        double load = getLoadDouble(player) - 20.0;
        setLoadDouble(player, Math.min(100, Math.max(0, load)));
    }

    public static void clearLastGhostPos(UUID player) {
        LAST_GHOST_POS.remove(player);
    }

    private static void syncLoad(ServerPlayer player) {
        int load = getLoad(player.getUUID());
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        new StressSyncPacket(load).encode(buf);
        dev.architectury.networking.NetworkManager.sendToPlayer(player, ExistenceNetwork.STRESS_SYNC_ID, buf);
    }

    public static int getLoad(UUID player) {
        return (int) Math.floor(getLoadDouble(player));
    }

    private static double getLoadDouble(UUID player) {
        return LOAD_MAP.getOrDefault(player, 0.0);
    }

    private static void setLoadDouble(UUID player, double value) {
        LOAD_MAP.put(player, value);
    }
}