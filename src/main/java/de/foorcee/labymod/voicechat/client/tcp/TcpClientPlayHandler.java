package de.foorcee.labymod.voicechat.client.tcp;

import de.foorcee.labymod.voicechat.client.LabymodVoicechatClient;
import de.maxhenkel.voicechat.VoicechatClient;
import de.maxhenkel.voicechat.voice.client.AudioChannel;
import de.maxhenkel.voicechat.voice.common.PlayerState;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.labymod.voicechat.protocol.packet.audio.AudioChunkServer;
import net.labymod.voicechat.protocol.packet.login.KickRequest;
import net.labymod.voicechat.protocol.packet.visit.Alive;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@ChannelHandler.Sharable
@Getter
@Log4j2
public class TcpClientPlayHandler extends ChannelInboundHandlerAdapter {

    private final LabymodVoicechatClient client;
    private final Map<UUID, AudioChannel> audioChannels = new HashMap<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof KickRequest kick) {
            client.kick(kick.getReason());
        } else if (msg instanceof AudioChunkServer audioChunkServer) {
            PlayerState state = VoicechatClient.CLIENT.getPlayerStateManager().getState(audioChunkServer.getUuid());
            if(state != null && state.isDisabled()) return;

            AudioChannel sendTo = audioChannels.get(audioChunkServer.getUuid());
            if (sendTo == null) {
                AudioChannel ch = new AudioChannel(this.client, audioChunkServer.getUuid());
                ch.addToQueue(audioChunkServer);
                ch.start();
                audioChannels.put(audioChunkServer.getUuid(), ch);
            } else {
                sendTo.addToQueue(audioChunkServer);
            }

            audioChannels.values().stream().filter(AudioChannel::canKill).forEach(AudioChannel::closeAndKill);
            audioChannels.entrySet().removeIf(entry -> entry.getValue().isClosed());
        } else if (msg instanceof Alive alive) {
            VoicechatClient.CLIENT.getPlayerStateManager().registerPlayer(alive.getUuid());
            log.info("Received Alive for " + alive.getUuid());
        } else if (msg instanceof AddressedEnvelope addressedEnvelope) {
            channelRead(ctx, addressedEnvelope.content());
        }
    }
}
