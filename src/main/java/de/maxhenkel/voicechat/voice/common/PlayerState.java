package de.maxhenkel.voicechat.voice.common;

import com.mojang.authlib.GameProfile;

public class PlayerState {

    private boolean disabled;
    private boolean disconnected;
    private GameProfile gameProfile;

    public PlayerState(boolean disabled, boolean disconnected, GameProfile gameProfile) {
        this.disabled = disabled;
        this.disconnected = disconnected;
        this.gameProfile = gameProfile;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isDisconnected() {
        return disconnected;
    }

    public void setDisconnected(boolean disconnected) {
        this.disconnected = disconnected;
    }

    public GameProfile getGameProfile() {
        return gameProfile;
    }

    public void setGameProfile(GameProfile gameProfile) {
        this.gameProfile = gameProfile;
    }

}
