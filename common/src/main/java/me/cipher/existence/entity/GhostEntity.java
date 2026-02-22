package me.cipher.existence.entity;

import me.cipher.existence.server.ServerStressManager;
import me.cipher.existence.sound.voicechat.GhostVoicePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
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
    private static final EntityDataAccessor<Boolean> DATA_VANISHED = SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.BOOLEAN);

    private long despawnTick;
    private int lookAwayTimer = 0;
    private int interactionCooldown = 0;
    private int vanishTimer = 0;
    private int reappearDelay = 0;
    private int speakCooldown = 0;

    public GhostEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SKIN_OWNER, Optional.empty());
        this.entityData.define(DATA_TARGET, Optional.empty());
        this.entityData.define(DATA_VANISHED, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new OpenDoorGoal(this, false));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0D));
    }


    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.1D);
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

    public boolean isVanished() {
        return this.entityData.get(DATA_VANISHED);
    }

    private void setVanished(boolean vanished) {
        this.entityData.set(DATA_VANISHED, vanished);
        this.setInvisible(vanished);
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
            if (random.nextInt(20) == 0) {
                level().addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                        getX(), getY() + 1.8, getZ(),
                        0, 0.1, 0);
            }
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

        if (isVanished()) {
            reappearDelay--;
            if (reappearDelay <= 0) {
                teleportToBlindSpot(target);
                setVanished(false);
                vanishTimer = 0;
            }
            return;
        }

        vanishTimer++;
        if (vanishTimer > 100 + random.nextInt(200)) {
            setVanished(true);
            reappearDelay = 60 + random.nextInt(120);
            vanishTimer = 0;
            level().broadcastEntityEvent(this, (byte) 60);
            return;
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

        Vec3 toGhost = this.position().subtract(target.position()).normalize();
        double dot = target.getLookAngle().dot(toGhost);
        if (dot > 0.85) {
            lookAwayTimer++;
            if (lookAwayTimer > 20) {
                teleportBehind(target);
                ServerStressManager.addAcuteEvent(target.getUUID());
                lookAwayTimer = 0;
            }
        } else {
            lookAwayTimer = 0;
        }

        if (target.swinging && !this.swinging) {
            this.swing(target.getUsedItemHand());
        }

        UUID skinOwner = getSkinOwner();
        if (skinOwner != null) {
            Player source = level().getPlayerByUUID(skinOwner);
            if (source != null && distanceToSqr(source) < 256) {
                this.getLookControl().setLookAt(source);
            }
        }

        if (speakCooldown <= 0 && random.nextInt(200) == 0) {
            if (target instanceof ServerPlayer serverTarget) {
                GhostVoicePlayer.trySpeak(this);
                speakCooldown = 400 + random.nextInt(400);
            }
        } else {
            speakCooldown--;
        }
    }

    private void teleportBehind(Player target) {
        Vec3 lookVec = target.getLookAngle().normalize();
        Vec3 behind = target.position().subtract(lookVec.scale(3));
        for (int attempt = 0; attempt < 10; attempt++) {
            double x = behind.x + (random.nextDouble() - 0.5) * 2;
            double z = behind.z + (random.nextDouble() - 0.5) * 2;
            double y = target.getY();
            BlockPos pos = BlockPos.containing(x, y, z);
            if (level().getBlockState(pos).isAir() && level().getBlockState(pos.above()).isAir()) {
                this.teleportTo(x, y, z);
                break;
            }
        }
    }

    private void teleportToBlindSpot(Player target) {
        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = 5 + random.nextDouble() * 8;
            double dx = Math.cos(angle) * distance;
            double dz = Math.sin(angle) * distance;
            double x = target.getX() + dx;
            double z = target.getZ() + dz;
            double y = target.getY() + random.nextInt(3) - 1;
            BlockPos pos = BlockPos.containing(x, y, z);
            if (level().getBlockState(pos).isAir() && level().getBlockState(pos.above()).isAir()) {
                this.teleportTo(x, y, z);
                break;
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 60 && level().isClientSide) {
            for (int i = 0; i < 10; i++) {
                level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 1, getZ(),
                        0, 0.1, 0);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        UUID skin = getSkinOwner();
        if (skin != null) compound.putUUID("SkinOwner", skin);
        UUID target = getTargetPlayer();
        if (target != null) compound.putUUID("TargetPlayer", target);
        long currentTick = level() instanceof ServerLevel ? ((ServerLevel)level()).getServer().getTickCount() : 0;
        compound.putInt("RemainingTicks", (int)(despawnTick - (level().getGameTime() - (currentTick - level().getGameTime()))));
        compound.putBoolean("Vanished", isVanished());
        compound.putInt("VanishTimer", vanishTimer);
        compound.putInt("ReappearDelay", reappearDelay);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.hasUUID("SkinOwner")) setSkinOwner(compound.getUUID("SkinOwner"));
        if (compound.hasUUID("TargetPlayer")) setTargetPlayer(compound.getUUID("TargetPlayer"));
        if (compound.contains("RemainingTicks")) {
            int remaining = compound.getInt("RemainingTicks");
            if (level() instanceof ServerLevel) {
                long currentServerTick = ((ServerLevel)level()).getServer().getTickCount();
                this.despawnTick = currentServerTick + remaining;
            } else {
                this.despawnTick = level().getGameTime() + remaining;
            }
        }
        setVanished(compound.getBoolean("Vanished"));
        vanishTimer = compound.getInt("VanishTimer");
        reappearDelay = compound.getInt("ReappearDelay");
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