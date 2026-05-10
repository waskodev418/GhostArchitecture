<?php
    require_once "encrypt.inc.php";

    if($_SERVER["REQUEST_METHOD"] != "POST"){
        http_response_code(401);
        exit;
    }
    
    $json = file_get_contents('php://input');
    $solicit = json_decode($json, true)["solicit"];

    $key = decrypt_asymmetric($solicit["key"]);

    if(!$key){
        http_response_code(400);
        exit;
    }

    $vault = decrypt_client($solicit["vault"], $key);

    if(!$vault){
        http_response_code(400);
        exit;
    }

    $vault = json_decode($vault, true);

    $clientID = decrypt_asymmetric($vault["id"]);

    if(!$clientID){
        http_response_code(400);
        echo $vault["id"];
        exit;
    }

    $dati = [
        "nome"   => $vault["data"]["nomePrenotazione"] ?? null,
        "inizio" => $vault["data"]["dataInizio"] ?? null,
        "fine"   => $vault["data"]["dataFine"] ?? null,
        "idVeicolo" => $vault["data"]["idVeicolo"] ?? null,
        "action" => $vault["data"]["action"] ?? null,
        "key"    => $key ?? null,
        "client" => $clientID ?? null
    ];

    if (in_array(null, $dati, true)) {
        http_response_code(400);
        exit;
    }
    
    header('Content-Type: application/json');
    
    if($dati["action"] == 1){ // add
        echo json_encode(["token" => genTokenAdd($dati)]);
    }elseif($dati["action"] == 2){ // remove (ONLY TO SHOW the potential of the token management for multiple purposes)
        //The client might just send the unique infos of a record - since it can not access the ID -
        //such as - in this case - the name and the date of the booking.
        //This very same logic can apply into modifying a record.
        echo json_encode(["token" => genTokenRem($dati)]);
    }
    
    exit;

// ---------------------------------------------------------------
// -------------------- funzioni ---------------------------------
// ---------------------------------------------------------------

    function genNonce(){
        $permitted_chars = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
        $N = random_int(0, 20);
        $s = "";
        while($N-- > 0)$s .= $permitted_chars[random_int(0, 61)];
        return $s;
    }//If we don't know one of the hashes we can not find the other (id record = nonce + ID utente + nonce + hash)
    
    function genTokenAdd($dati){
        $json = [
            "tokenid" => random_int(0, 100) . time(),
            "key" => $dati["key"], 
            "data" => [
                "idRecord" => genNonce() . $dati["client"] . genNonce() . $dati["idVeicolo"],
                "dataInizio" => $dati["inizio"],
                "dataFine" => $dati["fine"],
                "idVeicolo" => $dati["idVeicolo"],
                "nome" => $dati["nome"],
            ]
        ];

        return genToken(json_encode($json));
    }

    function genTokenRem($dati){
        $json = [
            "tokenid" => random_int(0, 100) . time(),
            "key" => $dati["key"], 
            "data" => [
                "idRecord" => $dati["client"] . genNonce() . $dati["idVeicolo"]
            ]
        ];

        return genToken(json_encode($json));
    }
    
    function genToken($string){
        $data = readPulse();
        if($data == null){
            sleep(0.5); //eventually wait for pulse_daemon to finish writing
            $data = readPulse();
            if($data == null){
                throw new Error("impossibile caricare le chiavi");
            }
        }
        
        $key = hash_hkdf('sha256', $data["key1"], 32, 'encryption');
        $macKey = hash_hkdf('sha256', $data["key2"], 32, 'auth');

        $method = "AES-256-CBC";
        $ivLength = openssl_cipher_iv_length($method);
        $iv = openssl_random_pseudo_bytes($ivLength);
        
        $token = openssl_encrypt($string, $method, $key, OPENSSL_RAW_DATA, $iv);
        $payload = $iv . $token;
        $mac = hash_hmac('sha256', $payload, $macKey, true);
        
        return base64_encode($mac . $payload);
    }

    
?>