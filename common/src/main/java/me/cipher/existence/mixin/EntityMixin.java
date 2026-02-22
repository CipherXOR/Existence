package me.cipher.existence.mixin;

import me.cipher.existence.Existence;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "canCollideWith", at = @At("HEAD"), cancellable = true)
    private void onCanCollideWith(Entity other, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player) || !(other instanceof Player)) {
            return;
        }

        UUID selfId = self.getUUID();
        UUID otherId = other.getUUID();

        if (!self.level().isClientSide) {
            if (!Existence.VISIBILITY.isVisible(selfId, otherId) ||
                    !Existence.VISIBILITY.isVisible(otherId, selfId)) {
                cir.setReturnValue(false);
            }
        } else {
            Player localPlayer = Minecraft.getInstance().player;
            if (localPlayer == null) return;

            UUID localId = localPlayer.getUUID();
            if (selfId.equals(localId)) {
                if (!Existence.VISIBILITY.isVisible(localId, otherId)) {
                    cir.setReturnValue(false);
                }
            } else if (otherId.equals(localId)) {
                if (!Existence.VISIBILITY.isVisible(localId, selfId)) {
                    cir.setReturnValue(false);
                }
            }
        }
    }

    @Inject(method = "push", at = @At("HEAD"), cancellable = true)
    private void onPush(Entity entity, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player) || !(entity instanceof Player)) {
            return;
        }

        UUID selfId = self.getUUID();
        UUID otherId = entity.getUUID();

        if (!self.level().isClientSide) {
            if (!Existence.VISIBILITY.isVisible(selfId, otherId) ||
                    !Existence.VISIBILITY.isVisible(otherId, selfId)) {
                ci.cancel();
            }
        } else {
            Player localPlayer = Minecraft.getInstance().player;
            if (localPlayer == null) return;

            UUID localId = localPlayer.getUUID();
            if (selfId.equals(localId)) {
                if (!Existence.VISIBILITY.isVisible(localId, otherId)) {
                    ci.cancel();
                }
            } else if (otherId.equals(localId)) {
                if (!Existence.VISIBILITY.isVisible(localId, selfId)) {
                    ci.cancel();
                }
            }
        }
    }
}