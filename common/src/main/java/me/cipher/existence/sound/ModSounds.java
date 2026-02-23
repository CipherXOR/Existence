package me.cipher.existence.sound;

import me.cipher.existence.Existence;
import me.cipher.existence.registry.DeferredRegistry;
import me.cipher.existence.registry.RegistryEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {
    public static final DeferredRegistry<SoundEvent> SOUNDS =
            new DeferredRegistry<>(Existence.MOD_ID, "sound_event");

    public static final RegistryEntry<SoundEvent> RECORDER_HORROR =
            SOUNDS.register("recorder_horror",
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Existence.MOD_ID, "recorder_horror")));

    public static final RegistryEntry<SoundEvent> RECORDER_HORROR2 =
            SOUNDS.register("recorder_horror2",
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Existence.MOD_ID, "recorder_horror2")));
    public static void init() {}
}