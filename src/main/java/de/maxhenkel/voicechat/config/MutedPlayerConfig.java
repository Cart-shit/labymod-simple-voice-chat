package de.maxhenkel.voicechat.config;

import java.nio.file.Path;
import java.util.UUID;

public class MutedPlayerConfig extends Config {

    private static final String VALUE = "1b";

    public MutedPlayerConfig(Path path) {
        super(path);
    }

    public boolean isPlayerMuted(UUID uuid) {
        String property = properties.getProperty(uuid.toString());
        return property != null && property.equals(VALUE);
    }

    public void setPlayerMuted(UUID uuid, boolean mute) {
        if (mute) {
            properties.setProperty(uuid.toString(), VALUE);
        } else {
            properties.remove(uuid.toString());
        }
    }
}
