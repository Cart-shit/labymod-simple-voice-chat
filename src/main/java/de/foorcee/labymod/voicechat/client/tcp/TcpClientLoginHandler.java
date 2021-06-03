package de.foorcee.labymod.voicechat.client.tcp;

import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import de.foorcee.labymod.voicechat.client.LabymodVoicechatClient;
import de.foorcee.labymod.voicechat.client.auth.AuthenticationMode;
import de.foorcee.labymod.voicechat.client.encryption.EncryptionUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.log4j.Log4j2;
import net.labymod.voicechat.protocol.packet.login.*;
import net.minecraft.client.Minecraft;

import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public class TcpClientLoginHandler extends ChannelInboundHandlerAdapter {

    private static final Gson GSON = new Gson();

    private final LabymodVoicechatClient client;
    private final AtomicInteger state = new AtomicInteger(-1);

    public TcpClientLoginHandler(LabymodVoicechatClient client) {
        this.client = client;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        validateState(-1);
        ctx.writeAndFlush(new Handshake(System.currentTimeMillis(), 4));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info(state.get() + " " + msg.getClass().getName());
        if (msg instanceof Handshake) {
            validateState(0);
            GameProfile profile = Minecraft.getInstance().getUser().getGameProfile();
            ctx.writeAndFlush(new LoginRequest(profile.getId(), profile.getName()));
        } else if (msg instanceof EncryptionRequest) {
            validateState(1);
            EncryptionRequest request = (EncryptionRequest) msg;

            SecretKey secretKey = client.getEncryption().generateKey();
            PublicKey publicKey = client.getEncryption().decodePublicKey(request.getPublicKey());
            String serverId = request.getServerId();

            String hash = new BigInteger(client.getEncryption().getServerIdHash(serverId, publicKey, secretKey)).toString(16);


            this.client.getAuthenticationHandler().handleAuthentication(hash, result -> {
                log.info("Send Encryption response " + result.getMode());
                try {
                    if (result.getMode() == AuthenticationMode.LABYCONNECT) {
                        ctx.channel().writeAndFlush(new EncryptionResponseWithPin(EncryptionUtil.encryptData(publicKey, secretKey.getEncoded()),
                                EncryptionUtil.encryptData(publicKey, request.getVerifyToken()), EncryptionUtil.encryptData(publicKey, result.getPin().getBytes())));
                    } else if (result.getMode() == AuthenticationMode.MOJANG) {
                        ctx.writeAndFlush(new EncryptionResponse(EncryptionUtil.encryptData(publicKey, secretKey.getEncoded()),
                                EncryptionUtil.encryptData(publicKey, request.getVerifyToken())));
                    }
                } catch (GeneralSecurityException e) {
                    e.printStackTrace();
                }
            });
        } else if (msg instanceof UdpEnable) {
            validateState(2);
            UdpEnable enable = (UdpEnable) msg;
            client.connectToUdp(() -> {
                Channel udp = client.getUdpClient();
                udp.writeAndFlush(new UdpEnable(enable.getSecret(), ((InetSocketAddress) udp.localAddress()).getPort()));
            });
        } else if (msg instanceof LoginResponse) {
            validateState(3);
            LoginResponse response = (LoginResponse) msg;

            ctx.pipeline().replace(this, "handle", this.client.getTcpClientPlayHandler());
            this.client.connected(response.isYouAreAdmin());
        } else if (msg instanceof KickRequest) {
            KickRequest request = (KickRequest) msg;
            client.kick(request.getReason());
        }
    }


    private void validateState(int state) {
        if (this.state.getAndIncrement() != state) {
            throw new IllegalStateException("Expected Protocolstate " + state + " actual " + (this.state.get() - 1));
        }
    }
}
