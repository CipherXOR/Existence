package me.cipher.existence.item;

import me.cipher.existence.Existence;
import me.cipher.existence.registry.DeferredRegistry;
import me.cipher.existence.registry.RegistryEntry;
import net.minecraft.world.item.Item;

public class ModItems {
    public static final DeferredRegistry<Item> ITEMS =
            new DeferredRegistry<>(Existence.MOD_ID, "item");

    public static final RegistryEntry<Item> RECORDER =
            ITEMS.register("recorder", () -> new RecorderItem(new Item.Properties()));

    public static void init() {}
}