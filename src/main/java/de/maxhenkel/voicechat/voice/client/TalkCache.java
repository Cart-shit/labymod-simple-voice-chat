package de.maxhenkel.voicechat.voice.client;

import de.foorcee.labymod.voicechat.client.LabymodVoicechatClient;
import de.maxhenkel.voicechat.VoicechatClient;
import de.maxhenkel.voicechat.events.ClientWorldEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TalkCache {

    private static final long TIMEOUT = 500L;

    private final Map<UUID, Long> cache = new ConcurrentHashMap<>();

    public TalkCache() {
        ClientWorldEvents.DISCONNECT.register(cache::clear);
    }

    public void updateTalking(UUID player) {
        cache.put(player, System.currentTimeMillis());
    }

    public boolean isTalking(Player player) {
        return isTalking(player.getUUID());
    }

    public boolean isTalking(UUID player) {
        if (player.equals(Minecraft.getInstance().player.getUUID())) {
            LabymodVoicechatClient client = VoicechatClient.CLIENT.getClient();
            if (client != null && client.getMicThread() != null) {
                return client.getMicThread().isTalking();
            }
        }

        Long lastTalk = cache.getOrDefault(player, 0L);
        return System.currentTimeMillis() - lastTalk < TIMEOUT;
    }

    public List<UUID> getTalkingPlayers(long val, TimeUnit timeUnit) {
        long delay = timeUnit.toMillis(val);
        return cache.entrySet().stream().filter(entry -> System.currentTimeMillis() - entry.getValue() < delay)
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }

}
