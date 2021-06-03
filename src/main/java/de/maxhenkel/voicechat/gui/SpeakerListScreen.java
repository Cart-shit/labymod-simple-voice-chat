package de.maxhenkel.voicechat.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.maxhenkel.voicechat.VoicechatClient;
import de.maxhenkel.voicechat.gui.widgets.PlayerList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

public class SpeakerListScreen extends VoiceChatScreenBase {

    private static final ResourceLocation TEXTURE = new ResourceLocation(VoicechatClient.MODID, "textures/gui/gui_create_group.png");

    private PlayerList playerList;
    private EditBox nameSearch;

    public SpeakerListScreen() {
        super(new TranslatableComponent("gui.voicechat.speaker_list.title"), 195, 146);
    }

    @Override
    protected void init() {
        super.init();
        hoverAreas.clear();
        clearWidgets();
        minecraft.keyboardHandler.setSendRepeatsToGui(true);

        playerList = new PlayerList(this, 9, 49, 160, 88, () -> {
            String input = nameSearch.getValue();
            if(input == null) input = "";
            return VoicechatClient.CLIENT.getPlayerStateManager().getPlayerStatesByName(input);
        });

        nameSearch = new EditBox(font, guiLeft + 78, guiTop + 20, 88, 10, TextComponent.EMPTY);

        nameSearch.setMaxLength(16);

        addRenderableWidget(nameSearch);
    }

    @Override
    public void tick() {
        super.tick();
        nameSearch.tick();
    }

    @Override
    public void onClose() {
        super.onClose();
        minecraft.keyboardHandler.setSendRepeatsToGui(false);
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float delta) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        blit(matrixStack, guiLeft, guiTop, 0, 0, xSize, ySize, 512, 512);

        playerList.drawGuiContainerBackgroundLayer(matrixStack, delta, mouseX, mouseY);

        playerList.drawGuiContainerForegroundLayer(matrixStack, mouseX, mouseY);

        for (Widget widget : renderables) {
            widget.render(matrixStack, mouseX, mouseY, delta);
        }

        font.draw(matrixStack, new TranslatableComponent("gui.voicechat.speaker_list.title"), guiLeft + 8, guiTop + 5, FONT_COLOR);
        font.draw(matrixStack, new TranslatableComponent("message.voicechat.search"), guiLeft + 8, guiTop + 21, FONT_COLOR);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            minecraft.setScreen(null);
            return true;
        }

        return nameSearch.keyPressed(keyCode, scanCode, modifiers)
                || nameSearch.isVisible()
                || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (playerList.mouseScrolled(mouseX, mouseY, amount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (playerList.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (playerList.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void resize(Minecraft client, int width, int height) {
        String groupNameText = nameSearch.getValue();
        init(client, width, height);
        nameSearch.setValue(groupNameText);
    }

}
