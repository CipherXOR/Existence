package me.cipher.existence.item;

import me.cipher.existence.Existence;
import me.cipher.existence.registry.DeferredRegistry;
import me.cipher.existence.registry.RegistryEntry;
import net.minecraft.world.item.Item;

public class ModItems {
    public static final DeferredRegistry<Item> ITEMS =
            new DeferredRegistry<>(Existence.MOD_ID, "item");

    public static final RegistryEntry<Item> RECORDER =
            ITEMS.register("recorder", () -> new RecorderItem(new Item.Properties().stacksTo(1)));

    public static final RegistryEntry<Item> DIARY_FRAGMENT_1 =
            ITEMS.register("diary_fragment_1", () -> new DiaryFragmentItem(new Item.Properties().stacksTo(1)));

    public static final RegistryEntry<Item> DIARY_FRAGMENT_2 =
            ITEMS.register("diary_fragment_2", () -> new DiaryFragmentItem(new Item.Properties().stacksTo(1)));

    public static final RegistryEntry<Item> DIARY_BOOK =
            ITEMS.register("diary_book", () -> new DiaryBookItem(new Item.Properties().stacksTo(1)));

    public static void init() {}
}