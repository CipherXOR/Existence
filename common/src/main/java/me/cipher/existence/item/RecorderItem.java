package me.cipher.existence.item;

import me.cipher.existence.server.ServerStressManager;
import me.cipher.existence.sound.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RecorderItem extends Item {
    public RecorderItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.RECORDER_HORROR.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            if (player instanceof ServerPlayer serverPlayer) {
                ServerStressManager.addLoad(serverPlayer.getUUID(), 5.0);
            }
            player.getCooldowns().addCooldown(this, 200);
        } else {
            player.playSound(ModSounds.RECORDER_HORROR.get(), 1.0F, 1.0F);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.existence.recorder.desc"));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}