# GhostArchitecture 
Note: this file contains some specifics of the architecture while the folders contains a simple - hopeful effective - `proof of concept`.
Disclaimer: The original development was carried out across multiple repositories on my institutional (school) account. This version has been refactored and centralized to facilitate peer review and demonstrate the architecture's core pillars in a single, cohesive environment.
## Core Pillars

### 1. Distributed
Security is decentralized across three distinct nodes:
- **The Thick Client:** Manages primary encryption and ephemeral key lifecycles.
- **Web Server:** Functions as a front-line filter that authorizes non-idempotent operations without ever seeing the raw data, effectively decoupling the `read path` from the `write path`.
- **Data Server:** The final authority that communicates with the DBMS and executes validated requests.

### 2. Stateless
By utilizing **Temporal Key Shifting (The Pulse)** and symmetric tokens, the system eliminates server-side session state. Every packet is self-validating, enabling seamless horizontal scaling and inherent resilience against session hijacking.

### 3. Thick Client
The client is an active cryptographic participant. It performs "Inner Box" encryption using an ephemeral key ($K_{client}$). This ensures that sensitive data remains opaque during transit, even if the "Outer Box" (transport layer) is stripped or inspected by the Relay.

### 4. Privacy-by-Design
Only the architecture as a whole knows the truth, but no individual component possesses that knoledge. Here some example:
- **The Web Server** possesses zero knowledge of the payload's intent.
- **The Network** gains zero knowledge of transaction outcomes. All responses - whether Success, Failure, or Replay - are masked identically with constant-length, high-entropy payloads.


### 5. Role-Based Isolation
Each node within the architecture is specialized to perform a single set of operations and output a result. This modularity facilitates easy network expansion: neither servers nor clients need to track the identity of their interlocutors, provided the messages are cryptographically valid.

---

## Defensive Mechanisms

### Protocol Obfuscation
To blind attackers, the **Data Server** eschews standard success/failure indicators:
- **Uniform Status:** Every response is indistinguishable at the protocol level.
- **Metadata Blinding:** Responses are strictly padded to **64 bytes** (IV + Ciphertext + MAC).
- **Cryptographic Noise:** Replay attacks or decryption failures trigger the generation of 64 bytes of cryptographically secure random noise, rendering "Failure" indistinguishable from "Success."

### Asymmetric Resource Exhaustion
- **Cheap Rejection:** Malicious or expired tokens are discarded at the decryption layer.
- **AES-NI Acceleration:** Decryption is orders of magnitude faster than Database I/O, allowing the server to neutralize DoS attacks by dropping junk traffic before it impacts the DBMS.
- **DBMS Integrity:** Operational safety is reinforced via Unique Constraints, ensuring the database naturally rejects duplicate state changes resulting from replayed tokens.

---

## Data Flow

### Deterministic key generation
The following algorithm is written in a personal psudocode. It's goal is to just show how to generate - in a deterministic way - a new key.

``` java
Result getRotateKet(){

    int reminder = root[0] % 3;
    var prk = switch(reminder){
        // it hashes arg1 with arg2, arg3 with arg4 and then hashes the two results
        0 => hmacSha256(key1, key2, key3, root)
        1 => hmacSha256(key3, root, key2 , key1)
        2 => hmacSha256(key2, key1, root, key3)
    };
    
    int offset = key3[5] % 22;
    int lenght = key2[-1] % 22 + 20;
    var reverse_key = prk.getSubstring( offset, lenght );
    
    int lifespan = Integer.parse(key2[ key1[-1] % key2.size() ] + key3[ key1[-2] % key3.size() ]);
    lifespan = max(60, lifespan);
    
    root = (root[-1] % 2 == 0) :
    hmacSha256(root, key3) ? hmacSha256(key3, root);
    
    key3 = key2;
    key2 = key1;
    key1 = reverse_key.reverse();
    
    return {key1, lifespan};
} 
```
The system utilizes a sliding-window synchronization where the key-rotation cadence is self-correcting based on the key involved in the successful decryption:
if the server decrypted the message using the second key then it will count a few seconds more before generating the next pulse.
This feature is applicable only once per pulse.

### Non-Idempotent Operations

1. The Client sends its data via a `solicit` message to the **Web Server**:

```json 
"solicit": {
    "vault": {
        "content": "... encrypted with the client key (K_client)"
    },
    "key": "[K_client encrypted with the Web Server's public key]"
}
```

2. *Web server* checks some custom params - such as the level of the ID which
could be rapresented by a number at the beginning of the `clientID` - and then generates
a `token` encrypted with the current $K_{pulse}$

``` JSON 
"token": {
    "tokenid": "[a random generated number]",
    "key": "[the client key]", 
    "data": { 
      "... specific to the client's intent" 
      }
}
```
##### note: tokenid currently only has future-proofing potential

3. The client - unaware of the content in the token - sends the data to the *Data server*

4. *Data server* decrypts the token and performs the depicted operation - DBMS manages
the eventual exception coming from a **reply attack**.

``` JSON 
"response": {
      "result": "[the result of the operation encrypted with the client key]"
}
```
---
### Idempotent operations
> this time the client communicates directly with the Data server

1. the client sends the Data server a `collect` message:

``` JSON
  "collect": {
    "clientid": "[specific to the client's intent encrypted with the client key]",
    "key": "[the client key encrypted with public key]"
}
```
2. the Data server replies with the `response` message once again

``` JSON 
"response": {
      "result": "[the result of the operation encrypted with the client key]"
}
```
---

## Vulnerabilities
As of today 10/05/2026, the only vulnerability discovered lies in the moment when a client requests the main page from the Web server: it has to send its client id over the internet alone.
Yet, in practice this is easly solved with the use of `TLS` for this very first message exchange.

---

## Perfomance enhancements
The following list contains simple - yet effective - features to implement in order to increase the overall throughput of the architecture - and, in some cases, its footprint.

- **Dispatcher tier**: a simple proxy-like element whose role is to route the `token` messages, coming from the Web server, direcly to the Data server - if the client is further away in space.

- **FPGAs or ASICs**: implement the whole encryption/decryption layer at the hardware level to free up CPU time and optimize the speed of the system. Perhaps this can also make the keys even more secure as they would live only at the circuit level.

---

## Migration
In GhostArchitecture, Data Servers are disposable commodities. Because security is anchored in the Client's Key and the Web Server's Pulse, data is location-agnostic. This enables 'Follow-the-Sun' migration and Multi-Node Sharding, where a single account's data can be distributed across various servers and moved at the user's discretion. The user can physically 'pull' their data to a local node for speed or 'push' it to a specific jurisdiction for privacy — all without the infrastructure ever seeing the raw content.
