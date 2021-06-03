package de.maxhenkel.voicechat;

import com.mojang.blaze3d.platform.InputConstants;
import de.foorcee.labymod.voicechat.client.DevAuthUtil;
import de.maxhenkel.voicechat.config.ClientConfig;
import de.maxhenkel.voicechat.config.ConfigBuilder;
import de.maxhenkel.voicechat.config.MutedPlayerConfig;
import de.maxhenkel.voicechat.config.PlayerVolumeConfig;
import de.maxhenkel.voicechat.events.ClientTickVisitListener;
import de.maxhenkel.voicechat.voice.client.ClientVoiceEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class VoicechatClient implements ClientModInitializer {

    public static final String MODID = "voicechat";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static KeyMapping KEY_PTT;
    public static KeyMapping KEY_MUTE;
    public static KeyMapping KEY_DISABLE;
    public static KeyMapping KEY_HIDE_ICONS;
    public static KeyMapping KEY_VOICE_CHAT;
    public static KeyMapping KEY_VOICE_CHAT_SETTINGS;
    public static KeyMapping KEY_GROUP;

    public static ClientVoiceEvents CLIENT;
    public static ClientConfig CLIENT_CONFIG;
    public static PlayerVolumeConfig VOLUME_CONFIG;
    public static MutedPlayerConfig MUTED_PLAYER;

    @Override
    public void onInitializeClient() {
        DevAuthUtil.auth();

        ConfigBuilder.create(Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(MODID).resolve("voicechat-client.properties"), builder -> CLIENT_CONFIG = new ClientConfig(builder));
        VOLUME_CONFIG = new PlayerVolumeConfig(Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("voicechat-volumes.properties"));
        MUTED_PLAYER = new MutedPlayerConfig(Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(MODID).resolve("voicechat-mutes.properties"));

        KEY_PTT = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.push_to_talk", GLFW.GLFW_KEY_CAPS_LOCK, "key.categories.voicechat"));
        KEY_MUTE = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.mute_microphone", GLFW.GLFW_KEY_M, "key.categories.voicechat"));
        KEY_DISABLE = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.disable_voice_chat", GLFW.GLFW_KEY_N, "key.categories.voicechat"));
        KEY_HIDE_ICONS = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.hide_icons", GLFW.GLFW_KEY_H, "key.categories.voicechat"));
        KEY_VOICE_CHAT = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.voice_chat", GLFW.GLFW_KEY_V, "key.categories.voicechat"));
        KEY_VOICE_CHAT_SETTINGS = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.voice_chat_settings", InputConstants.UNKNOWN.getValue(), "key.categories.voicechat"));
        KEY_GROUP = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.voice_chat_group", InputConstants.UNKNOWN.getValue(), "key.categories.voicechat"));

        CLIENT = new ClientVoiceEvents(this);

        new ClientTickVisitListener();
    }
}
