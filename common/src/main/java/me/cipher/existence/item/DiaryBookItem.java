package me.cipher.existence.item;

import io.netty.buffer.Unpooled;
import me.cipher.existence.Existence;
import me.cipher.existence.network.ExistenceNetwork;
import me.cipher.existence.network.VisiblePlayerPacket;
import me.cipher.existence.server.ServerVisibleManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import dev.architectury.networking.NetworkManager;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class DiaryBookItem extends Item {
    private static final int VISIBLE_DURATION = 200;
    private static final int COOLDOWN = 1200;

    public DiaryBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        MinecraftServer server = serverPlayer.getServer();
        Random random = new Random();

        if (random.nextBoolean()) {
            for (ServerPlayer other : serverPlayer.serverLevel().players()) {
                if (other == serverPlayer) continue;
                ServerVisibleManager.setVisible(serverPlayer, other, VISIBLE_DURATION);
                ServerVisibleManager.setVisible(other, serverPlayer, VISIBLE_DURATION);

                FriendlyByteBuf buf1 = new FriendlyByteBuf(Unpooled.buffer());
                VisiblePlayerPacket.encode(new VisiblePlayerPacket(other.getUUID(), true, VISIBLE_DURATION), buf1);
                NetworkManager.sendToPlayer(serverPlayer, ExistenceNetwork.VISIBLE_PLAYER_ID, buf1);

                FriendlyByteBuf buf2 = new FriendlyByteBuf(Unpooled.buffer());
                VisiblePlayerPacket.encode(new VisiblePlayerPacket(serverPlayer.getUUID(), true, VISIBLE_DURATION), buf2);
                NetworkManager.sendToPlayer(other, ExistenceNetwork.VISIBLE_PLAYER_ID, buf2);
            }
            serverPlayer.sendSystemMessage(Component.translatable("advancement.existence.diary_book"));
        } else {
            if (server != null) {
                Existence.spawnGhostForTarget(serverPlayer, server);
            }
        }

        serverPlayer.getCooldowns().addCooldown(this, COOLDOWN);
        return InteractionResultHolder.success(stack);
    }
}