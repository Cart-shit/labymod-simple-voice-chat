package net.labymod.voicechat.protocol.packet.audio;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class AudioChunkServer implements Serializable {
    private UUID uuid;
    private byte[] bytes;
}
