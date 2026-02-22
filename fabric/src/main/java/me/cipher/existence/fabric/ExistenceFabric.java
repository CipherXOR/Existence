package me.cipher.existence.fabric;

import me.cipher.existence.Existence;
import me.cipher.existence.entity.ModEntities;
import me.cipher.existence.item.ModItems;
import me.cipher.existence.registry.RegistryEntry;
import me.cipher.existence.sound.ModSounds;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

import java.util.List;

public class ExistenceFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        for (RegistryEntry<? extends EntityType<?>> entry : ModEntities.ENTITIES.getEntries()) {
            registerEntity(entry);
        }
        for (RegistryEntry<? extends Item> entry : ModItems.ITEMS.getEntries()) {
            registerItem(entry);
        }
        for (RegistryEntry<? extends SoundEvent> entry : ModSounds.SOUNDS.getEntries()) {
            registerSound(entry);
        }

        Existence.register();
        Existence.setupListeners();
    }

    private static void registerEntity(RegistryEntry<? extends EntityType<?>> entry) {
        captureHelper(entry);
    }

    private static <I extends Item> void registerItem(RegistryEntry<I> entry) {
        I item = entry.getSupplier().get();
        entry.initialize(item);
        Registry.register(
                BuiltInRegistries.ITEM,
                new ResourceLocation(entry.getId()),
                item
        );
    }

    private static <S extends SoundEvent> void registerSound(RegistryEntry<S> entry) {
        S sound = entry.getSupplier().get();
        entry.initialize(sound);
        Registry.register(
                BuiltInRegistries.SOUND_EVENT,
                new ResourceLocation(entry.getId()),
                sound
        );
    }

    private static <T extends EntityType<?>> void captureHelper(RegistryEntry<T> entry) {
        T type = entry.getSupplier().get();
        entry.initialize(type);
        Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                new ResourceLocation(entry.getId()),
                type
        );
    }
}