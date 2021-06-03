package de.maxhenkel.voicechat.voice.client;

import de.foorcee.labymod.voicechat.client.LabymodVoicechatClient;
import de.maxhenkel.opus4j.Opus;
import de.maxhenkel.voicechat.VoicechatClient;
import de.maxhenkel.voicechat.voice.common.OpusEncoder;
import de.maxhenkel.voicechat.voice.common.Utils;
import lombok.extern.log4j.Log4j2;
import net.labymod.voicechat.protocol.packet.audio.AudioChunkServer;
import net.minecraft.client.Minecraft;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.nio.ByteBuffer;
import java.util.UUID;

@Log4j2
public class MicThread extends Thread {

    private UUID uuid;
    private LabymodVoicechatClient client;
    private TargetDataLine mic;
    private boolean running;
    private boolean microphoneLocked;
    private OpusEncoder encoder;

    public MicThread(LabymodVoicechatClient client) throws LineUnavailableException {
        this.client = client;
        this.running = true;
        this.encoder = new OpusEncoder(client.getAudioChannelConfig().getSampleRate(),
                client.getAudioChannelConfig().getFrameSize(), client.getMaxPacketSize(), Opus.OPUS_APPLICATION_VOIP);
        setDaemon(true);
        setName("MicrophoneThread");
        AudioFormat af = client.getAudioChannelConfig().getMonoFormat();
        mic = DataLines.getMicrophone();
        mic.open(af);
        this.uuid = Minecraft.getInstance().getUser().getGameProfile().getId();
    }

    @Override
    public void run() {
        while (running) {
            // Checking here for timeouts, because we don't have any other looping thread
            client.checkTimeout();
            if (microphoneLocked) {
                Utils.sleep(10);
            } else {
                MicrophoneActivationType type = VoicechatClient.CLIENT_CONFIG.microphoneActivationType.get();
                if (VoicechatClient.CLIENT.getPlayerStateManager().isDisabled()) {
                    Utils.sleep(10);
                } else if (type.equals(MicrophoneActivationType.PTT)) {
                    ptt();
                } else if (type.equals(MicrophoneActivationType.VOICE)) {
                    voice();
                }
            }
        }
    }

    private boolean activating;
    private int deactivationDelay;
    private byte[] lastBuff;

    private void voice() {
        wasPTT = false;

        if (VoicechatClient.CLIENT.getPlayerStateManager().isMuted()) {
            activating = false;
            if (mic.isActive()) {
                mic.stop();
                mic.flush();
            }
            Utils.sleep(10);
            return;
        }

        int dataLength = client.getAudioChannelConfig().getFrameSize();

        mic.start();

        if (mic.available() < dataLength) {
            Utils.sleep(1);
            return;
        }
        byte[] buff = new byte[dataLength];
        mic.read(buff, 0, buff.length);
        Utils.adjustVolumeMono(buff, VoicechatClient.CLIENT_CONFIG.microphoneAmplification.get().floatValue());

        int offset = Utils.getActivationOffset(buff, VoicechatClient.CLIENT_CONFIG.voiceActivationThreshold.get());
        if (activating) {
            if (offset < 0) {
                if (deactivationDelay >= VoicechatClient.CLIENT_CONFIG.deactivationDelay.get()) {
                    activating = false;
                    deactivationDelay = 0;
                } else {
                    sendAudioPacket(buff);
                    deactivationDelay++;
                }
            } else {
                sendAudioPacket(buff);
            }
        } else {
            if (offset > 0) {
                if (lastBuff != null) {
                    sendAudioPacket(lastBuff);
                }
                sendAudioPacket(buff);
                activating = true;
            }
        }
        lastBuff = buff;
    }

    private boolean wasPTT;

    private void ptt() {
        activating = false;
        int dataLength = client.getAudioChannelConfig().getFrameSize();

        if (!VoicechatClient.CLIENT.getPttKeyHandler().isPTTDown()) {
            if (wasPTT) {
                mic.stop();
                mic.flush();
                wasPTT = false;
            }
            Utils.sleep(10);
            return;
        } else {
            wasPTT = true;
        }

        mic.start();

        if (mic.available() < dataLength) {
            Utils.sleep(1);
            return;
        }
        byte[] buff = new byte[dataLength];
        mic.read(buff, 0, buff.length);
        Utils.adjustVolumeMono(buff, VoicechatClient.CLIENT_CONFIG.microphoneAmplification.get().floatValue());
        sendAudioPacket(buff);
    }

    private void sendAudioPacket(byte[] data) {
        try {
            byte[] encoded = encoder.encode(data);

            ByteBuffer encodedBuffer = ByteBuffer.allocate(encoded.length + 8);
            encodedBuffer.putLong(System.nanoTime());
            encodedBuffer.put(encoded);
            encodedBuffer.flip();
            encoded = encodedBuffer.array();

            client.sendUdpPacket(new AudioChunkServer(uuid, encoded));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TargetDataLine getMic() {
        return mic;
    }

    public boolean isTalking() {
        return !microphoneLocked && (activating || wasPTT);
    }

    public void setMicrophoneLocked(boolean microphoneLocked) {
        this.microphoneLocked = microphoneLocked;
        activating = false;
        wasPTT = false;
        deactivationDelay = 0;
        lastBuff = null;
    }

    public void close() {
        running = false;
        mic.stop();
        mic.flush();
        mic.close();
        encoder.close();
    }

}
