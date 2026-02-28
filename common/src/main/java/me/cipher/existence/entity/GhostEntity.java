package me.cipher.existence.entity;

import me.cipher.existence.server.ServerStressManager;
import me.cipher.existence.sound.voicechat.GhostVoicePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public class GhostEntity extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> DATA_SKIN_OWNER = SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> DATA_TARGET = SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_GHOST_MODE = SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.INT);

    private long despawnTick;
    private int interactionCooldown = 0;
    private int speakCooldown = 0;
    private static final int SPEAK_INTERVAL = 200;
    private int mimicCooldown = 0;
    private int vanishCooldown = 0;

    public GhostEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SKIN_OWNER, Optional.empty());
        this.entityData.define(DATA_TARGET, Optional.empty());
        this.entityData.define(DATA_GHOST_MODE, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new OpenDoorGoal(this, false));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new FollowPlayerGoal(this, 1.0D, 3.0F, 10.0F));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D);
    }

    public void setSkinOwner(UUID uuid) {
        this.entityData.set(DATA_SKIN_OWNER, Optional.of(uuid));
    }

    public UUID getSkinOwner() {
        return this.entityData.get(DATA_SKIN_OWNER).orElse(null);
    }

    public void setTargetPlayer(UUID uuid) {
        this.entityData.set(DATA_TARGET, Optional.of(uuid));
    }

    public UUID getTargetPlayer() {
        return this.entityData.get(DATA_TARGET).orElse(null);
    }

    public void setGhostMode(int mode) {
        this.entityData.set(DATA_GHOST_MODE, mode);
    }

    public int getGhostMode() {
        return this.entityData.get(DATA_GHOST_MODE);
    }

    public void setDespawnTime(int ticks) {
        if (!level().isClientSide) {
            this.despawnTick = level().getGameTime() + ticks;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }

        if (level().getGameTime() >= despawnTick && despawnTick > 0) {
            this.remove(RemovalReason.DISCARDED);
            return;
        }

        Player target = level().getPlayerByUUID(getTargetPlayer());
        if (target == null) {
            return;
        }

        double stress = 0;
        if (target instanceof ServerPlayer serverTarget) {
            stress = ServerStressManager.getLoad(serverTarget.getUUID());
        }

        if (interactionCooldown <= 0) {
            BlockPos pos = this.blockPosition();
            searchLoop:
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        BlockPos checkPos = pos.offset(dx, dy, dz);
                        BlockState state = level().getBlockState(checkPos);
                        if (state.getBlock() instanceof TorchBlock || state.getBlock() instanceof LanternBlock) {
                            level().setBlock(checkPos, Blocks.AIR.defaultBlockState(), 3);
                            interactionCooldown = 40;
                            break searchLoop;
                        }
                    }
                }
            }
        } else {
            interactionCooldown--;
        }

        if (speakCooldown <= 0) {
            if (target instanceof ServerPlayer) {
                GhostVoicePlayer.trySpeak(this);
                speakCooldown = (int) (SPEAK_INTERVAL / (1 + stress / 200));
            }
        } else {
            speakCooldown--;
        }

        int baseCooldown = (int) (200 - stress * 1.5);
        baseCooldown = Math.max(40, baseCooldown);

        if (mimicCooldown <= 0) {
            if (random.nextInt(100) < stress / 2) {
                mimicPlayerAction(target);
                mimicCooldown = baseCooldown * 2;
            }
        } else {
            mimicCooldown--;
        }

        if (vanishCooldown <= 0) {
            if (random.nextInt(100) < stress / 4) {
                vanishAndReappear(target);
                vanishCooldown = baseCooldown * 4;
            }
        } else {
            vanishCooldown--;
        }
    }

    private void mimicPlayerAction(Player target) {
        if (target.swinging) {
            this.swing(target.getUsedItemHand());
        }
        this.setShiftKeyDown(target.isShiftKeyDown());
        this.getLookControl().setLookAt(target.getLookAngle().x * 10, target.getEyeY(), target.getLookAngle().z * 10);
    }

    private void vanishAndReappear(Player target) {
        this.setInvisible(true);
        this.teleportTo(target.getX() + random.nextInt(10) - 5, target.getY(), target.getZ() + random.nextInt(10) - 5);
        this.setInvisible(false);
    }

    private static class FollowPlayerGoal extends Goal {
        private final GhostEntity ghost;
        private final double speedModifier;
        private final float stopDistance;
        private final float followDistance;
        private Player target;
        private double x, y, z;

        public FollowPlayerGoal(GhostEntity ghost, double speed, float stopDist, float followDist) {
            this.ghost = ghost;
            this.speedModifier = speed;
            this.stopDistance = stopDist;
            this.followDistance = followDist;
        }

        @Override
        public boolean canUse() {
            UUID targetId = ghost.getTargetPlayer();
            if (targetId == null) return false;
            this.target = ghost.level().getPlayerByUUID(targetId);
            if (target == null) return false;
            return ghost.distanceTo(target) > followDistance;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && ghost.distanceTo(target) > stopDistance && ghost.distanceTo(target) <= followDistance * 2;
        }

        @Override
        public void start() {
            Vec3 lookVec = target.getLookAngle().normalize();
            double angle = ghost.random.nextDouble() * 2 * Math.PI;
            double offsetX = Math.cos(angle) * 2;
            double offsetZ = Math.sin(angle) * 2;
            this.x = target.getX() - lookVec.x * 3 + offsetX;
            this.y = target.getY();
            this.z = target.getZ() - lookVec.z * 3 + offsetZ;
            ghost.getNavigation().moveTo(x, y, z, speedModifier);
        }

        @Override
        public void tick() {
            if (target == null) return;
            if (ghost.distanceToSqr(x, y, z) < 4 || ghost.tickCount % 40 == 0) {
                Vec3 lookVec = target.getLookAngle().normalize();
                double angle = ghost.random.nextDouble() * 2 * Math.PI;
                double offsetX = Math.cos(angle) * 2;
                double offsetZ = Math.sin(angle) * 2;
                this.x = target.getX() - lookVec.x * 3 + offsetX;
                this.y = target.getY();
                this.z = target.getZ() - lookVec.z * 3 + offsetZ;
                ghost.getNavigation().moveTo(x, y, z, speedModifier);
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        super.handleEntityEvent(id);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        UUID skin = getSkinOwner();
        if (skin != null) compound.putUUID("SkinOwner", skin);
        UUID target = getTargetPlayer();
        if (target != null) compound.putUUID("TargetPlayer", target);
        compound.putInt("GhostMode", getGhostMode());
        long currentTick = level() instanceof ServerLevel ? ((ServerLevel)level()).getServer().getTickCount() : 0;
        compound.putInt("RemainingTicks", (int)(despawnTick - (level().getGameTime() - (currentTick - level().getGameTime()))));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.hasUUID("SkinOwner")) setSkinOwner(compound.getUUID("SkinOwner"));
        if (compound.hasUUID("TargetPlayer")) setTargetPlayer(compound.getUUID("TargetPlayer"));
        if (compound.contains("GhostMode")) setGhostMode(compound.getInt("GhostMode"));
        if (compound.contains("RemainingTicks")) {
            int remaining = compound.getInt("RemainingTicks");
            if (level() instanceof ServerLevel) {
                long currentServerTick = ((ServerLevel)level()).getServer().getTickCount();
                this.despawnTick = currentServerTick + remaining;
            } else {
                this.despawnTick = level().getGameTime() + remaining;
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) {
            Entity attacker = source.getEntity();
            if (attacker instanceof Player player) {
                UUID targetUuid = getTargetPlayer();
                if (player.getUUID().equals(targetUuid)) {
                    ServerStressManager.addKillEvent(player.getUUID());
                    this.remove(RemovalReason.DISCARDED);
                }
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && getTargetPlayer() != null) {
            ServerStressManager.clearLastGhostPos(getTargetPlayer());
        }
        super.remove(reason);
    }

    @Override
    public boolean canBeCollidedWith() { return true; }

    @Override
    public void push(Entity entity) {
        if (entity instanceof Player player) {
            UUID targetUuid = getTargetPlayer();
            if (targetUuid != null && !player.getUUID().equals(targetUuid)) return;
        }
        super.push(entity);
    }

    @Override
    public boolean isPushable() { return true; }
}