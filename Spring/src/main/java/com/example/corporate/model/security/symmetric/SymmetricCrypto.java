package com.example.corporate.model.security.symmetric;

import org.antlr.v4.runtime.misc.Pair;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

public class SymmetricCrypto extends SecurityGenerator {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * Derivate the keys necessary for encryption
     * @param keyVault the main key
     * @param info the tag: either "encryption" or "auth"
     * @return the bytes of the key
     * @throws Exception in case the process fails
     */
    private static byte[] hkdfExpand(String keyVault, String info) throws Exception {
        byte[] ikm = keyVault.getBytes(StandardCharsets.UTF_8);
        byte[] salt = new byte[32];

        Mac hmac = Mac.getInstance(HMAC_ALGORITHM);

        // EXTRACT
        // match PHP salt
        hmac.init(new SecretKeySpec(salt, HMAC_ALGORITHM));
        byte[] prk = hmac.doFinal(ikm);

        // EXPAND
        hmac.init(new SecretKeySpec(prk, HMAC_ALGORITHM));
        byte[] infoBytes = info.getBytes(StandardCharsets.UTF_8);
        byte[] concat = new byte[infoBytes.length + 1];
        System.arraycopy(infoBytes, 0, concat, 0, infoBytes.length);
        concat[infoBytes.length] = 0x01; // Primo blocco T1

        return Arrays.copyOf(hmac.doFinal(concat), 32);
    }

    public static String encrypt(String data, Pair<String,String> keyPair) throws Exception {

        byte[] key = hkdfExpand(keyPair.a, "encryption");
        byte[] macKey = hkdfExpand(keyPair.b, "auth");

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        byte[] iv = new byte[cipher.getBlockSize()];
        new java.security.SecureRandom().nextBytes(iv);

        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));

        byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // Payload: IV + Ciphertext
        byte[] payload = new byte[iv.length + encryptedData.length];
        System.arraycopy(iv, 0, payload, 0, iv.length);
        System.arraycopy(encryptedData, 0, payload, iv.length, encryptedData.length);

        // MAC (HMAC-SHA256)
        Mac hmac = Mac.getInstance(HMAC_ALGORITHM);
        hmac.init(new SecretKeySpec(macKey, HMAC_ALGORITHM));
        byte[] mac = hmac.doFinal(payload);

        // Base64(MAC + Payload)
        byte[] fullResult = new byte[mac.length + payload.length];
        System.arraycopy(mac, 0, fullResult, 0, mac.length);
        System.arraycopy(payload, 0, fullResult, mac.length, payload.length);

        return Base64.getEncoder().encodeToString(fullResult);
    }

    public static String decrypt(String base64Token, Pair<String,String> keyPair) throws Exception {
        byte[] fullData = Base64.getDecoder().decode(base64Token);
        byte[] key = hkdfExpand(keyPair.a, "encryption");
        byte[] macKey = hkdfExpand(keyPair.b, "auth");

        // extract MAC (32 byte per SHA-256)
        byte[] receivedMac = Arrays.copyOfRange(fullData, 0, 32);
        byte[] payload = Arrays.copyOfRange(fullData, 32, fullData.length);

        // verify MAC
        Mac hmac = Mac.getInstance(HMAC_ALGORITHM);
        hmac.init(new SecretKeySpec(macKey, HMAC_ALGORITHM));
        byte[] computedMac = hmac.doFinal(payload);
        if (!java.security.MessageDigest.isEqual(receivedMac, computedMac)) {
            throw new SecurityException("MAC non valido! Il token è stato manomesso.");
        }

        // extract IV (16 byte per AES) e Ciphertext
        byte[] iv = Arrays.copyOfRange(payload, 0, 16);
        byte[] encryptedData = Arrays.copyOfRange(payload, 16, payload.length);

        // decrypt
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        return new String(cipher.doFinal(encryptedData), StandardCharsets.UTF_8);
    }
}
