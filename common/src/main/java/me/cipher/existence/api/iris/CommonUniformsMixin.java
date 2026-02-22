package me.cipher.existence.api.iris;

import me.cipher.existence.client.ClientStressManager;
import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.irisshaders.iris.shaderpack.IdMap;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.uniforms.CommonUniforms;
import net.irisshaders.iris.uniforms.FrameUpdateNotifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CommonUniforms.class, remap = false)
public class CommonUniformsMixin {
    @Inject(method = "addNonDynamicUniforms", at = @At("RETURN"), remap = false)
    private static void onAddNonDynamicUniforms(UniformHolder uniforms, IdMap idMap, PackDirectives directives, FrameUpdateNotifier updateNotifier, CallbackInfo ci) {
        uniforms.uniform1f(UniformUpdateFrequency.PER_FRAME, "existenceStress",
                () -> ClientStressManager.getLoad() / 100.0f);
    }
}