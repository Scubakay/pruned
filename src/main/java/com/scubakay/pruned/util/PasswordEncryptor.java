package com.scubakay.pruned.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class PasswordEncryptor {
    private static final String ALGORITHM = "AES";
    private static final int KEY_LENGTH = 16; // 128 bits

    private static byte[] getKey(String machineId) {
        String key = machineId.length() >= KEY_LENGTH ? machineId.substring(0, KEY_LENGTH) : String.format("%-16s", machineId).replace(' ', '0');
        return key.getBytes();
    }

    public static String encrypt(String password, String machineId) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(getKey(machineId), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(password.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting password", e);
        }
    }

    public static String decrypt(String encryptedPassword, String machineId) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(getKey(machineId), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getDecoder().decode(encryptedPassword);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting password", e);
        }
    }
}

