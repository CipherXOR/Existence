package me.cipher.existence.mixin;

import me.cipher.existence.server.ServerStressManager;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {

    @Inject(method = "stopSleepInBed", at = @At("HEAD"))
    private void onStopSleepInBed(boolean wakeTime, boolean updateSleepingPlayers, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (wakeTime) {
            ServerStressManager.onSleep(player);
        }
    }
}