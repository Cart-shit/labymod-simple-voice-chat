package net.labymod.voicechat.protocol.packet.login;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Handshake implements Serializable {
    private long time;
    private int version;
}
