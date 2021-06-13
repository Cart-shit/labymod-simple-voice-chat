package de.maxhenkel.voicechat.voice.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.foorcee.labymod.voicechat.client.LabymodVoicechatClient;
import de.maxhenkel.voicechat.VoicechatClient;
import de.maxhenkel.voicechat.events.ClientWorldEvents;
import de.maxhenkel.voicechat.events.RenderEvents;
import de.maxhenkel.voicechat.gui.VoiceChatScreen;
import de.maxhenkel.voicechat.gui.VoiceChatSettingsScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.mixin.client.rendering.MixinInGameHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

@Environment(EnvType.CLIENT)
public class ClientVoiceEvents {

    private static final ResourceLocation MICROPHONE_ICON = new ResourceLocation(VoicechatClient.MODID, "textures/gui/microphone.png");
    private static final ResourceLocation MICROPHONE_OFF_ICON = new ResourceLocation(VoicechatClient.MODID, "textures/gui/microphone_off.png");
    private static final ResourceLocation SPEAKER_ICON = new ResourceLocation(VoicechatClient.MODID, "textures/gui/speaker.png");
    private static final ResourceLocation SPEAKER_ICON_INACTIVE = new ResourceLocation(VoicechatClient.MODID, "textures/gui/speaker_inactive.png");
    private static final ResourceLocation SPEAKER_OFF_ICON = new ResourceLocation(VoicechatClient.MODID, "textures/gui/speaker_off.png");
    private static final ResourceLocation DISCONNECT_ICON = new ResourceLocation(VoicechatClient.MODID, "textures/gui/disconnected.png");
    private static final ResourceLocation GROUP_ICON = new ResourceLocation(VoicechatClient.MODID, "textures/gui/group.png");

    private final LabymodVoicechatClient client;
    private final ClientPlayerStateManager playerStateManager;
    private final TalkCache talkCache;
    private final PTTKeyHandler pttKeyHandler;
    private final Minecraft minecraft;

    public ClientVoiceEvents(VoicechatClient client) {
        playerStateManager = new ClientPlayerStateManager();
        talkCache = new TalkCache();
        pttKeyHandler = new PTTKeyHandler();
        minecraft = Minecraft.getInstance();

        ClientWorldEvents.DISCONNECT.register(this::onDisconnect);

        HudRenderCallback.EVENT.register(this::renderHUD);
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTickEnd);
        RenderEvents.RENDER_NAMEPLATE.register(this::onRenderName);

        this.client = new LabymodVoicechatClient(client);
        this.client.connect();
    }

    public void onDisconnect() {
//        ClientVoiceChatEvents.VOICECHAT_DISCONNECTED.invoker().run();
//        if (client != null) {
//            client = null;
//        }
    }

    @Nullable
    public LabymodVoicechatClient getClient() {
        return client;
    }

    public ClientPlayerStateManager getPlayerStateManager() {
        return playerStateManager;
    }

    public PTTKeyHandler getPttKeyHandler() {
        return pttKeyHandler;
    }

    public void renderHUD(PoseStack stack, float tickDelta) {
        if (!isMultiplayerServer()) {
            return;
        }
        if (VoicechatClient.CLIENT_CONFIG.hideIcons.get()) {
            return;
        }

        if ((client != null && !client.isConnected()) || playerStateManager.isDisconnected()) {
            renderIcon(stack, DISCONNECT_ICON);
        } else if (playerStateManager.isDisabled()) {
            renderIcon(stack, SPEAKER_OFF_ICON);
        } else if (playerStateManager.isMuted() && VoicechatClient.CLIENT_CONFIG.microphoneActivationType.get().equals(MicrophoneActivationType.VOICE)) {
            renderIcon(stack, MICROPHONE_OFF_ICON);
        } else if (client != null && client.getMicThread() != null && client.getMicThread().isTalking()) {
            renderIcon(stack, MICROPHONE_ICON);
        }

        if (Minecraft.getInstance().options.renderDebug) return;
        TalkingChatManager.renderIcons(stack);
    }

    private void renderIcon(PoseStack matrixStack, ResourceLocation texture) {
        matrixStack.pushPose();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, texture);
        //double width = minecraft.getMainWindow().getScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        Screen.blit(matrixStack, 16, height - 32, 0, 0, 16, 16, 16, 16);
        matrixStack.popPose();
    }

    public void onClientTickEnd(Minecraft minecraft) {
        if (VoicechatClient.KEY_VOICE_CHAT.consumeClick() && checkConnected()) {
            minecraft.setScreen(new VoiceChatScreen());
        }

        if (VoicechatClient.KEY_GROUP.consumeClick() && checkConnected()) {
        }

        if (VoicechatClient.KEY_VOICE_CHAT_SETTINGS.consumeClick() && checkConnected()) {
            minecraft.setScreen(new VoiceChatSettingsScreen());
        }

        if (VoicechatClient.KEY_PTT.consumeClick()) {
            checkConnected();
        }

        if (VoicechatClient.KEY_MUTE.consumeClick() && checkConnected()) {
            playerStateManager.setMuted(!playerStateManager.isMuted());
        }

        if (VoicechatClient.KEY_DISABLE.consumeClick() && checkConnected()) {
            playerStateManager.setDisabled(!playerStateManager.isDisabled());
        }

        if (VoicechatClient.KEY_HIDE_ICONS.consumeClick()) {
            boolean hidden = !VoicechatClient.CLIENT_CONFIG.hideIcons.get();
            VoicechatClient.CLIENT_CONFIG.hideIcons.set(hidden);
            VoicechatClient.CLIENT_CONFIG.hideIcons.save();

            if (hidden) {
                minecraft.player.displayClientMessage(new TranslatableComponent("message.voicechat.icons_hidden"), true);
            } else {
                minecraft.player.displayClientMessage(new TranslatableComponent("message.voicechat.icons_visible"), true);
            }
        }

        if (minecraft.screen != null) {
            if (client.getMicThread() != null && !client.getMicThread().isMicrophoneLocked()) {
                client.getMicThread().setMicrophoneLocked(true);
            }
        }else{
            if (client.getMicThread() != null && client.getMicThread().isMicrophoneLocked()) {
                client.getMicThread().setMicrophoneLocked(false);
            }
        }
    }

    public boolean checkConnected() {
        if (VoicechatClient.CLIENT.getClient() == null || !VoicechatClient.CLIENT.getClient().getState().isConnected()) {
            sendUnavailableMessage();
            return false;
        }
        return true;
    }

    public void sendUnavailableMessage() {
        minecraft.player.displayClientMessage(new TranslatableComponent("message.voicechat.voice_chat_unavailable"), true);
    }

    public boolean isMultiplayerServer() {
        return minecraft.getCurrentServer() != null && !minecraft.getCurrentServer().isLan();
    }

    public void onRenderName(Entity entity, Component component, PoseStack stack, MultiBufferSource vertexConsumers, int light) {
        if (!isMultiplayerServer()) {
            return;
        }
        if (VoicechatClient.CLIENT_CONFIG.hideIcons.get()) {
            return;
        }
        if (!(entity instanceof Player)) {
            return;
        }
        if (entity == minecraft.player) {
            return;
        }

        Player player = (Player) entity;

        if (!minecraft.options.hideGui) {
            if (playerStateManager.isPlayerDisconnected(player)) {
                return;
                //renderPlayerIcon(player, component, DISCONNECT_ICON, stack, vertexConsumers, light);
            } else if (playerStateManager.isPlayerDisabled(player)) {
                renderPlayerIcon(player, component, SPEAKER_OFF_ICON, stack, vertexConsumers, light);
            } else if (VoicechatClient.CLIENT.getTalkCache().isTalking(player)) {
                renderPlayerIcon(player, component, SPEAKER_ICON, stack, vertexConsumers, light);
            } else {
                renderPlayerIcon(player, component, SPEAKER_ICON_INACTIVE, stack, vertexConsumers, light);
            }
        }
    }

    public TalkCache getTalkCache() {
        return talkCache;
    }

    protected void renderPlayerIcon(Player player, Component component, ResourceLocation texture, PoseStack matrixStackIn, MultiBufferSource buffer, int light) {
        matrixStackIn.pushPose();
        matrixStackIn.translate(0D, player.getBbHeight() + 0.5D, 0D);
        matrixStackIn.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        matrixStackIn.scale(-0.025F, -0.025F, 0.025F);
        matrixStackIn.translate(0D, -1D, 0D);

        float offset = (float) (minecraft.font.width(component) / 2 + 2);

        VertexConsumer builder = buffer.getBuffer(RenderType.text(texture));
        int alpha = 32;

        if (player.isDiscrete()) {
            vertex(builder, matrixStackIn, offset, 10F, 0F, 0F, 1F, alpha, light);
            vertex(builder, matrixStackIn, offset + 10F, 10F, 0F, 1F, 1F, alpha, light);
            vertex(builder, matrixStackIn, offset + 10F, 0F, 0F, 1F, 0F, alpha, light);
            vertex(builder, matrixStackIn, offset, 0F, 0F, 0F, 0F, alpha, light);
        } else {
            vertex(builder, matrixStackIn, offset, 10F, 0F, 0F, 1F, light);
            vertex(builder, matrixStackIn, offset + 10F, 10F, 0F, 1F, 1F, light);
            vertex(builder, matrixStackIn, offset + 10F, 0F, 0F, 1F, 0F, light);
            vertex(builder, matrixStackIn, offset, 0F, 0F, 0F, 0F, light);

            VertexConsumer builderSeeThrough = buffer.getBuffer(RenderType.textSeeThrough(texture));
            vertex(builderSeeThrough, matrixStackIn, offset, 10F, 0F, 0F, 1F, alpha, light);
            vertex(builderSeeThrough, matrixStackIn, offset + 10F, 10F, 0F, 1F, 1F, alpha, light);
            vertex(builderSeeThrough, matrixStackIn, offset + 10F, 0F, 0F, 1F, 0F, alpha, light);
            vertex(builderSeeThrough, matrixStackIn, offset, 0F, 0F, 0F, 0F, alpha, light);
        }

        matrixStackIn.popPose();
    }

    private static void vertex(VertexConsumer builder, PoseStack matrixStack, float x, float y, float z, float u, float v, int light) {
        vertex(builder, matrixStack, x, y, z, u, v, 255, light);
    }

    private static void vertex(VertexConsumer builder, PoseStack matrixStack, float x, float y, float z, float u, float v, int alpha, int light) {
        PoseStack.Pose entry = matrixStack.last();
        builder.vertex(entry.pose(), x, y, z)
                .color(255, 255, 255, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(entry.normal(), 0F, 0F, -1F)
                .endVertex();
    }

}
