package me.cipher.existence;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.InteractionEvent;
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
import me.cipher.existence.server.ServerStressManager;
import me.cipher.existence.server.ServerVisibilityChecker;
import me.cipher.existence.server.ServerVisibleManager;
import me.cipher.existence.util.OwnerAwareItemEntity;
import me.cipher.existence.util.VisibilityChecker;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

public class Existence {
    public static final String MOD_ID = "existence";
    private static final Random RANDOM = new Random();
    private static long nextGhostSpawnTime = 0;
    private static final int MIN_GHOST_INTERVAL = 6000;
    private static final int MAX_GHOST_INTERVAL = 9600;
    private static final int GHOST_RETRY_INTERVAL = 600;
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

        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, face) -> {
            if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                BlockState state = player.level().getBlockState(pos);
                if (state.getBlock() instanceof BedBlock) {
                    ServerStressManager.onSleep(serverPlayer);
                }
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
        });
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
        if (nextGhostSpawnTime == 0) {
            nextGhostSpawnTime = currentTick + MIN_GHOST_INTERVAL + RANDOM.nextInt(MAX_GHOST_INTERVAL - MIN_GHOST_INTERVAL + 1);
        }
        if (currentTick >= nextGhostSpawnTime) {
            boolean success = trySpawnGhost(server);
            if (success) {
                int baseInterval = MIN_GHOST_INTERVAL + RANDOM.nextInt(MAX_GHOST_INTERVAL - MIN_GHOST_INTERVAL + 1);
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
        if (players.size() < 2) return false;
        ServerPlayer skinSource = players.get(server.overworld().getRandom().nextInt(players.size()));
        List<ServerPlayer> candidates = players.stream().filter(p -> p != skinSource).toList();
        if (candidates.isEmpty()) return false;
        ServerPlayer target = candidates.get(server.overworld().getRandom().nextInt(candidates.size()));
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
            int lifespanTicks = 60 + RANDOM.nextInt(241);
            ghost.setDespawnTime(lifespanTicks);
            level.addFreshEntity(ghost);
            return true;
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