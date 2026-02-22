package me.cipher.existence.sound.voicechat;

import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.*;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import me.cipher.existence.Existence;
import me.cipher.existence.entity.GhostEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.openal.AL10;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ExistenceVoicechatPlugin implements VoicechatPlugin {
    public static VoicechatServerApi serverApi;
    private static final Map<UUID, OpusDecoder> decoders = new ConcurrentHashMap<>();
    private static final Map<UUID, List<short[]>> playerSnippets = new ConcurrentHashMap<>();
    private static final Map<UUID, short[]> currentSnippet = new ConcurrentHashMap<>();
    private static final int MAX_SNIPPETS = 20;
    private static final int MIN_SNIPPET_LENGTH = 960 * 40;

    @Override
    public String getPluginId() {
        return Existence.MOD_ID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
        registration.registerEvent(PlayerDisconnectedEvent.class, this::onPlayerDisconnect);
        registration.registerEvent(VoicechatServerStartedEvent.class, e -> serverApi = e.getVoicechat());
        registration.registerEvent(VoicechatServerStoppedEvent.class, e -> {
            decoders.values().forEach(OpusDecoder::close);
            decoders.clear();
            playerSnippets.clear();
            currentSnippet.clear();
            serverApi = null;
        });

        registration.registerEvent(OpenALSoundEvent.class, this::onOpenALSound);
    }

    private void onMicrophonePacket(MicrophonePacketEvent event) {
        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null) return;
        if (!(sender.getPlayer().getPlayer() instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();

        byte[] opusData = event.getPacket().getOpusEncodedData();
        if (opusData.length > 0) {
            OpusDecoder decoder = decoders.computeIfAbsent(uuid, k -> event.getVoicechat().createDecoder());
            short[] pcm = decoder.decode(opusData);
            short[] existing = currentSnippet.getOrDefault(uuid, new short[0]);
            short[] combined = new short[existing.length + pcm.length];
            System.arraycopy(existing, 0, combined, 0, existing.length);
            System.arraycopy(pcm, 0, combined, existing.length, pcm.length);
            currentSnippet.put(uuid, combined);
        } else {
            OpusDecoder decoder = decoders.get(uuid);
            if (decoder != null) decoder.resetState();
            short[] snippet = currentSnippet.remove(uuid);
            if (snippet != null && snippet.length >= MIN_SNIPPET_LENGTH) {
                List<short[]> snippets = playerSnippets.computeIfAbsent(uuid, k -> new ArrayList<>());
                snippets.add(snippet);
                if (snippets.size() > MAX_SNIPPETS) snippets.remove(0);
            }
        }
    }

    private void onPlayerDisconnect(PlayerDisconnectedEvent event) {
        UUID uuid = event.getPlayerUuid();
        OpusDecoder decoder = decoders.remove(uuid);
        if (decoder != null) decoder.close();
        playerSnippets.remove(uuid);
        currentSnippet.remove(uuid);
    }

    public static List<short[]> getSnippets(UUID playerUuid) {
        return playerSnippets.getOrDefault(playerUuid, Collections.emptyList());
    }

    private void onOpenALSound(OpenALSoundEvent event) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        Player player = client.player;

        client.level.getEntitiesOfClass(GhostEntity.class, player.getBoundingBox().inflate(50)).forEach(ghost -> {
            if (event.getChannelId() != null && event.getChannelId().equals(ghost.getUUID())) {
                AL10.alSourcef(event.getSource(), AL10.AL_PITCH, 0.7f);
            }
        });
    }
}