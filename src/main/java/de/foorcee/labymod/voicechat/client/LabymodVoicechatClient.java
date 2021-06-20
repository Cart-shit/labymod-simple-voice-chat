package de.foorcee.labymod.voicechat.client;

import de.foorcee.labymod.voicechat.client.auth.AuthenticationHandler;
import de.foorcee.labymod.voicechat.client.encryption.ClientEncryption;
import de.foorcee.labymod.voicechat.client.tcp.TcpClientLoginHandler;
import de.foorcee.labymod.voicechat.client.tcp.TcpClientPlayHandler;
import de.foorcee.labymod.voicechat.client.tcp.TcpConnectionHandler;
import de.foorcee.labymod.voicechat.client.udp.DatagramDecoder;
import de.maxhenkel.voicechat.VoicechatClient;
import de.maxhenkel.voicechat.events.ClientVoiceChatEvents;
import de.maxhenkel.voicechat.voice.client.AudioChannel;
import de.maxhenkel.voicechat.voice.client.AudioChannelConfig;
import de.maxhenkel.voicechat.voice.client.MicThread;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.labymod.voicechat.protocol.packet.audio.KeepAlive;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Getter
@Log4j2
public class LabymodVoicechatClient {

    private static final Timer TIMER = new Timer();
    private static final KeepAlive KEEP_ALIVE = new KeepAlive(new byte[256]);
    private static final boolean EPOLL = false;

    private final VoicechatClient client;
    private final EventLoopGroup group;
    private final ObjectEncoder objectEncoder;
    private final ClientEncryption encryption;
    private final AuthenticationHandler authenticationHandler;

    private final TcpClientPlayHandler tcpClientPlayHandler;
    private final AudioChannelConfig audioChannelConfig;

    private MicThread micThread;

    private ConnectionState state = ConnectionState.DISCONNECTED;

    private final int maxPacketSize = 38280;

    private Channel tcpClient;
    private Channel udpClient;

    public LabymodVoicechatClient(VoicechatClient client) {
        this.client = client;
        this.group = EPOLL ? new EpollEventLoopGroup(1) : new NioEventLoopGroup(1);
        this.objectEncoder = new ObjectEncoder();
        this.encryption = new ClientEncryption();
        this.authenticationHandler = new AuthenticationHandler();
        this.tcpClientPlayHandler = new TcpClientPlayHandler(this);
        this.audioChannelConfig = new AudioChannelConfig(this);

        this.group.scheduleAtFixedRate(() -> {
            sendPacket(KEEP_ALIVE);
            sendUdpPacket(KEEP_ALIVE);
        }, 60L, 60L, TimeUnit.SECONDS);
    }

    public void connect() {
        if (!state.isDisconnected()) return;
        this.state = ConnectionState.CONNECTING;
        Bootstrap tcpBoot = new Bootstrap();
        tcpBoot.group(this.group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast("encode", objectEncoder)
                                .addLast("decode", new ObjectDecoder(ClassResolvers.softCachingResolver(getClass().getClassLoader())))
                                .addLast("connection", new TcpConnectionHandler(LabymodVoicechatClient.this))
                                .addLast("handle", new TcpClientLoginHandler(LabymodVoicechatClient.this));
                    }
                });
        ChannelFuture future = tcpBoot.connect(serverAddress());
        future.addListener(action -> {
            if (action.isSuccess()) {
                this.state = ConnectionState.LOGIN;
                this.tcpClient = future.channel();
                if (!this.tcpClient.isActive()) {
                    disconnect();
                }
            } else {
                action.cause().printStackTrace();
                disconnect();
            }
        });
    }

    public void connectToUdp(Runnable runnable) {
        Bootstrap udpBoot = new Bootstrap();
        if (EPOLL) log.info("Epoll is available ...");
        Class<? extends Channel> channelClass = EPOLL ? EpollDatagramChannel.class : NioDatagramChannel.class;
        log.info("Using channel " + channelClass.getName());
        udpBoot.group(this.group)
                .channel(channelClass)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.IP_TOS, 0x10 | 0x08);

        if (EPOLL) {
            udpBoot.option(EpollChannelOption.SO_REUSEPORT, true);
        }

        udpBoot.handler(new ChannelInitializer<DatagramChannel>() {
            protected void initChannel(DatagramChannel datagramChannel) {
                datagramChannel.pipeline()
                        .addLast("datagram_in", new DatagramDecoder(ClassResolvers.softCachingResolver(getClass().getClassLoader())))
                        .addLast("encode", LabymodVoicechatClient.this.objectEncoder)
                        .addLast("handle", LabymodVoicechatClient.this.tcpClientPlayHandler);
            }
        });
        ChannelFuture future = udpBoot.connect(this.tcpClient.remoteAddress(), this.tcpClient.localAddress());
        future.addListener(action -> {
            if (action.isSuccess()) {
                this.udpClient = future.channel();
                if (!this.udpClient.isActive()) {
                    disconnect();
                    throw new RuntimeException("Failed to connect to udp voiceserver, failed to connect");
                }

                runnable.run();
            } else {
                action.cause().printStackTrace();
                disconnect();
            }
        });
    }

    public boolean isConnected() {
        return getState().isConnected();
    }

    public void connected(boolean youAreAdmin) {
        updateState(ConnectionState.ESTABLISHED);
        ClientVoiceChatEvents.VOICECHAT_CONNECTED.invoker().accept(client);
        startMicThread();
    }

    public void updateState(ConnectionState state) {
        this.state = state;
        log.info("Switched to state " + state.name());
    }

    public void kick(String reason) {
        disconnect();
        updateState(ConnectionState.KICKED);
        log.info("Kick from VoiceChat for " + reason);
    }

    public void disconnect() {
        updateState(ConnectionState.DISCONNECTED);
        if (this.tcpClient != null && this.tcpClient.isActive()) {
            this.tcpClient.close();
        }

        if (this.udpClient != null && this.udpClient.isActive()) {
            this.udpClient.close();
        }

        try {
            if (micThread != null) {
                VoicechatClient.LOGGER.debug("Closing microphone thread");
                micThread.close();
                micThread = null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void reconnect() {
        log.info("try reconnect in 30 seconds");
        TIMER.schedule(new TimerTask() {
            @Override
            public void run() {
                log.info("reconnecting ...");
                connect();
            }
        }, TimeUnit.SECONDS.toMillis(30));
    }

    public void sendPacket(Object obj) {
        if (!this.state.isConnected() || !tcpClient.isActive()) return;
        this.tcpClient.writeAndFlush(obj);
    }

    public void sendUdpPacket(Object obj) {
        if (!this.state.isConnected() || !this.udpClient.isActive()) return;
        this.udpClient.writeAndFlush(obj);
    }

    public InetSocketAddress serverAddress() {
        return new InetSocketAddress("voice.labymod.net", false ? 9943 : 9942);
    }

    private void startMicThread() {
        try {
            micThread = new MicThread(this);
            micThread.start();
        } catch (Exception e) {
            VoicechatClient.LOGGER.error("Mic unavailable " + e);
            e.printStackTrace();
        }
    }

    public void checkTimeout() {
//        if (lastKeepAlive >= 0 && System.currentTimeMillis() - lastKeepAlive > keepAlive * 10L) {
//            VoicechatClient.LOGGER.info("Connection timeout");
//            VoicechatClient.CLIENT.onDisconnect();
//        }
    }

    public void reloadDataLines() {
        VoicechatClient.LOGGER.debug("Reloading data lines");
        if (micThread != null) {
            VoicechatClient.LOGGER.debug("Restarting microphone thread");
            micThread.close();
            micThread = null;
        }
        startMicThread();
        VoicechatClient.LOGGER.debug("Clearing audio channels");
        Map<UUID, AudioChannel> audioChannels = tcpClientPlayHandler.getAudioChannels();
        audioChannels.forEach((uuid, audioChannel) -> audioChannel.closeAndKill());
        audioChannels.clear();
    }
}
