package de.maxhenkel.voicechat.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.netty.buffer.Unpooled;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.charset.StandardCharsets;

@Mixin(ClientPacketListener.class)
public class LabymodSpooferMixin {

    @Shadow
    private Connection connection;

    @Inject(at = @At("HEAD"), method = "handleCustomPayload")
    public void injectPayloadChannel(ClientboundCustomPayloadPacket packet, CallbackInfo callbackInfo) {
        System.out.println(packet.getIdentifier());
        if (ClientboundCustomPayloadPacket.BRAND.equals(packet.getIdentifier())) {

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("version", "Es gibt kein LabyMod für die 1.17.0");
            JsonObject ccpObj = new JsonObject();
            ccpObj.addProperty("enabled", true);
            ccpObj.addProperty("version", 2);
            jsonObject.add("ccp", ccpObj);

            JsonObject shadowObj = new JsonObject();
            shadowObj.addProperty("enabled", true);
            shadowObj.addProperty("version", 1);
            jsonObject.add("shadow", shadowObj);

            jsonObject.add("addons", new JsonArray());

            FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.buffer());

            byte[] keyBytes = "INFO".getBytes(StandardCharsets.UTF_8);
            byteBuf.writeVarInt(keyBytes.length);
            byteBuf.writeBytes(keyBytes);

            byte[] messageBytes = jsonObject.toString().getBytes(StandardCharsets.UTF_8);
            byteBuf.writeVarInt(messageBytes.length);
            byteBuf.writeBytes(messageBytes);

            connection.send(new ServerboundCustomPayloadPacket(new ResourceLocation("labymod3", "main"), byteBuf));
        }
    }
}
