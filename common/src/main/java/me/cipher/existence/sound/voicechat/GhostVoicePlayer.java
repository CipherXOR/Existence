package me.cipher.existence.sound.voicechat;

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import me.cipher.existence.entity.GhostEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

public class GhostVoicePlayer {
    private static final Random RANDOM = new Random();
    private static final int FRAME_SIZE = 960;

    public static void trySpeak(GhostEntity ghost) {
        if (ghost.level().isClientSide) return;
        UUID skinOwner = ghost.getSkinOwner();
        if (skinOwner == null) return;

        List<short[]> snippets = ExistenceVoicechatPlugin.getSnippets(skinOwner);
        if (snippets.isEmpty()) return;

        VoicechatServerApi api = ExistenceVoicechatPlugin.serverApi;
        if (api == null) return;

        short[] snippet = snippets.get(RANDOM.nextInt(snippets.size()));

        de.maxhenkel.voicechat.api.Entity apiEntity = api.fromEntity(ghost);
        EntityAudioChannel channel = api.createEntityAudioChannel(UUID.randomUUID(), apiEntity);
        Objects.requireNonNull(channel).setDistance(16.0f);
        channel.setWhispering(true);

        Thread playbackThread = getThread(api, snippet, channel);
        playbackThread.start();
    }

    private static @NotNull Thread getThread(VoicechatServerApi api, short[] snippet, EntityAudioChannel channel) {
        Thread playbackThread = new Thread(() -> {
            OpusEncoder encoder = null;
            try {
                encoder = api.createEncoder();
                int offset = 0;
                while (offset < snippet.length) {
                    int len = Math.min(FRAME_SIZE, snippet.length - offset);
                    short[] frame = new short[len];
                    System.arraycopy(snippet, offset, frame, 0, len);
                    byte[] opus = encoder.encode(frame);
                    channel.send(opus);
                    offset += len;
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException ignored) {}
                }
                channel.flush();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (encoder != null) {
                    encoder.close();
                }
            }
        });
        playbackThread.setDaemon(true);
        return playbackThread;
    }
}