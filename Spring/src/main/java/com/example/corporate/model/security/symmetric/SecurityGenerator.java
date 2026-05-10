package com.example.corporate.model.security.symmetric;

import org.antlr.v4.runtime.misc.Pair;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Formatter;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

import static java.lang.Math.max;
import static java.lang.Math.min;

@Service
public class SecurityGenerator {

    private final static ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock(true);
    private static String merkleChain = "f2KlfqBSgY0h2w0h3";
    private static String serverKey1 = "6Lp9vRj8WzNfK2mXqYt7uG9vB4sP1nL3rE7xN1aZ8wQ";
    private static String serverKey2 = "8f2K9mP5zR7vXq4N1bW3sK0jH6tG9yU2";
    private static String serverKey3 = "9d1K9mP5zR7wYq4N1bW3sK0jH6t=HO1o4";

    public static Pair<String, String> getKeys(){
        try{
            LOCK.readLock().lock();
            return new Pair<>(serverKey1, serverKey2);
        }finally {
            LOCK.readLock().unlock();
        }
    }

    public static Pair<String, String> getElderKeys(){
        try{
            LOCK.readLock().lock();
            return new Pair<>(serverKey2, serverKey3);
        }finally {
            LOCK.readLock().unlock();
        }
    }


    //PULSE KEY -------------------------------

    public SecurityGenerator(){
        Thread daemon = new Thread(() ->{
            while(!Thread.interrupted()){
                int lifespan = pulseKeyGen();

                try {
                    Thread.sleep(lifespan * 1000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        daemon.start();
    }

    private int pulseKeyGen(){
        Function<Character, Integer> fromCharToInt = (Character c) -> (int)c;
        Function<String, Character> getBack = (String s) -> s.charAt(s.length() - 1);

        try{
            LOCK.writeLock().lock();
            int reminder = fromCharToInt.apply(merkleChain.charAt(0)) % 3;
            String prk;

            if(reminder == 0) prk = hmacSha256(hmacSha256(serverKey1, serverKey2), hmacSha256(serverKey3, merkleChain));
            else if(reminder == 1) prk = hmacSha256(hmacSha256(serverKey3, merkleChain), hmacSha256(serverKey2, serverKey1));
            else prk = hmacSha256(hmacSha256(serverKey2, serverKey1), hmacSha256(merkleChain, serverKey3));

            //since we will use a derivation from the main key in the ciphers,
            //an unfixed length increments entropy much more than a fixed one
            int offset = fromCharToInt.apply(serverKey3.charAt(5)) % 22;
            int size = fromCharToInt.apply(getBack.apply(serverKey2)) % 22 + 20;
            String reverse_key = prk.substring(offset, offset + size);

            int idx1 = fromCharToInt.apply(getBack.apply(serverKey1)) % serverKey2.length();
            int idx2 = fromCharToInt.apply(serverKey1.charAt(serverKey1.length()-2)) % serverKey3.length();

            int val1 = fromCharToInt.apply(serverKey2.charAt(idx1));
            int val2 = fromCharToInt.apply(serverKey3.charAt(idx2));
            int lifespan = max(60, val1 + val2);

            boolean isEven = (fromCharToInt.apply(getBack.apply(merkleChain)) % 2 == 0);
            merkleChain = isEven ? hmacSha256(merkleChain, serverKey3) : hmacSha256(serverKey3, merkleChain);

            serverKey3 = serverKey2;
            serverKey2 = serverKey1;
            serverKey1 = "";
            for (int i = reverse_key.length() - 1; i >= 0; i--) serverKey1 += reverse_key.charAt(i);

            return lifespan;
        }finally {
            LOCK.writeLock().unlock();
        }
    }

    private String hmacSha256(String key, String data) {
        try {
            // Initialize the HMAC-SHA256 Mac instance
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");

            // Create the key specification
            SecretKeySpec secret_key = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            sha256_HMAC.init(secret_key);

            // Perform the hashing
            byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // Convert the byte array to a Hex String (matching C++ output)
            return bytesToHex(hash).toLowerCase();

        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC-SHA256", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
