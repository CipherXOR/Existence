package me.cipher.existence.mixin;

import me.cipher.existence.server.ServerVisibleManager;
import me.cipher.existence.util.OwnerAwareItemEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.UUID;
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity implements OwnerAwareItemEntity {
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(ItemEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    protected ItemEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void onDefineSynchedData(CallbackInfo ci) {
        this.entityData.define(DATA_OWNER, Optional.empty());
    }

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void onAddAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        UUID owner = getOwnerUUID();
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void onReadAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        if (tag.hasUUID("Owner")) {
            setOwnerUUID(tag.getUUID("Owner"));
        } else {
            setOwnerUUID(null);
        }
    }

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void onPlayerTouch(Player player, CallbackInfo ci) {
        if (!this.level().isClientSide) {
            UUID owner = getOwnerUUID();
            if (owner != null) {
                if (!ServerVisibleManager.isVisible(player.getUUID(), owner)) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "tryToMerge", at = @At("HEAD"), cancellable = true)
    private void onTryToMerge(ItemEntity other, CallbackInfo ci) {
        UUID thisOwner = getOwnerUUID();
        UUID otherOwner = ((OwnerAwareItemEntity) other).getOwnerUUID();

        if (thisOwner != null && otherOwner != null && !thisOwner.equals(otherOwner)) {
            ci.cancel();
        } else if (thisOwner == null && otherOwner != null) {
            ci.cancel();
        } else if (thisOwner != null && otherOwner == null) {
            ci.cancel();
        }
    }

    @Override
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNER).orElse(null);
    }

    @Override
    public void setOwnerUUID(UUID uuid) {
        this.entityData.set(DATA_OWNER, Optional.ofNullable(uuid));
    }
}