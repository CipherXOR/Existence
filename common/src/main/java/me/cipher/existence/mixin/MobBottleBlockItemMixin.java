package me.cipher.existence.mixin;

import firis.mobbottle.common.item.MobBottleBlockItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(MobBottleBlockItem.class)
public class MobBottleBlockItemMixin {

    @Inject(method = "catchMobBottle", at = @At("HEAD"), cancellable = true)
    private void onCatchMobBottle(Player player, Entity entity, InteractionHand hand, CallbackInfoReturnable<Boolean> cir) {
        if (entity != null) {
            Optional<ResourceKey<EntityType<?>>> key = BuiltInRegistries.ENTITY_TYPE.getResourceKey(entity.getType());
            if (key.isPresent() && key.get().location().getNamespace().equals("existence")) {
                cir.setReturnValue(false);
            }
        }
    }
}