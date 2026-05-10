<?php
    require_once "include_php/context.inc.php";
    require_once "function/encrypt.inc.php";

    if(!isset($_COOKIE["clientid"]))
    {
        $id_prova = time() . "abc" . random_int(0, 999); //solo per il test
        setcookie("clientid", encrypt_asymmetric($id_prova), time() + 3600 * 24 * 30, "/index.php"); // shall live only over HTTPS, but for texting also HTTP is fine
    }
?>
<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>homepage</title>
</head>
<body>
    <header>
    <h1>prenota veicolo</h1>
    </header>
    <hr>

    <main id="response">
    </main>

    <!-- <footer>   -->
    <?php include_once "include_php/footer.inc.php" ?>
    <!-- </footer>  -->
    
    <script type="module" src="script/domScript.js"></script>
    <script type="module">
        import * as Dom from "./script/domScript.js";
        Dom.<?= getContext() ?>
    </script>
</body>
</html>