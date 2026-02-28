package me.cipher.existence.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class DiaryProgressData extends SavedData {
    private boolean fragment1Obtained = false;
    private UUID fragment1Holder = null;
    private boolean fragment2Generated = false;

    public static DiaryProgressData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                DiaryProgressData::new,
                DiaryProgressData::new,
                "existence_diary"
        );
    }

    public DiaryProgressData() {}

    public DiaryProgressData(CompoundTag tag) {
        fragment1Obtained = tag.getBoolean("fragment1Obtained");
        if (tag.hasUUID("fragment1Holder")) {
            fragment1Holder = tag.getUUID("fragment1Holder");
        }
        fragment2Generated = tag.getBoolean("fragment2Generated");
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        tag.putBoolean("fragment1Obtained", fragment1Obtained);
        if (fragment1Holder != null) {
            tag.putUUID("fragment1Holder", fragment1Holder);
        }
        tag.putBoolean("fragment2Generated", fragment2Generated);
        return tag;
    }

    public boolean isFragment1Obtained() { return fragment1Obtained; }
    public UUID getFragment1Holder() { return fragment1Holder; }
    public boolean isFragment2Generated() { return fragment2Generated; }

    public void setFragment1Obtained(UUID holder) {
        this.fragment1Obtained = true;
        this.fragment1Holder = holder;
        setDirty();
    }

    public void setFragment2Generated() {
        this.fragment2Generated = true;
        setDirty();
    }
}