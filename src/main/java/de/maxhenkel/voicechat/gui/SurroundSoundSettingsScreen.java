package de.maxhenkel.voicechat.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.maxhenkel.voicechat.VoicechatClient;
import de.maxhenkel.voicechat.gui.widgets.SoundDistanceFadeSlider;
import de.maxhenkel.voicechat.gui.widgets.SoundDistanceSlider;
import de.maxhenkel.voicechat.gui.widgets.ToggleImageButton;
import de.maxhenkel.voicechat.voice.client.ClientPlayerStateManager;
import de.maxhenkel.voicechat.voice.client.MicrophoneActivationType;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;

public class SurroundSoundSettingsScreen extends VoiceChatScreenBase {

    private static final ResourceLocation TEXTURE = new ResourceLocation(VoicechatClient.MODID, "textures/gui/gui_voicechat.png");

    public SurroundSoundSettingsScreen() {
        super(new TranslatableComponent("gui.voicechat.voice_chat.title"), 195, 76);
    }

    @Override
    protected void init() {
        super.init();

        addRenderableWidget(new SoundDistanceSlider(guiLeft + 10, guiTop + 20, xSize - 20, 20));
        addRenderableWidget(new SoundDistanceFadeSlider(guiLeft + 10, guiTop + 45, xSize - 20, 20));
    }

    @Override
    public void tick() {
        super.tick();
    }


    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        blit(matrixStack, guiLeft, guiTop, 0, 0, xSize, ySize);

        super.render(matrixStack, mouseX, mouseY, partialTicks);

        Component title = new TranslatableComponent("gui.voicechat.voice_chat.title");
        int titleWidth = font.width(title);
        font.draw(matrixStack, title.getVisualOrderText(), (float) (guiLeft + (xSize - titleWidth) / 2), guiTop + 7, FONT_COLOR);
    }

}
