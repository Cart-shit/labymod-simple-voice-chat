package de.maxhenkel.voicechat.gui.widgets;

import de.maxhenkel.voicechat.VoicechatClient;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

public class SoundDistanceSlider extends AbstractSliderButton {

    private static final double MIN_VALUE = 1D;
    private static final double MAX_VALUE = 64D;
    private static final float STEP = 1f;

    public SoundDistanceSlider(int x, int y, int width, int height) {
        super(x, y, width, height, TextComponent.EMPTY, SliderUtils.toPct(VoicechatClient.CLIENT_CONFIG.voiceChatDistance.get(), MIN_VALUE, MAX_VALUE, STEP));
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(getMsg());
    }

    public Component getMsg() {
        return new TranslatableComponent("message.voicechat.voice_surround_distance", SliderUtils.toValue(value, MIN_VALUE, MAX_VALUE, STEP));
    }

    @Override
    protected void applyValue() {
        double realValue = SliderUtils.toValue(value, MIN_VALUE, MAX_VALUE, STEP);
        VoicechatClient.CLIENT_CONFIG.voiceChatDistance.set(realValue);
        VoicechatClient.CLIENT_CONFIG.voiceChatDistance.save();

        if (VoicechatClient.CLIENT_CONFIG.voiceChatFadeDistance.get() > realValue) {
            VoicechatClient.CLIENT_CONFIG.voiceChatFadeDistance.set(realValue);
            VoicechatClient.CLIENT_CONFIG.voiceChatFadeDistance.save();
        }
    }
}
