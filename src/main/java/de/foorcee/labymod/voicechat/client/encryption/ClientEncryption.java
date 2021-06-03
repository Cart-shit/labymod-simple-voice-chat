package de.foorcee.labymod.voicechat.client.encryption;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class ClientEncryption {

    public SecretKey generateKey() throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(128);
        return generator.generateKey();
    }

    public byte[] getServerIdHash(String input, PublicKey key, SecretKey secretKey) throws NoSuchAlgorithmException {
        return diggestOperation("SHA-1", input.getBytes(StandardCharsets.UTF_8), secretKey.getEncoded(), key.getEncoded());
    }

    private byte[] diggestOperation(String algo, byte[]... bytes) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algo);
        for (byte[] b : bytes) {
            digest.update(b);
        }
        return digest.digest();
    }

    public PublicKey decodePublicKey(byte[] data) throws NoSuchAlgorithmException, InvalidKeySpecException {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(spec);
    }
}
