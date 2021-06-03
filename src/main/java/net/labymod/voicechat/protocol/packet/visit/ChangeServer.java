package net.labymod.voicechat.protocol.packet.visit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ChangeServer implements Serializable {
    private String server;
    private int port;
}
