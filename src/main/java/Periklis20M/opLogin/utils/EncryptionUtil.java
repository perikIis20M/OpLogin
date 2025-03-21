package Periklis20M.opLogin.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class EncryptionUtil {
    private final SecretKey key;

    public EncryptionUtil(String encodedKey) {
        if (encodedKey == null || encodedKey.isEmpty()) {
            // Generate new key if none exists
            byte[] newKey = new byte[16];
            new SecureRandom().nextBytes(newKey);
            this.key = new SecretKeySpec(newKey, "AES");
        } else {
            // Use existing key
            byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
            this.key = new SecretKeySpec(decodedKey, "AES");
        }
    }

    public String encrypt(String value) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedBytes = cipher.doFinal(value.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String decrypt(String encrypted) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encrypted));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getEncodedKey() {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
} 