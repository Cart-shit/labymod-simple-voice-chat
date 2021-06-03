package de.foorcee.labymod.voicechat.client.udp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.serialization.ClassResolver;

import java.io.*;
import java.util.List;

@Sharable
public class DatagramDecoder extends MessageToMessageDecoder<DatagramPacket> {

    private final ClassResolver resolver;

    public DatagramDecoder(ClassResolver resolver) {
        this.resolver = resolver;
    }

    public boolean acceptInboundMessage(Object msg) {
        return msg instanceof DatagramPacket;
    }

    protected void decode(ChannelHandlerContext ctx, DatagramPacket packet, List<Object> out) throws IOException, ClassNotFoundException {
        ByteBuf buf = packet.content();
        buf.retain();
        buf.skipBytes(4);
        Object msg = readObject(buf);
        if (msg == null) {
            return;
        }
        out.add(new DefaultAddressedEnvelope(msg, packet.recipient(), packet.sender()));
    }

    private Object readObject(ByteBuf frame) throws IOException, ClassNotFoundException {
        try (CompactObjectInputStream ois = new CompactObjectInputStream(new ByteBufInputStream(frame), this.resolver)) {
            return ois.readObject();
        }
    }

    private static class CompactObjectInputStream extends ObjectInputStream {
        private final ClassResolver classResolver;

        CompactObjectInputStream(InputStream in, ClassResolver classResolver) throws IOException {
            super(in);
            this.classResolver = classResolver;
        }

        protected void readStreamHeader() throws IOException {
            int version = readByte() & 0xFF;
            if (version != 5)
                throw new StreamCorruptedException("Unsupported version: " + version);
        }

        protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
            String className;
            Class<?> clazz;
            int type = read();
            if (type < 0)
                throw new EOFException();
            switch (type) {
                case 0:
                    return super.readClassDescriptor();
                case 1:
                    className = readUTF();
                    clazz = this.classResolver.resolve(className);
                    return ObjectStreamClass.lookupAny(clazz);
            }
            throw new StreamCorruptedException("Unexpected class descriptor type: " + type);
        }

        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            Class<?> clazz;
            try {
                clazz = this.classResolver.resolve(desc.getName());
            } catch (ClassNotFoundException var4) {
                clazz = super.resolveClass(desc);
            }
            return clazz;
        }
    }
}
