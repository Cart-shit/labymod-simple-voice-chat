package de.foorcee.labymod.voicechat.client.tcp;

import de.foorcee.labymod.voicechat.client.LabymodVoicechatClient;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class TcpConnectionHandler extends ChannelInboundHandlerAdapter {

    private final LabymodVoicechatClient client;

    public TcpConnectionHandler(LabymodVoicechatClient client) {
        this.client = client;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("Lost connection to voice server");
        client.disconnect();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.warn("Exception caught in voice server connection: " + cause.getMessage(), cause);
    }
}
