package com.example.corporate.model.security;

import com.example.corporate.model.security.symmetric.SecurityGenerator;
import com.example.corporate.model.security.symmetric.SymmetricCrypto;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

public class ServerToken extends SecureRequest{

    private final Map<String, Object> VALORI;
    private final String TOKEN_ID;

    @JsonCreator
    public ServerToken(@JsonProperty("token") String raw_data) throws Exception {
        String data;
        try {
            data = SymmetricCrypto.decrypt(raw_data, SymmetricCrypto.getKeys());
        } catch (Exception e) {
            data = SymmetricCrypto.decrypt(raw_data, SymmetricCrypto.getElderKeys());
        }

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> root = mapper.readValue(data, new TypeReference<Map<String, Object>>() {});

        this.TOKEN_ID = String.valueOf(root.get("tokenid"));
        setClientKey(String.valueOf(root.get("key")));

        this.VALORI = (Map<String, Object>) root.get("data");
    }

    public String getTokenID(){
        return this.TOKEN_ID;
    }

    public String getData(String key) {
        Object val = VALORI.get(key);
        return val != null ? String.valueOf(val) : null;
    }
}
