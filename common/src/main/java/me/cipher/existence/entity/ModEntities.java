package me.cipher.existence.entity;

import me.cipher.existence.Existence;
import me.cipher.existence.registry.DeferredRegistry;
import me.cipher.existence.registry.RegistryEntry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {
    public static final DeferredRegistry<EntityType<?>> ENTITIES =
            new DeferredRegistry<>(Existence.MOD_ID, "entity_type");

    public static final RegistryEntry<EntityType<GhostEntity>> GHOST =
            ENTITIES.register("ghost", () ->
                    EntityType.Builder.of(GhostEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(8)
                            .build("ghost"));

    public static void init() {}
}