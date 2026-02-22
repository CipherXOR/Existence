package me.cipher.existence.forge;

import me.cipher.existence.Existence;
import me.cipher.existence.client.ClientStressManager;
import me.cipher.existence.client.render.GhostEntityRenderer;
import me.cipher.existence.entity.ModEntities;
import me.cipher.existence.item.ModItems;
import me.cipher.existence.registry.RegistryEntry;
import me.cipher.existence.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.Random;

@Mod(Existence.MOD_ID)
public class ExistenceForge {
    private static final Random RANDOM = new Random();
    private static int messageCooldown = 0;
    private static int hallucinationCooldown = 0;

    public ExistenceForge() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::registerEntityRenderers);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
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

    private void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GHOST.get(), GhostEntityRenderer::new);
    }

    private static <T extends EntityType<?>> void registerEntity(RegistryEntry<T> entry) {
        T type = entry.getSupplier().get();
        entry.initialize(type);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, new ResourceLocation(entry.getId()), type);
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
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;
        int load = ClientStressManager.getLoad();

        if (load > 50 && RANDOM.nextInt(100) == 0) {
            client.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                    client.player.getX() + (RANDOM.nextDouble() - 0.5) * 20,
                    client.player.getY() + RANDOM.nextDouble() * 5,
                    client.player.getZ() + (RANDOM.nextDouble() - 0.5) * 20,
                    0, 0.1, 0);
        }

        if (load > 80) {
            if (messageCooldown <= 0 && RANDOM.nextInt(300) == 0) {
                client.player.displayClientMessage(
                        Component.translatable("message.existence.see_it")
                                .withStyle(style -> style.withColor(0xAA0000).withItalic(true)),
                        false);
                messageCooldown = 2400;
            }
        }
        if (messageCooldown > 0) messageCooldown--;
        if (hallucinationCooldown > 0) {
            hallucinationCooldown--;
        }
    }
}