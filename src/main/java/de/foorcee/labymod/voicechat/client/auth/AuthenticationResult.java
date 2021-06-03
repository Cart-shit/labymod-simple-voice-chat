package de.foorcee.labymod.voicechat.client.auth;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class AuthenticationResult {
    private final AuthenticationMode mode;
    private final String pin;

    public static AuthenticationResult mojang() {
        return new AuthenticationResult(AuthenticationMode.MOJANG, null);
    }

    public static AuthenticationResult labyconnect(String pin) {
        return new AuthenticationResult(AuthenticationMode.LABYCONNECT, pin);
    }
}
