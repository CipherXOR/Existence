package me.cipher.existence.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import me.cipher.existence.client.ClientStressManager;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public class FogRendererMixin {

    private static final float BASE_START = 78.0f * 0.3f;
    private static final float BASE_END   = 128.0f;

    private static final float MIN_SCALE = 0.15f;

    @Inject(method = "setupFog", at = @At("RETURN"))
    private static void onSetupFog(Camera camera, FogRenderer.FogMode fogMode,
                                   float farDistance, boolean thicken, float partialTick, CallbackInfo ci) {
        if (fogMode != FogRenderer.FogMode.FOG_TERRAIN) return;
        if (camera.getFluidInCamera() != FogType.NONE) return;

        int load = ClientStressManager.getLoad();
        float stress = Mth.clamp(load / 100.0f, 0.0f, 1.0f);

        float scale = 1.0f - stress * (1.0f - MIN_SCALE);

        float fogStart = BASE_START * scale;
        float fogEnd   = BASE_END   * scale;
        fogEnd = Math.max(fogStart + 1.0f, fogEnd);

        RenderSystem.setShaderFogStart(fogStart);
        RenderSystem.setShaderFogEnd(fogEnd);
    }
}