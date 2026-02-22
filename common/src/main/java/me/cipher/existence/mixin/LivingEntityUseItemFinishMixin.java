package me.cipher.existence.mixin;

import me.cipher.existence.server.ServerStressManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityUseItemFinishMixin {

    @Inject(method = "completeUsingItem", at = @At("HEAD"))
    private void onCompleteUsingItem(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer player)) return;

        ItemStack usedItem = player.getUseItem();
        if (usedItem.isEmpty() || !usedItem.isEdible()) return;

        var item = usedItem.getItem();
        boolean badFood = item == Items.ROTTEN_FLESH ||
                item == Items.PORKCHOP ||
                item == Items.BEEF ||
                item == Items.CHICKEN ||
                item == Items.MUTTON ||
                item == Items.RABBIT ||
                item == Items.COD ||
                item == Items.SALMON ||
                item == Items.TROPICAL_FISH ||
                item == Items.PUFFERFISH;
        boolean healthyFood = item == Items.BREAD ||
                item == Items.COOKED_PORKCHOP ||
                item == Items.COOKED_BEEF ||
                item == Items.COOKED_CHICKEN ||
                item == Items.COOKED_MUTTON ||
                item == Items.COOKED_RABBIT ||
                item == Items.COOKED_COD ||
                item == Items.COOKED_SALMON ||
                item == Items.GOLDEN_APPLE ||
                item == Items.ENCHANTED_GOLDEN_APPLE ||
                item == Items.GOLDEN_CARROT;

        if (badFood) {
            ServerStressManager.onEatBadFood(player);
        } else if (healthyFood) {
            ServerStressManager.onEatHealthyFood(player);
        }
    }
}