package de.foorcee.labymod.voicechat.client.encryption;

import javax.crypto.Cipher;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class EncryptionUtil {

    public static byte[] encryptData(Key key, byte[] b) throws GeneralSecurityException {
        return cipherOperation(1, key, b);
    }

    public static byte[] cipherOperation(int i, Key key, byte[] bytes) throws GeneralSecurityException {
        return createTheCipherInstance(i, key.getAlgorithm(), key).doFinal(bytes);
    }

    private static Cipher createTheCipherInstance(int i, String string, Key key) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(string);
        cipher.init(i, key);
        return cipher;
    }
}
