package net.labymod.voicechat.protocol.packet.login;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class LoginRequest implements Serializable {
    private UUID uuid;
    private String name;
}
