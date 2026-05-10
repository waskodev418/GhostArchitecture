<?php 

//funzioni per la gestione del vault -----------------------------

function decrypt_client($payload, $clientkey) {

    $salt = str_repeat("\0", 32);
    $key = hash_hkdf('sha256', $clientkey, 32, 'encryption', $salt);
    $macKey= hash_hkdf('sha256', $clientkey, 32, 'auth', $salt);

    $method = "AES-256-CBC";
    
    $binaryData = base64_decode($payload, true);
    
    $macLength = 32;
    $ivLength = openssl_cipher_iv_length($method);

    //Extract the components (MAC, IV, payload)
    $macFromPayload = substr($binaryData, 0, $macLength);
    $iv = substr($binaryData, $macLength, $ivLength);
    $ciphertext = substr($binaryData, $macLength + $ivLength);
    
    //Verify the MAC
    $payloadToVerify = $iv . $ciphertext;
    $calculatedMac = hash_hmac('sha256', $payloadToVerify, $macKey, true);

    if (!hash_equals($macFromPayload, $calculatedMac)) {
        return false; // Authentication failed
    }

    // Decrypt the payload
    $decryptedJson = openssl_decrypt($ciphertext, $method, $key, OPENSSL_RAW_DATA, $iv);

    if ($decryptedJson === false) {
        return false;
    }

    //Decode the JSON back to an array/object
    return json_decode($decryptedJson, true);
}

function decrypt_asymmetric($payload, $keyFilePath = __DIR__ . "/private_key.pem") {

    if (!file_exists($keyFilePath)) {
        throw new Exception("Key file not found at: " . $keyFilePath);
    }

    $privateKeyContent = file_get_contents($keyFilePath);
    
    $encryptedData = base64_decode($payload, true);
    if ($encryptedData === false) return false;

    // Load the private key
    $privateKeyResource = openssl_pkey_get_private($privateKeyContent);

    // Decrypt
    $decrypted = '';
    $result = openssl_private_decrypt(
        $encryptedData, 
        $decrypted, 
        $privateKeyResource, 
        OPENSSL_PKCS1_OAEP_PADDING
    );

    return $result ? $decrypted : false;
}

function encrypt_asymmetric($plainText, $publicKeyPath = __DIR__ . "/public.pem") {
    
    if (!file_exists($publicKeyPath)) {
        throw new Exception("Public key file not found at: " . $publicKeyPath);
    }

    //Read the public key
    $publicKeyContent = file_get_contents($publicKeyPath);
    $publicKeyResource = openssl_pkey_get_public($publicKeyContent);

    if (!$publicKeyResource) {
        throw new Exception("Invalid public key.");
    }

    //Encrypt the data
    $encrypted = '';
    $result = openssl_public_encrypt(
        $plainText, 
        $encrypted, 
        $publicKeyResource, 
        OPENSSL_PKCS1_OAEP_PADDING
    );

    if (!$result) {
        throw new Exception("Encryption failed: " . openssl_error_string());
    }

    //Return as Base64 string
    return base64_encode($encrypted);
}

function readPulse($path = __DIR__ . "/symmetric_key.json") {

    if (!file_exists($path)) {
        return null;
    }

    // We use @ to suppress warnings in case the OS is busy swapping the file
    $content = @file_get_contents($path);
    
    if ($content === false) {
        return null;
    }

    $data = json_decode($content, true);
    
    if (json_last_error() !== JSON_ERROR_NONE) {
        return null;
    }

    return $data;
}
?>