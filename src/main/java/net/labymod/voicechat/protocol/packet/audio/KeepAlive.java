package net.labymod.voicechat.protocol.packet.audio;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class KeepAlive implements Serializable {
    private byte[] data;
}
