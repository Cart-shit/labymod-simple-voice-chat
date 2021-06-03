package de.maxhenkel.voicechat.gui.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import de.maxhenkel.voicechat.VoicechatClient;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

public class SoundDistanceFadeSlider extends AbstractSliderButton {

    private static final double MIN_VALUE = 1D;
    private static final float STEP = 1f;

    public SoundDistanceFadeSlider(int x, int y, int width, int height) {
        super(x, y, width, height, TextComponent.EMPTY,
                SliderUtils.toPct(VoicechatClient.CLIENT_CONFIG.voiceChatFadeDistance.get(), MIN_VALUE,
                        VoicechatClient.CLIENT_CONFIG.voiceChatDistance.get(), STEP));
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(getMsg());
    }

    public Component getMsg() {
        return new TranslatableComponent("message.voicechat.voice_surround_distance_fade", SliderUtils.toValue(value, MIN_VALUE, VoicechatClient.CLIENT_CONFIG.voiceChatDistance.get(), STEP));
    }

    @Override
    protected void applyValue() {
        double realValue = SliderUtils.toValue(value, MIN_VALUE, VoicechatClient.CLIENT_CONFIG.voiceChatDistance.get(), STEP);
        VoicechatClient.CLIENT_CONFIG.voiceChatFadeDistance.set(realValue);
        VoicechatClient.CLIENT_CONFIG.voiceChatFadeDistance.save();
    }
}
