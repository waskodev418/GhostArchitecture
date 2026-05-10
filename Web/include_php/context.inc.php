<?php 
function getContext(): string {

    if(isset($_GET["id"])){
        return 'getById(' . $_GET["id"] .');';
    } else if(isset($_GET["prenotazioni"])){
        return 'showPrenotazioni();';
    } else {
        return 'getAll();';
    }
}
?>