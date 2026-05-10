<?php
    header("Content-Type: application/javascript");
    $publicKey = file_get_contents("../../function/public.pem");
?>

export const PUBLIC_KEY = <?= json_encode($publicKey) ?>;