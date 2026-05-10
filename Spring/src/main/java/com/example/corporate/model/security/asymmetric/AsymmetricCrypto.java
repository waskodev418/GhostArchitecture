package com.example.corporate.model.security.asymmetric;

import javax.crypto.Cipher;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class AsymmetricCrypto {

    public final static String keyFilePath = "./keys/private_key.pem";

    public static String decryptAsymmetric(String payload) throws Exception {
        // 1. & 2. Verifica e lettura del file
        String keyContent = Files.readString(Paths.get(keyFilePath));

        // 3. Decodifica del payload (Base64)
        byte[] encryptedData = Base64.getDecoder().decode(payload);

        // 4. Caricamento della chiave privata (parsing del formato PEM)
        PrivateKey privateKey = loadPrivateKey(keyContent);

        // 5. Decrittografia con RSA e padding OAEP
        // Nota: OPENSSL_PKCS1_OAEP_PADDING in PHP corrisponde a "RSA/ECB/OAEPWithSHA-1AndMGF1Padding"
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] decryptedBytes = cipher.doFinal(encryptedData);
        return new String(decryptedBytes);
    }

    private static PrivateKey loadPrivateKey(String pemContent) throws Exception {
        // Rimuove intestazioni, piè di pagina e spazi bianchi
        String privateKeyPEM = pemContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);

        // Genera la chiave in formato PKCS8
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return keyFactory.generatePrivate(keySpec);
    }
}
