package net.labymod.voicechat.protocol.packet.login;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class EncryptionResponseWithPin extends EncryptionResponse implements Serializable {
    private byte[] pin;

    public EncryptionResponseWithPin(byte[] sharedSecret, byte[] verifyToken, byte[] pin) {
        super(sharedSecret, verifyToken);
        this.pin = pin;
    }

    public EncryptionResponseWithPin() {
    }
}
