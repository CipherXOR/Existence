package me.cipher.existence;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.utils.Env;
import io.netty.buffer.Unpooled;
import me.cipher.existence.client.ClientVisibilityChecker;
import me.cipher.existence.entity.GhostEntity;
import me.cipher.existence.entity.ModEntities;
import me.cipher.existence.item.ModItems;
import me.cipher.existence.network.ExistenceNetwork;
import me.cipher.existence.network.VisiblePlayerPacket;
import me.cipher.existence.server.DiaryProgressData;
import me.cipher.existence.server.ServerStressManager;
import me.cipher.existence.server.ServerVisibilityChecker;
import me.cipher.existence.server.ServerVisibleManager;
import me.cipher.existence.util.HiddenFromAware;
import me.cipher.existence.util.OwnerAwareItemEntity;
import me.cipher.existence.util.VisibilityChecker;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class Existence {
    public static final String MOD_ID = "existence";
    private static final Random RANDOM = new Random();
    private static long nextGhostSpawnTime = 0;
    private static final int MIN_GHOST_INTERVAL = 6000;
    private static final int MAX_GHOST_INTERVAL = 9600;
    private static final int GHOST_RETRY_INTERVAL = 600;
    private static long nextDiarySpawnTime = 0;
    private static final int DIARY_SPAWN_INTERVAL = 6000;
    public static VisibilityChecker VISIBILITY;

    public static void register() {
        if (Platform.getEnvironment() == Env.SERVER) {
            VISIBILITY = new ServerVisibilityChecker();
        } else {
            VISIBILITY = new ClientVisibilityChecker();
        }
        ExistenceNetwork.init();
    }

    public static void setupListeners() {
        EntityAttributeRegistry.register(ModEntities.GHOST, GhostEntity::createAttributes);
        PlayerEvent.ATTACK_ENTITY.register((player, level, target, hand, result) -> {
            if (!level.isClientSide && target instanceof net.minecraft.world.entity.LivingEntity living) {
                ServerStressManager.onAttackEntity((ServerPlayer) player, living);
            }
            return EventResult.pass();
        });

        EntityEvent.LIVING_HURT.register((entity, source, amount) -> {
            if (!entity.level().isClientSide && entity instanceof ServerPlayer) {
                ServerStressManager.onHurt((ServerPlayer) entity);
            }
            return EventResult.pass();
        });

        PlayerEvent.PLAYER_ADVANCEMENT.register((player, advancement) -> {
            if (!player.level().isClientSide && player instanceof ServerPlayer) {
                ServerStressManager.onAdvancement(player);
            }
        });

        PlayerEvent.DROP_ITEM.register((player, itemEntity) -> {
            if (!player.level().isClientSide) {
                ((OwnerAwareItemEntity) itemEntity).setOwnerUUID(player.getUUID());
            }
            return EventResult.pass();
        });

        PlayerEvent.PICKUP_ITEM_POST.register((player, itemEntity, stack) -> {
            if (player.level().isClientSide) return;
            ServerLevel level = (ServerLevel) player.level();
            Item item = stack.getItem();
            if (item == ModItems.DIARY_FRAGMENT_1.get()) {
                handleFragment1Pickup(level, (ServerPlayer) player);
            } else if (item == ModItems.DIARY_FRAGMENT_2.get()) {
                handleFragment2Pickup(level);
            }
        });

        PlayerEvent.PLAYER_JOIN.register((player) -> {
            if (!player.level().isClientSide) {
                MinecraftServer server = player.server;
                long currentTick = server.getTickCount();

                for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                    if (other == player) continue;
                    serverPlayer(other, currentTick, player);
                    serverPlayer(player, currentTick, other);
                }
            }
        });

        TickEvent.SERVER_POST.register(server -> {
            ServerVisibleManager.tick(server);
            ServerStressManager.tick(server);

            if (server.getTickCount() % 300 == 0) {
                triggerRealReveal(server);
            }
            if (server.getTickCount() % 6000 == 0) {
                spawnRelic(server);
                spawnRecorder(server);
            }

            handleGhostSpawn(server);
            handleDiarySpawn(server);
        });
    }

    private static void handleFragment1Pickup(ServerLevel level, ServerPlayer player) {
        DiaryProgressData data = DiaryProgressData.get(level);
        if (data.isFragment1Obtained()) return;

        data.setFragment1Obtained(player.getUUID());

        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof ItemEntity ie && ie.getItem().getItem() == ModItems.DIARY_FRAGMENT_1.get() && !ie.isRemoved()) {
                ie.discard();
            }
        }
    }

    private static void handleFragment2Pickup(ServerLevel level) {
        DiaryProgressData data = DiaryProgressData.get(level);
        if (!data.isFragment2Generated()) {
            data.setFragment2Generated();
        }

        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof ItemEntity ie && ie.getItem().getItem() == ModItems.DIARY_FRAGMENT_2.get() && !ie.isRemoved()) {
                ie.discard();
            }
        }
    }

    private static void serverPlayer(ServerPlayer player, long currentTick, ServerPlayer other) {
        long expireNewToOther = ServerVisibleManager.getExpireTick(player.getUUID(), other.getUUID());
        if (expireNewToOther > currentTick) {
            int remaining = (int) (expireNewToOther - currentTick);
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            VisiblePlayerPacket.encode(new VisiblePlayerPacket(other.getUUID(), true, remaining), buf);
            NetworkManager.sendToPlayer(player, ExistenceNetwork.VISIBLE_PLAYER_ID, buf);
        }
    }

    private static void handleGhostSpawn(MinecraftServer server) {
        long currentTick = server.getTickCount();
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        double avgStress = players.stream()
                .mapToDouble(p -> ServerStressManager.getLoad(p.getUUID()))
                .average().orElse(0.0);

        int adjustedMin = (int) (MIN_GHOST_INTERVAL * (1.0 - avgStress / 300.0));
        int adjustedMax = (int) (MAX_GHOST_INTERVAL * (1.0 - avgStress / 300.0));
        adjustedMin = Math.max(1200, adjustedMin);
        adjustedMax = Math.max(2400, adjustedMax);

        if (nextGhostSpawnTime == 0) {
            nextGhostSpawnTime = currentTick + adjustedMin + RANDOM.nextInt(adjustedMax - adjustedMin + 1);
        }
        if (currentTick >= nextGhostSpawnTime) {
            boolean success = trySpawnGhost(server);
            if (success) {
                int baseInterval = adjustedMin + RANDOM.nextInt(adjustedMax - adjustedMin + 1);
                if (!server.overworld().isDay()) {
                    baseInterval = baseInterval / 2;
                }
                nextGhostSpawnTime = currentTick + baseInterval;
            } else {
                nextGhostSpawnTime = currentTick + GHOST_RETRY_INTERVAL;
            }
        }
    }

    private static boolean trySpawnGhost(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return false;

        ServerPlayer target;
        ServerPlayer skinSource;

        if (players.size() == 1) {
            target = players.get(0);
            skinSource = target;
        } else {
            Map<ServerPlayer, Double> weightedTargets = new HashMap<>();
            for (ServerPlayer p : players) {
                double stress = ServerStressManager.getLoad(p.getUUID());
                weightedTargets.put(p, stress);
            }
            target = selectWeightedRandom(weightedTargets, server.overworld().getRandom());

            List<ServerPlayer> skinSources = players.stream()
                    .filter(p -> p != target)
                    .toList();
            if (skinSources.isEmpty()) return false;
            skinSource = skinSources.get(server.overworld().getRandom().nextInt(skinSources.size()));
        }

        return spawnGhostAtPlayer(target, skinSource, server);
    }

    private static boolean spawnGhostAtPlayer(ServerPlayer target, ServerPlayer skinSource, MinecraftServer server) {
        Level level = target.level();
        Vec3 lookVec = target.getLookAngle().normalize();
        Vec3 behind = target.position().subtract(lookVec.scale(5));
        double x = behind.x;
        double y = target.getY();
        double z = behind.z;
        boolean safe = false;
        BlockPos pos = BlockPos.containing(x, y, z);
        if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir() &&
                level.noCollision(target.getBoundingBox().move(x - target.getX(), y - target.getY(), z - target.getZ()))) {
            safe = true;
        }
        if (!safe) {
            for (int i = 1; i <= 5; i++) {
                for (int j = 0; j < 10; j++) {
                    double angle = server.overworld().getRandom().nextDouble() * 2 * Math.PI;
                    double dx = Math.cos(angle) * i;
                    double dz = Math.sin(angle) * i;
                    double nx = target.getX() + dx;
                    double nz = target.getZ() + dz;
                    double ny = target.getY();
                    BlockPos npos = BlockPos.containing(nx, ny, nz);
                    if (level.getBlockState(npos).isAir() && level.getBlockState(npos.above()).isAir()) {
                        x = nx;
                        z = nz;
                        safe = true;
                        break;
                    }
                }
                if (safe) break;
            }
        }
        if (!safe) return false;

        GhostEntity ghost = ModEntities.GHOST.get().create(level);
        if (ghost != null) {
            ghost.setPos(x, y, z);
            ghost.setSkinOwner(skinSource.getUUID());
            ghost.setTargetPlayer(target.getUUID());
            ghost.setCustomName(skinSource.getName());
            ghost.setCustomNameVisible(true);
            int lifespanTicks = 600 + RANDOM.nextInt(401);
            ghost.setDespawnTime(lifespanTicks);
            level.addFreshEntity(ghost);
            return true;
        }
        return false;
    }

    public static void spawnGhostForTarget(ServerPlayer target, MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        ServerPlayer skinSource;

        if (players.size() == 1) {
            skinSource = target;
        } else {
            List<ServerPlayer> others = players.stream()
                    .filter(p -> p != target)
                    .toList();
            if (others.isEmpty()) return;
            skinSource = others.get(server.overworld().getRandom().nextInt(others.size()));
        }

        spawnGhostAtPlayer(target, skinSource, server);
    }

    private static <T> T selectWeightedRandom(Map<T, Double> weights, RandomSource random) {
        double totalWeight = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        double r = random.nextDouble() * totalWeight;
        double cumulative = 0.0;
        for (Map.Entry<T, Double> entry : weights.entrySet()) {
            cumulative += entry.getValue();
            if (r < cumulative) {
                return entry.getKey();
            }
        }
        return weights.keySet().iterator().next();
    }

    private static void handleDiarySpawn(MinecraftServer server) {
        if (server.getTickCount() < nextDiarySpawnTime) return;
        if (server.getTickCount() % DIARY_SPAWN_INTERVAL != 0) return;

        ServerLevel level = server.overworld();
        DiaryProgressData data = DiaryProgressData.get(level);
        List<ServerPlayer> players = level.players();

        if (players.isEmpty()) return;

        if (!data.isFragment1Obtained()) {
            if (trySpawnDiaryFragment(level, 1, null)) {
                nextDiarySpawnTime = server.getTickCount() + DIARY_SPAWN_INTERVAL;
            }
        } else if (!data.isFragment2Generated()) {
            UUID holder = data.getFragment1Holder();
            if (players.size() == 1) {
                if (trySpawnDiaryFragment(level, 2, null)) {
                    data.setFragment2Generated();
                    nextDiarySpawnTime = server.getTickCount() + DIARY_SPAWN_INTERVAL;
                }
            } else {
                if (trySpawnDiaryFragment(level, 2, holder)) {
                    data.setFragment2Generated();
                    nextDiarySpawnTime = server.getTickCount() + DIARY_SPAWN_INTERVAL;
                }
            }
        }
    }

    private static boolean trySpawnDiaryFragment(ServerLevel level, int fragmentNumber, UUID hiddenFrom) {
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) return false;
        ServerPlayer targetPlayer = players.get(level.random.nextInt(players.size()));

        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = level.random.nextDouble() * 2 * Math.PI;
            double distance = 10 + level.random.nextDouble() * 10;
            double x = targetPlayer.getX() + Math.cos(angle) * distance;
            double z = targetPlayer.getZ() + Math.sin(angle) * distance;
            double y = targetPlayer.getY() + level.random.nextInt(5) - 2;
            BlockPos pos = BlockPos.containing(x, y, z);
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolid()) {
                ItemStack stack = (fragmentNumber == 1) ?
                        new ItemStack(ModItems.DIARY_FRAGMENT_1.get()) :
                        new ItemStack(ModItems.DIARY_FRAGMENT_2.get());
                ItemEntity itemEntity = new ItemEntity(level, x, y, z, stack);
                if (hiddenFrom != null) {
                    ((HiddenFromAware) itemEntity).setHiddenFrom(hiddenFrom);
                }
                level.addFreshEntity(itemEntity);
                return true;
            }
        }
        return false;
    }

    private static void triggerRealReveal(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.size() < 2) return;
        for (int i = 0; i < players.size(); i++) {
            ServerPlayer p1 = players.get(i);
            for (int j = i + 1; j < players.size(); j++) {
                ServerPlayer p2 = players.get(j);
                if (p1.distanceTo(p2) < 8 && isDay(p1.level())) {
                    int duration = 400;
                    ServerVisibleManager.setVisible(p1, p2, duration);
                    ServerVisibleManager.setVisible(p2, p1, duration);
                    FriendlyByteBuf buf1 = new FriendlyByteBuf(Unpooled.buffer());
                    VisiblePlayerPacket.encode(new VisiblePlayerPacket(p2.getUUID(), true, duration), buf1);
                    NetworkManager.sendToPlayer(p1, ExistenceNetwork.VISIBLE_PLAYER_ID, buf1);
                    FriendlyByteBuf buf2 = new FriendlyByteBuf(Unpooled.buffer());
                    VisiblePlayerPacket.encode(new VisiblePlayerPacket(p1.getUUID(), true, duration), buf2);
                    NetworkManager.sendToPlayer(p2, ExistenceNetwork.VISIBLE_PLAYER_ID, buf2);
                }
            }
        }
    }

    private static void spawnRelic(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;
        ServerPlayer player = players.get(server.overworld().getRandom().nextInt(players.size()));
        Level level = player.level();
        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = level.random.nextDouble() * 2 * Math.PI;
            double distance = 10 + level.random.nextDouble() * 10;
            double x = player.getX() + Math.cos(angle) * distance;
            double z = player.getZ() + Math.sin(angle) * distance;
            double y = player.getY() + level.random.nextInt(5) - 2;
            BlockPos pos = BlockPos.containing(x, y, z);
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolid()) {
                ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
                CompoundTag tag = book.getOrCreateTag();
                tag.putString("title", Component.Serializer.toJson(Component.translatable("item.existence.relic_book.title")));
                tag.putString("author", Component.Serializer.toJson(Component.translatable("item.existence.relic_book.author")));
                ListTag pages = new ListTag();
                pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable("book.existence.relic.page1"))));
                pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable("book.existence.relic.page2"))));
                pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable("book.existence.relic.page3"))));
                tag.put("pages", pages);
                book.setTag(tag);
                ItemEntity itemEntity = new ItemEntity(level, x, y, z, book);
                level.addFreshEntity(itemEntity);
                break;
            }
        }
    }

    private static void spawnRecorder(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;
        ServerPlayer player = players.get(server.overworld().getRandom().nextInt(players.size()));
        Level level = player.level();
        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = level.random.nextDouble() * 2 * Math.PI;
            double distance = 10 + level.random.nextDouble() * 10;
            double x = player.getX() + Math.cos(angle) * distance;
            double z = player.getZ() + Math.sin(angle) * distance;
            double y = player.getY() + level.random.nextInt(5) - 2;
            BlockPos pos = BlockPos.containing(x, y, z);
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolid()) {
                ItemStack recorderStack = new ItemStack(ModItems.RECORDER.get());
                ItemEntity itemEntity = new ItemEntity(level, x, y, z, recorderStack);
                level.addFreshEntity(itemEntity);
                break;
            }
        }
    }

    private static boolean isDay(Level level) {
        return level.isDay();
    }
}