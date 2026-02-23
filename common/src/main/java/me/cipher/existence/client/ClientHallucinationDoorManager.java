package me.cipher.existence.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ClientHallucinationDoorManager {
    private static final Map<BlockPos, OriginalDoorState> DOOR_STATES = new HashMap<>();

    private static class OriginalDoorState {
        BlockState state;
        long expireTick;

        OriginalDoorState(BlockState state, long expireTick) {
            this.state = state;
            this.expireTick = expireTick;
        }
    }

    public static void addDoor(BlockPos pos, int durationTicks) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;
        BlockState current = level.getBlockState(pos);
        if (!(current.getBlock() instanceof DoorBlock)) return;
        long expire = level.getGameTime() + durationTicks;
        OriginalDoorState existing = DOOR_STATES.get(pos);
        if (existing != null) {
            existing.expireTick = Math.max(existing.expireTick, expire);
        } else {
            DOOR_STATES.put(pos, new OriginalDoorState(current, expire));
        }
        openDoor(level, pos);
    }

    private static void openDoor(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock) {
            BlockState newState = state.setValue(DoorBlock.OPEN, true);
            level.setBlock(pos, newState, 3);
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;
        long currentTime = level.getGameTime();
        Iterator<Map.Entry<BlockPos, OriginalDoorState>> it = DOOR_STATES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, OriginalDoorState> entry = it.next();
            if (entry.getValue().expireTick <= currentTime) {
                BlockPos pos = entry.getKey();
                BlockState currentState = level.getBlockState(pos);
                if (currentState.getBlock() instanceof DoorBlock) {
                    level.setBlock(pos, entry.getValue().state, 3);
                }
                it.remove();
            }
        }
    }
}