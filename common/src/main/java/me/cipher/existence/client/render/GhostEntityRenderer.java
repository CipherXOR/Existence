package me.cipher.existence.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import me.cipher.existence.entity.GhostEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public class GhostEntityRenderer extends LivingEntityRenderer<GhostEntity, PlayerModel<GhostEntity>> {
    public GhostEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(GhostEntity entity) {
        UUID skinOwner = entity.getSkinOwner();
        if (skinOwner != null) {
            var playerInfo = Objects.requireNonNull(Minecraft.getInstance().getConnection()).getPlayerInfo(skinOwner);
            if (playerInfo != null) {
                return playerInfo.getSkinLocation();
            }
        }
        return DefaultPlayerSkin.getDefaultSkin();
    }

    @Override
    protected boolean shouldShowName(GhostEntity entity) {
        return true;
    }

    @Override
    public void render(GhostEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Player localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) return;
        UUID targetUuid = entity.getTargetPlayer();
        if (!localPlayer.getUUID().equals(targetUuid)) {
            return;
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}