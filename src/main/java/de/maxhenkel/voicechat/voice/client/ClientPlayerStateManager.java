package de.maxhenkel.voicechat.voice.client;

import de.maxhenkel.voicechat.VoicechatClient;
import de.maxhenkel.voicechat.events.ClientVoiceChatEvents;
import de.maxhenkel.voicechat.events.ClientWorldEvents;
import de.maxhenkel.voicechat.voice.common.PlayerState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class ClientPlayerStateManager {

    private boolean muted;
    private final PlayerState state;
    private final Map<UUID, PlayerState> states;

    public ClientPlayerStateManager() {
        muted = VoicechatClient.CLIENT_CONFIG.muted.get();
        state = new PlayerState(VoicechatClient.CLIENT_CONFIG.disabled.get(), true, Minecraft.getInstance().getUser().getGameProfile());
        states = new HashMap<>();
        ClientVoiceChatEvents.VOICECHAT_CONNECTED.register(this::onVoiceChatConnected);
        ClientVoiceChatEvents.VOICECHAT_DISCONNECTED.register(this::onVoiceChatDisconnected);
        ClientWorldEvents.DISCONNECT.register(this::onDisconnect);
    }

    /**
     * Called when the voicechat client gets disconnected or the player logs out
     */
    public void onVoiceChatDisconnected() {
        state.setDisconnected(true);
    }

    /**
     * Called when the voicechat client gets (re)connected
     */
    public void onVoiceChatConnected(VoicechatClient client) {
        state.setDisconnected(false);
    }

    public void registerPlayer(UUID player) {
        if (Minecraft.getInstance().level == null) return;
        Player p = Minecraft.getInstance().level.getPlayerByUUID(player);
        if (p == null) return;
        boolean muted = VoicechatClient.MUTED_PLAYER.isPlayerMuted(player);
        PlayerState playerState = new PlayerState(muted, false, p.getGameProfile());
        states.put(player, playerState);
    }

    private void onDisconnect() {
        clearStates();
    }

    public boolean isPlayerDisabled(Player player) {
        PlayerState playerState = states.get(player.getUUID());
        if (playerState == null) {
            return false;
        }

        return playerState.isDisabled();
    }

    public boolean isPlayerDisconnected(Player player) {
        PlayerState playerState = states.get(player.getUUID());
        if (playerState == null) {
            return true;
        }

        return playerState.isDisconnected();
    }

    public boolean isDisabled() {
        return state.isDisabled();
    }

    public boolean isDisconnected() {
        return state.isDisconnected();
    }

    public void setDisabled(boolean disabled) {
        state.setDisabled(disabled);
        VoicechatClient.CLIENT_CONFIG.disabled.set(disabled);
        VoicechatClient.CLIENT_CONFIG.disabled.save();
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        VoicechatClient.CLIENT_CONFIG.muted.set(muted);
        VoicechatClient.CLIENT_CONFIG.muted.save();
    }

    public List<PlayerState> getPlayerStates() {
        return new ArrayList<>(states.values());
    }

    public List<PlayerState> getPlayerStatesByName(String name) {
        String lowerCase = name.toLowerCase(Locale.ROOT);
        return states.values().stream()
                .filter(playerState -> playerState.getGameProfile().getName().toLowerCase(Locale.ROOT).startsWith(lowerCase))
                .sorted(Comparator.comparing(playerState -> playerState.getGameProfile().getName()))
                .collect(Collectors.toList());
    }

    @Nullable
    public PlayerState getState(UUID player) {
        return states.get(player);
    }

    public void clearStates() {
        states.clear();
    }
}
