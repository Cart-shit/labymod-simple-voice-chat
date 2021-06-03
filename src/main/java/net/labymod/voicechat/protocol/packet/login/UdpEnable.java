package net.labymod.voicechat.protocol.packet.login;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class UdpEnable implements Serializable {
    private int secret;
    private int port;
}
