import { PUBLIC_KEY } from "./archive.js.php";

// asymmetric encryption
export async function asym_encrypt(plainText, pemKey = PUBLIC_KEY) {
    // Strip PEM headers and convert to binary
    const pemHeader = "-----BEGIN PUBLIC KEY-----";
    const pemFooter = "-----END PUBLIC KEY-----";
    const pemContents = pemKey.substring(
        pemKey.indexOf(pemHeader) + pemHeader.length,
        pemKey.indexOf(pemFooter)
    ).replace(/\s/g, '');

    const binaryDerString = window.atob(pemContents);
    const binaryDer = new Uint8Array(binaryDerString.length);
    for (let i = 0; i < binaryDerString.length; i++) {
        binaryDer[i] = binaryDerString.charCodeAt(i);
    }

    // Import Key
    const cryptoKey = await window.crypto.subtle.importKey(
        "spki",
        binaryDer.buffer,
        {
            name: "RSA-OAEP",
            hash: "SHA-1", 
        },
        false,
        ["encrypt"]
    );

    // Encrypt
    const encoder = new TextEncoder();
    const encrypted = await window.crypto.subtle.encrypt(
        { name: "RSA-OAEP" },
        cryptoKey,
        encoder.encode(plainText)
    );

    // Convert to Base64 for PHP transport
    return btoa(String.fromCharCode(...new Uint8Array(encrypted)));
}

// symmetric encryption --------------------------------------------

async function generateClientKey() {
  const bytes = window.crypto.getRandomValues(new Uint8Array(32));
  return btoa(String.fromCharCode(...bytes));
}

async function deriveKeys(key) {
  const encoder = new TextEncoder();
  const ikm = encoder.encode(key); 

  // Match Java
  const salt = new Uint8Array(32); 

  const baseKey = await crypto.subtle.importKey(
    "raw", 
    ikm,
    "HKDF", 
    false, 
    ["deriveKey"]
  );

  // Derive Encryption Key
  const encKey = await crypto.subtle.deriveKey(
    { 
        name: "HKDF", 
        hash: "SHA-256", 
        salt: salt,
        info: encoder.encode("encryption") 
    },
    baseKey, 
    { name: "AES-CBC", length: 256 }, 
    true, 
    ["encrypt", "decrypt"]
  );

  // Derive Auth Key (MAC)
  const authKey = await crypto.subtle.deriveKey(
    { 
        name: "HKDF", 
        hash: "SHA-256", 
        salt: salt,
        info: encoder.encode("auth") 
    },
    baseKey, 
    { name: "HMAC", hash: "SHA-256", length: 256 }, 
    true, 
    ["sign", "verify"]
  );

  return { encKey, authKey };
}

export async function encrypt(text, key) {

    const CLIENT_KEY = key || await generateClientKey();
    
    const { encKey, authKey } = await deriveKeys(CLIENT_KEY);
    const encoder = new TextEncoder();
    const data = encoder.encode(JSON.stringify(text));
    const iv = crypto.getRandomValues(new Uint8Array(16));

    // Encrypt with AES-CBC
    const ciphertextBuffer = await crypto.subtle.encrypt(
    { name: "AES-CBC", iv: iv },
    encKey,
    data
    );
    const ciphertext = new Uint8Array(ciphertextBuffer);

    // Prepare payload for MAC (IV + Ciphertext)
    const payloadToSign = new Uint8Array(iv.length + ciphertext.length);
    payloadToSign.set(iv);
    payloadToSign.set(ciphertext, iv.length);

    // Create HMAC-SHA256
    const hmacBuffer = await crypto.subtle.sign("HMAC", authKey, payloadToSign);
    const hmac = new Uint8Array(hmacBuffer);

    // MAC + IV + Ciphertext
    const finalResult = new Uint8Array(hmac.length + payloadToSign.length);
    finalResult.set(hmac);
    finalResult.set(payloadToSign, hmac.length);

    // Convert to Base64
    return { 
        payload: btoa(String.fromCharCode(...finalResult)), 
        key: CLIENT_KEY 
    };
}

// decrypt
export async function decrypt(payload, clientKey) {
  // Derive the same keys used for encryption
  const { encKey, authKey } = await deriveKeys(clientKey);

  // Decode Base64 string to bytes
  const binaryString = atob(payload);
  const binaryData = new Uint8Array(binaryString.length);
  for (let i = 0; i < binaryString.length; i++) {
    binaryData[i] = binaryString.charCodeAt(i);
  }

  // Define lengths
  const macLength = 32; 
  const ivLength = 16;  // AES-256-CBC IV length

  // Extract components
  const macFromPayload = binaryData.slice(0, macLength);
  const iv = binaryData.slice(macLength, macLength + ivLength);
  const ciphertext = binaryData.slice(macLength + ivLength);

  // Verify the MAC
  const payloadToVerify = binaryData.slice(macLength);
  const isValid = await crypto.subtle.verify(
    "HMAC",
    authKey,
    macFromPayload,
    payloadToVerify
  );

  if (!isValid) {
    throw new Error("Authentication failed: Data has been tampered with.");
  }

  // Decrypt
  try {
    const decryptedBuffer = await crypto.subtle.decrypt(
      { name: "AES-CBC", iv: iv },
      encKey,
      ciphertext
    );

    // Decode the JSON string back to an object
    const decoder = new TextDecoder();
    return JSON.parse(decoder.decode(decryptedBuffer));
  } catch (e) {
    throw new Error("Decryption failed: Likely incorrect key or corrupted data.");
  }
}