package com.example.corporate.model.security;

import com.example.corporate.model.security.symmetric.SymmetricCrypto;
import org.antlr.v4.runtime.misc.Pair;

public abstract class SecureRequest {

    private String clientKey;

    public String encryptForClient(String message) throws Exception {
        return SymmetricCrypto.encrypt(message, new Pair<String, String>(clientKey, clientKey));
    }

    protected void setClientKey(String key){
        this.clientKey = key;
    }
}
