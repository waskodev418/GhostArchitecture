package com.example.corporate.model.security;

import com.example.corporate.model.security.asymmetric.AsymmetricCrypto;
import com.example.corporate.model.security.symmetric.SymmetricCrypto;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.antlr.v4.runtime.misc.Pair;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;

public class ClientRequest extends SecureRequest {

    private final String CLIENT_ID;

    @JsonCreator
    public ClientRequest(@JsonProperty("collect") HashMap<String, String> raw_token) throws Exception {
        String key = AsymmetricCrypto.decryptAsymmetric(raw_token.get("key"));
        String encrypted_id = SymmetricCrypto.decrypt(raw_token.get("clientid"), new Pair<>(key, key));
        setClientKey(key);
        this.CLIENT_ID = AsymmetricCrypto.decryptAsymmetric(encrypted_id.replace("\"", ""));
    }

    public String getClientID() {
        return CLIENT_ID;
    }
}
