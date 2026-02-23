package me.cipher.existence.mixin;

import me.cipher.existence.server.ServerStressManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerMixin {

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void onAddAdditionalSaveData(CompoundTag compound, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!self.level().isClientSide) {
            ServerStressManager.saveData(self, compound);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void onReadAdditionalSaveData(CompoundTag compound, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!self.level().isClientSide) {
            ServerStressManager.loadData(self, compound);
        }
    }
}