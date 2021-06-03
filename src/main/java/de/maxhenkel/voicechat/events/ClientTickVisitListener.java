package de.maxhenkel.voicechat.events;

import de.foorcee.labymod.voicechat.client.LabymodVoicechatClient;
import de.maxhenkel.voicechat.VoicechatClient;
import lombok.extern.log4j.Log4j2;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.labymod.voicechat.protocol.packet.visit.Visit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Log4j2
public class ClientTickVisitListener {

    private int lastPlayersCount = 0;
    private int prevUUIDArrayHash = 0;

    public ClientTickVisitListener() {
        registerEvents();
    }

    private void registerEvents() {
        ClientTickEvents.START_CLIENT_TICK.register(c -> {
            try {
                LabymodVoicechatClient client = VoicechatClient.CLIENT.getClient();
                if (client == null || !isInGame() || !client.getState().isConnected()) {
                    this.lastPlayersCount = 0;
                    this.prevUUIDArrayHash = 0;
                    return;
                }

                List<AbstractClientPlayer> players = Minecraft.getInstance().level.players();
                if (this.lastPlayersCount == players.size()) {
                    return;
                }

                this.lastPlayersCount = players.size();

                UUID[] uuids = new UUID[players.size()];

                int uuidHash = 1;
                for(int index = 0; index < uuids.length; index++) {
                    AbstractClientPlayer player = players.get(index);
                    if (player == null) {
                        return;
                    }

                    UUID uuid = uuids[index] = player.getUUID();
                    //Arrays.hashCode
                    uuidHash = 31 * uuidHash + (uuid == null ? 0 : uuid.hashCode());
                }

                if (uuidHash == this.prevUUIDArrayHash) {
                    return;
                }

                this.prevUUIDArrayHash = uuidHash;
                client.sendPacket(new Visit(uuids));
                log.info("Send UUIDs " + uuids.length + " " + Arrays.toString(uuids));
            } catch (Exception var7) {
                var7.printStackTrace();
            }
        });
    }

    public boolean isInGame() {
        return Minecraft.getInstance().player != null && Minecraft.getInstance().level != null;
    }
}
