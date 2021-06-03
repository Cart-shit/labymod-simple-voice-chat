package de.maxhenkel.voicechat.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.foorcee.labymod.voicechat.client.LabymodVoicechatClient;
import de.maxhenkel.voicechat.VoicechatClient;
import de.maxhenkel.voicechat.gui.widgets.*;
import de.maxhenkel.voicechat.voice.common.Utils;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

public class VoiceChatSettingsScreen extends VoiceChatScreenBase implements MicTestButton.MicListener {

    private static final ResourceLocation TEXTURE = new ResourceLocation(VoicechatClient.MODID, "textures/gui/gui_voicechat_settings.png");

    private double micValue;

    private VoiceActivationSlider voiceActivationSlider;

    public VoiceChatSettingsScreen() {
        super(new TranslatableComponent("gui.voicechat.voice_chat_settings.title"), 248, 226);
    }

    @Override
    protected void init() {
        super.init();

        voiceActivationSlider = new VoiceActivationSlider(guiLeft + 10, guiTop + 95, xSize - 20, 20);
        addRenderableWidget(new VoiceSoundSlider(guiLeft + 10, guiTop + 20, xSize - 20, 20));
        addRenderableWidget(new MicAmplificationSlider(guiLeft + 10, guiTop + 45, xSize - 20, 20));
        addRenderableWidget(new MicActivationButton(guiLeft + 10, guiTop + 70, xSize - 20, 20, voiceActivationSlider));
        addRenderableWidget(voiceActivationSlider);
        LabymodVoicechatClient c = VoicechatClient.CLIENT.getClient();
        if (c != null) {
            addRenderableWidget(new MicTestButton(guiLeft + 10, guiTop + 145, xSize - 20, 20, this, c));
        }
        addRenderableWidget(new Button(guiLeft + 10, guiTop + 170, xSize - 20, 20, new TranslatableComponent("message.voicechat.adjust_volumes"), button -> {
            minecraft.setScreen(new SpeakerListScreen());
//            minecraft.setScreen(new AdjustVolumeScreen(this, VoicechatClient.CLIENT.getPlayerStateManager().getPlayerStates().stream().filter(state -> !state.getGameProfile().getId().equals(minecraft.player.getUUID())).collect(Collectors.toList())));
        }));
        addRenderableWidget(new Button(guiLeft + 10, guiTop + 195, xSize / 2 - 15, 20, new TranslatableComponent("message.voicechat.select_microphone"), button -> {
            minecraft.setScreen(new SelectMicrophoneScreen(this));
        }));
        addRenderableWidget(new Button(guiLeft + xSize / 2 + 6, guiTop + 195, xSize / 2 - 15, 20, new TranslatableComponent("message.voicechat.select_speaker"), button -> {
            minecraft.setScreen(new SelectSpeakerScreen(this));
        }));
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        blit(matrixStack, guiLeft, guiTop, 0, 0, xSize, ySize, 512, 512);

        blit(matrixStack, guiLeft + 10, guiTop + 120, 0, 244, xSize - 20, 20, 512, 512);
        blit(matrixStack, guiLeft + 11, guiTop + 121, 0, 226, (int) ((xSize - 18) * micValue), 18, 512, 512);

        int pos = (int) ((xSize - 20) * Utils.dbToPerc(VoicechatClient.CLIENT_CONFIG.voiceActivationThreshold.get()));

        blit(matrixStack, guiLeft + 10 + pos, guiTop + 120, 0, 244, 1, 20, 512, 512);

        super.render(matrixStack, mouseX, mouseY, partialTicks);

        Component title = new TranslatableComponent("gui.voicechat.voice_chat_settings.title");
        int titleWidth = font.width(title);
        font.draw(matrixStack, title.getVisualOrderText(), (float) (guiLeft + (xSize - titleWidth) / 2), guiTop + 7, FONT_COLOR);
    }

    @Override
    public void onMicValue(double perc) {
        this.micValue = perc;
    }
}
