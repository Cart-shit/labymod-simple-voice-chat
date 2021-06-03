package de.foorcee.labymod.voicechat.client.auth;

import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.Minecraft;

import java.net.Proxy;
import java.util.UUID;
import java.util.function.Consumer;

public class AuthenticationHandler {

    public void handleAuthentication(String hash, Consumer<AuthenticationResult> resultConsumer) throws AuthenticationException {
        MinecraftSessionService sessionService = new YggdrasilAuthenticationService(Proxy.NO_PROXY, UUID.randomUUID().toString()).createMinecraftSessionService();
        sessionService.joinServer(Minecraft.getInstance().getUser().getGameProfile(), Minecraft.getInstance().getUser().getAccessToken(), hash);
        resultConsumer.accept(AuthenticationResult.mojang());
    }
}
