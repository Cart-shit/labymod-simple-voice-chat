package net.labymod.voicechat.protocol.packet.visit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Alive implements Serializable {
    private UUID uuid;
}
