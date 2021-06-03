package de.foorcee.labymod.voicechat.client;

import com.mojang.authlib.Agent;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.util.UUIDTypeAdapter;
import lombok.extern.log4j.Log4j2;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import sun.misc.Unsafe;

import javax.naming.AuthenticationException;
import java.lang.reflect.Field;
import java.net.Proxy;
import java.util.UUID;

@Log4j2
public class DevAuthUtil {

    private static final Unsafe unsafe;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void auth() {
        if(System.getenv("password") == null) return;
        UserAuthentication auth = new YggdrasilAuthenticationService(Proxy.NO_PROXY, UUID.randomUUID()
                .toString()).createUserAuthentication(Agent.MINECRAFT);
        auth.setUsername(System.getenv("username"));
        auth.setPassword(System.getenv("password"));

        try {
            auth.logIn();
            if (auth.getAvailableProfiles().length == 0)
                throw new AuthenticationException("No valid gameprofile found for the given account.");

            try {
                User session = new User(auth.getSelectedProfile().getName(), UUIDTypeAdapter.fromUUID(auth.getSelectedProfile().getId()),
                        auth.getAuthenticatedToken(), auth.getUserType().getName());
                Field sessionField = Minecraft.class.getDeclaredField("user");
                long offset = unsafe.objectFieldOffset(sessionField);
                unsafe.putObject(Minecraft.getInstance(), offset, session);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        } catch (AuthenticationException | com.mojang.authlib.exceptions.AuthenticationException e) {
            log.warn("Could not login with the given credentials, are they correct?", e);
            return;
        }
    }
}
