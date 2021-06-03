package net.labymod.voicechat.protocol.packet.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class AdminMutePlayer implements Serializable {
    private UUID uuid;
    boolean mute;
    private String reason;
    private int hours;
}
