package de.maxhenkel.voicechat.voice.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.foorcee.labymod.voicechat.client.LabymodVoicechatClient;
import de.maxhenkel.voicechat.VoicechatClient;
import de.maxhenkel.voicechat.gui.SkinUtils;
import de.maxhenkel.voicechat.voice.common.PlayerState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TalkingChatManager {

    private static final ResourceLocation TALK_OUTLINE = new ResourceLocation(VoicechatClient.MODID, "textures/gui/talk_outline.png");
    private static final Minecraft minecraft = Minecraft.getInstance();

    public static void renderIcons(PoseStack matrixStack) {
        LabymodVoicechatClient client = VoicechatClient.CLIENT.getClient();

        if (client == null) {
            return;
        }

        List<PlayerState> groupMembers = getTalkingPlayers(false);
        if (groupMembers.isEmpty()) return;

        matrixStack.pushPose();
        matrixStack.translate(8, 8, 0);
        matrixStack.scale(2F, 2F, 1F);

        for (int i = 0; i < groupMembers.size(); i++) {
            PlayerState state = groupMembers.get(i);
            matrixStack.pushPose();
            matrixStack.translate(0, i * 11, 0);

            if (VoicechatClient.CLIENT.getTalkCache().isTalking(state.getGameProfile().getId())) {
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
                RenderSystem.setShaderTexture(0, TALK_OUTLINE);
                Screen.blit(matrixStack, 0, 0, 0, 0, 10, 10, 16, 16);
            }


            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.setShaderTexture(0, SkinUtils.getSkin(state.getGameProfile()));
            Screen.blit(matrixStack, 1, 1, 8, 8, 8, 8, 64, 64);
            Screen.blit(matrixStack, 1, 1, 40, 8, 8, 8, 64, 64);

            matrixStack.pushPose();
            matrixStack.translate(12, 2.5, 0);
            matrixStack.scale(0.5F, 0.5F, 1F);
            minecraft.font.draw(matrixStack, getPlayerName(state), 1f, 1f, 1);
            matrixStack.popPose();

            matrixStack.popPose();
        }

        matrixStack.popPose();
    }

    public static List<PlayerState> getTalkingPlayers() {
        return getTalkingPlayers(true);
    }

    public static List<PlayerState> getTalkingPlayers(boolean includeSelf) {
        return VoicechatClient.CLIENT.getTalkCache()
                .getTalkingPlayers(5, TimeUnit.SECONDS).stream()
                .filter(uuid -> includeSelf || !uuid.equals(Minecraft.getInstance().player.getUUID()))
                .map(VoicechatClient.CLIENT.getPlayerStateManager()::getState).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private static Component getPlayerName(PlayerState playerState) {
        GameProfile profile = playerState.getGameProfile();
        if (minecraft.level != null) {
            Player player = minecraft.level.getPlayerByUUID(profile.getId());
            if (player != null) {
                return player.getDisplayName();
            }
            if (minecraft.getConnection() != null) {
                PlayerInfo playerInfo = minecraft.getConnection().getPlayerInfo(profile.getId());
                if (playerInfo != null && playerInfo.getTabListDisplayName() != null) {
                    return playerInfo.getTabListDisplayName();
                }
            }
        }

        TextComponent component = new TextComponent(playerState.getGameProfile().getName());
        component.setStyle(component.getStyle().applyFormat(ChatFormatting.WHITE));
        return component;
    }
}
