package me.cipher.existence.forge;

import me.cipher.existence.Existence;
import me.cipher.existence.client.ClientHallucinationDoorManager;
import me.cipher.existence.client.ClientHallucinationManager;
import me.cipher.existence.client.ClientStressManager;
import me.cipher.existence.client.render.GhostEntityRenderer;
import me.cipher.existence.entity.GhostEntity;
import me.cipher.existence.entity.ModEntities;
import me.cipher.existence.item.ModItems;
import me.cipher.existence.registry.RegistryEntry;
import me.cipher.existence.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

import java.util.Random;

@Mod(Existence.MOD_ID)
public class ExistenceForge {
    private static final Random RANDOM = new Random();
    private static int messageCooldown = 0;
    private static int hallucinationCooldown = 0;

    public ExistenceForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::registerEntityRenderers);
        modBus.addListener(this::onEntityAttributeCreation);
        modBus.addListener(this::onRegister);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener((RenderGuiEvent.Post event) -> {
            Component msg = ClientHallucinationManager.getCurrentMessage();
            if (msg != null) {
                Minecraft mc = Minecraft.getInstance();
                int screenWidth = mc.getWindow().getGuiScaledWidth();
                int screenHeight = mc.getWindow().getGuiScaledHeight();
                float textWidth = mc.font.width(msg);
                float targetWidth = screenWidth * 0.8f;
                float scale = Math.min(targetWidth / textWidth, 10.0f);
                GuiGraphics guiGraphics = event.getGuiGraphics();
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(screenWidth / 2.0, screenHeight / 2.0, 0);
                guiGraphics.pose().scale(scale, scale, 1.0f);
                int textWidthPx = mc.font.width(msg);
                int textHeightPx = mc.font.lineHeight;
                guiGraphics.drawString(mc.font, msg,
                        -textWidthPx / 2,
                        -textHeightPx / 2,
                        0xFFFFFFFF,
                        true);
                guiGraphics.pose().popPose();
            }
        });
    }

    private void onRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.ENTITY_TYPE)) {
            for (RegistryEntry entry : ModEntities.ENTITIES.getEntries()) {
                EntityType<?> type = (EntityType<?>) entry.getSupplier().get();
                entry.initialize(type);
                event.register(ForgeRegistries.ENTITY_TYPES.getRegistryKey(), new ResourceLocation(entry.getId()), () -> type);
            }
        } else if (event.getRegistryKey().equals(Registries.ITEM)) {
            for (RegistryEntry entry : ModItems.ITEMS.getEntries()) {
                Item item = (Item) entry.getSupplier().get();
                entry.initialize(item);
                event.register(ForgeRegistries.ITEMS.getRegistryKey(), new ResourceLocation(entry.getId()), () -> item);
            }
        } else if (event.getRegistryKey().equals(Registries.SOUND_EVENT)) {
            for (RegistryEntry entry : ModSounds.SOUNDS.getEntries()) {
                SoundEvent sound = (SoundEvent) entry.getSupplier().get();
                entry.initialize(sound);
                event.register(ForgeRegistries.SOUND_EVENTS.getRegistryKey(), new ResourceLocation(entry.getId()), () -> sound);
            }
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        Existence.register();
        Existence.setupListeners();
    }

    private void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GHOST.get(), GhostEntityRenderer::new);
    }

    private void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.GHOST.get(), GhostEntity.createAttributes().build());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;
        int load = ClientStressManager.getLoad();
        ClientHallucinationManager.tick();
        ClientHallucinationDoorManager.tick();
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
        if (hallucinationCooldown > 0) hallucinationCooldown--;
    }
}