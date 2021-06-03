package de.maxhenkel.voicechat.voice.client;

import de.foorcee.labymod.voicechat.client.LabymodVoicechatClient;
import de.maxhenkel.voicechat.VoicechatClient;
import de.maxhenkel.voicechat.voice.common.OpusDecoder;
import de.maxhenkel.voicechat.voice.common.PlayerState;
import de.maxhenkel.voicechat.voice.common.Utils;
import net.labymod.voicechat.protocol.packet.audio.AudioChunkServer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.tuple.Pair;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class AudioChannel extends Thread {

    private Minecraft minecraft;
    private LabymodVoicechatClient client;
    private UUID uuid;
    private BlockingQueue<AudioChunkServer> queue;
    private long lastPacketTime;
    private SourceDataLine speaker;
    private FloatControl gainControl;
    private boolean stopped;
    private OpusDecoder decoder;

    public AudioChannel(LabymodVoicechatClient client, UUID uuid) {
        this.client = client;
        this.uuid = uuid;
        this.queue = new LinkedBlockingQueue<>();
        this.lastPacketTime = System.currentTimeMillis();
        this.stopped = false;
        this.decoder = new OpusDecoder(client.getAudioChannelConfig().getSampleRate(), client.getAudioChannelConfig().getFrameSize(), client.getMaxPacketSize());
        this.minecraft = Minecraft.getInstance();
        setDaemon(true);
        setName("AudioChannelThread-" + uuid.toString());
        VoicechatClient.LOGGER.debug("Creating audio channel for " + uuid);
    }

    public boolean canKill() {
        return System.currentTimeMillis() - lastPacketTime > 30_000L;
    }

    public void closeAndKill() {
        VoicechatClient.LOGGER.debug("Closing audio channel for " + uuid);
        stopped = true;
    }

    public UUID getUUID() {
        return uuid;
    }

    public void addToQueue(AudioChunkServer p) {
        queue.add(p);
    }

    @Override
    public void run() {
        try {
            AudioFormat af = client.getAudioChannelConfig().getStereoFormat();
            speaker = DataLines.getSpeaker();
            speaker.open(af);
            gainControl = (FloatControl) speaker.getControl(FloatControl.Type.MASTER_GAIN);
            while (!stopped) {

                PlayerState playerState = VoicechatClient.CLIENT.getPlayerStateManager().getState(uuid);
                boolean disabled = VoicechatClient.CLIENT.getPlayerStateManager().isDisabled()
                        || playerState != null && playerState.isDisabled();

                if (disabled) {
                    speaker.stop();
                    queue.clear();
                    closeAndKill();
                    return;
                }

                // Stopping the data line when the buffer is empty
                // to prevent the last sound getting repeated
                if (speaker.getBufferSize() - speaker.available() <= 0 && speaker.isActive()) {
                    speaker.stop();
                }

                AudioChunkServer packet = queue.poll(10, TimeUnit.MILLISECONDS);
                if (packet == null) {
                    continue;
                }
                lastPacketTime = System.currentTimeMillis();


                ByteBuffer buffer = ByteBuffer.wrap(packet.getBytes());
                long time = buffer.getLong();
                byte[] encodedData = new byte[buffer.remaining()];
                buffer.get(encodedData);

                if (minecraft.level == null || minecraft.player == null) {
                    continue;
                }

                // Filling the speaker with silence for one packet size
                // to build a small buffer to compensate for network latency
                if (speaker.getBufferSize() - speaker.available() <= 0) {
                    byte[] data = new byte[Math.min(client.getAudioChannelConfig().getFrameSize() * VoicechatClient.CLIENT_CONFIG.outputBufferSize.get(), speaker.getBufferSize() - client.getAudioChannelConfig().getFrameSize())];
                    speaker.write(data, 0, data.length);
                }

                VoicechatClient.CLIENT.getTalkCache().updateTalking(uuid);
                byte[] decodedAudio = decoder.decode(encodedData);

                writeToSpeaker(decodedAudio);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            if (speaker != null) {
                speaker.stop();
                speaker.flush();
                speaker.close();
            }
            decoder.close();
            VoicechatClient.LOGGER.debug("Closed audio channel for " + uuid);
        }
    }

    private void writeToSpeaker(byte[] monoData) {
        PlayerState state = VoicechatClient.CLIENT.getPlayerStateManager().getState(uuid);
        byte[] stereo;
        float percentage = 1F;

        Player player = minecraft.level.getPlayerByUUID(uuid);
        if (player == null) {
            return;
        }
        float distance = player.distanceTo(minecraft.player);
        float fadeDistance = VoicechatClient.CLIENT_CONFIG.voiceChatFadeDistance.get().floatValue();
        float maxDistance = VoicechatClient.CLIENT_CONFIG.voiceChatDistance.get().floatValue();

        if (distance > fadeDistance) {
            percentage = 1F - Math.min((distance - fadeDistance) / (maxDistance - fadeDistance), 1F);
        }

        if (VoicechatClient.CLIENT_CONFIG.stereo.get()) {
            Pair<Float, Float> stereoVolume = Utils.getStereoVolume(minecraft, player.position(), maxDistance);
            stereo = Utils.convertToStereo(monoData, percentage * stereoVolume.getLeft(), percentage * stereoVolume.getRight());
        } else {
            stereo = Utils.convertToStereo(monoData, percentage, percentage);
        }

        float db = Math.min(Math.max(Utils.percentageToDB(VoicechatClient.CLIENT_CONFIG.voiceChatVolume.get().floatValue() * (float) VoicechatClient.VOLUME_CONFIG.getVolume(uuid)),
                gainControl.getMinimum()), gainControl.getMaximum());
        gainControl.setValue(db);

        speaker.write(stereo, 0, stereo.length);
        speaker.start();
    }

    public boolean isClosed() {
        return stopped;
    }

}
