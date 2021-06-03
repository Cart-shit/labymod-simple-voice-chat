package de.foorcee.labymod.voicechat.client;

public enum ConnectionState {
    DISCONNECTED,
    CONNECTING,
    LOGIN,
    ESTABLISHED,
    KICKED;

    public boolean isConnected() {
        return this == ESTABLISHED;
    }

    public boolean isDisconnected() {
        return this == DISCONNECTED || this == KICKED;
    }
}
