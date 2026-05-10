package com.example.corporate.controller;

import com.example.corporate.model.Prenotazione;
import com.example.corporate.model.security.ClientRequest;
import com.example.corporate.model.security.ServerToken;
import com.example.corporate.service.Manager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/concessionario/prenotazioni")
public class InterfacciaPrenotazioni {

    @Autowired
    private ObjectMapper objectMapper;
    private final Manager SERVICE;
    public InterfacciaPrenotazioni(Manager service){
        this.SERVICE = service;
    }


    private String generateRandomMessage() {
        SecureRandom random = new SecureRandom();

        int exp = random.nextInt(1, 13);
        int targetPlaintextSize = (int) Math.pow(2, exp);

        // Total binary size = IV(16) + MAC(32) + Ciphertext(blocks * 16)
        int totalBinaryBytes = 64 + targetPlaintextSize;
        byte[] noise = new byte[totalBinaryBytes];
        random.nextBytes(noise);

        return Base64.getEncoder().encodeToString(noise);
    }

    @PostMapping("/aggiungi")
    public ResponseEntity<HashMap<String, String>> addPrenotazione(@RequestBody ServerToken token) throws Exception {

        if (!SERVICE.addPrenotazione(token)) throw new RuntimeException("can not add it");

        var resultSet = new HashMap<String, Boolean>();
        resultSet.put("result", true);

        var response = new HashMap<String, String>();
        response.put("response",token.encryptForClient(objectMapper.writeValueAsString(resultSet)));

        return ResponseEntity.status(200).body(response);
    }

    @PostMapping("")
    public ResponseEntity<HashMap<String, String>> getPrenotazione(@RequestBody ClientRequest collect) throws Exception {

        var resultSet = SERVICE.getAllPrenotazioni(collect.getClientID());
        var wrap = new HashMap<String, Object>();
        wrap.put("result", resultSet);

        var response = new HashMap<String, String>();
        response.put("response", collect.encryptForClient(objectMapper.writeValueAsString(wrap)));
        return ResponseEntity.status(200).body(response);
    }

    /**
     * Generate a random string that looks like an encrypted one.
     * Hence, it obfuscates an attack feedback
     * @return a string similar to a base64 encrypted message via AES-256-CBC
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<HashMap<String, String>> handleJsonError() {
        var response = new HashMap<String, String>();
        response.put("response", generateRandomMessage());
        return ResponseEntity.status(200).body(response);
    }
}
