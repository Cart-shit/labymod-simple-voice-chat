package net.labymod.voicechat.protocol.packet.login;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class EncryptionRequest implements Serializable {
    private String serverId;
    private byte[] publicKey;
    private byte[] verifyToken;
}
