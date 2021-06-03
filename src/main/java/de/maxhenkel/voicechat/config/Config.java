package de.maxhenkel.voicechat.config;


import de.maxhenkel.voicechat.VoicechatClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Config {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

    protected Properties properties;
    protected Path path;

    public Config(Path path) {
        this.path = path;
        this.properties = new Properties();
        try {
            load();
        } catch (IOException e) {
            VoicechatClient.LOGGER.error("Failed to read " + path.getFileName().toString(), e);
            VoicechatClient.LOGGER.warn("Using default configuration values");
        }
    }

    public String get(String key) {
        return properties.getProperty(key);
    }

    public void set(String key, String value) {
        properties.setProperty(key, value);
    }

    public Properties getProperties() {
        return properties;
    }

    public void load() throws IOException {
        File file = path.toFile();
        if (file.exists()) {
            properties.load(new FileInputStream(file));
        }
    }

    private void saveUnthreaded() {
        try {
            File file = path.toFile();
            file.getParentFile().mkdirs();
            properties.store(new FileWriter(file, false), "");
        } catch (IOException e) {
            VoicechatClient.LOGGER.error("Failed to save " + path.getFileName().toString(), e);
        }
    }

    public void save() {
        EXECUTOR_SERVICE.execute(() -> {
            synchronized (this) {
                saveUnthreaded();
            }
        });
    }

}
