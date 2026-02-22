package me.cipher.existence.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import me.cipher.existence.Existence;
import me.cipher.existence.client.VisiblePlayerManager;
import me.cipher.existence.util.OwnerAwareItemEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void onRender(E entity, double x, double y, double z, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        Player localPlayer = mc.player;
        if (localPlayer == null) return;

        if (entity instanceof Player && entity != localPlayer) {
            if (!VisiblePlayerManager.isVisible(entity.getUUID())) {
                ci.cancel();
            }
        }

        else if (entity instanceof ItemEntity) {
            UUID owner = ((OwnerAwareItemEntity) entity).getOwnerUUID();
            if (owner != null && !Existence.VISIBILITY.isVisible(localPlayer.getUUID(), owner)) {
                ci.cancel();
            }
        }
    }
}