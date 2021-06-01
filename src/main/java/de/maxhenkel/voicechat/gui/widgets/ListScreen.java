package de.maxhenkel.voicechat.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.VoicechatClient;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;

public abstract class ListScreen<T> extends Screen {

    protected static final int FONT_COLOR = 4210752;

    private static final ResourceLocation TEXTURE = new ResourceLocation(Voicechat.MODID, "textures/gui/gui_generic_small.png");

    protected int guiLeft;
    protected int guiTop;
    protected int xSize;
    protected int ySize;

    protected List<T> elements;
    protected int index;

    protected Button previous;
    protected Button back;
    protected Button next;

    protected Screen parent;

    public ListScreen(Screen parent, List<T> elements, Component title) {
        super(title);
        this.parent = parent;
        this.elements = elements;
        xSize = 248;
        ySize = 85;
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (width - this.xSize) / 2;
        this.guiTop = (height - this.ySize) / 2;

        previous = new Button(guiLeft + 10, guiTop + 60, 60, 20, new TranslatableComponent("message.voicechat.previous"), button -> {
            index = (index - 1 + elements.size()) % elements.size();
            updateCurrentElement();
        });

        back = new Button(guiLeft + xSize / 2 - 30, guiTop + 60, 60, 20, new TranslatableComponent("message.voicechat.back"), button -> {
            minecraft.setScreen(parent);
        });

        next = new Button(guiLeft + xSize - 70, guiTop + 60, 60, 20, new TranslatableComponent("message.voicechat.next"), button -> {
            index = (index + 1) % elements.size();
            updateCurrentElement();
        });

        updateCurrentElement();
    }

    public void updateCurrentElement() {
        clearWidgets();
        addRenderableWidget(previous);
        addRenderableWidget(back);
        addRenderableWidget(next);

        if (elements.size() <= 1) {
            next.visible = false;
            previous.visible = false;
        }
    }

    @Nullable
    public T getCurrentElement() {
        if (elements.size() <= 0) {
            return null;
        }
        return elements.get(index);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == KeyBindingHelper.getBoundKeyOf(minecraft.options.keyInventory).getValue() || keyCode == KeyBindingHelper.getBoundKeyOf(VoicechatClient.KEY_VOICE_CHAT).getValue()) {
            minecraft.setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(PoseStack stack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        blit(stack, guiLeft, guiTop, 0, 0, xSize, ySize);

        super.render(stack, mouseX, mouseY, partialTicks);

        renderText(stack, getCurrentElement(), mouseX, mouseY, partialTicks);
    }

    protected abstract void renderText(PoseStack stack, @Nullable T element, int mouseX, int mouseY, float partialTicks);
}
