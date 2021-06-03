package net.labymod.voicechat.protocol.packet.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class AdminMuted implements Serializable {
    private UUID uuid;
    private boolean muted;
    private long secondsLeft;
    private String reason;
    private String mutedBy;
}
