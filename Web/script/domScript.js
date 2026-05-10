import { asym_encrypt, decrypt, encrypt } from "./libraries/encrypt.js";
import * as Connection from "./databaseScript.js";

const dom = document.getElementById("response");

export async function getAll(output = dom, callback = async li => {
    const id = li.getAttribute("id");

    await getById(id);
    history.pushState(null, null, '?id=' + id);
}){

    let lista;
    try {
        lista = await Connection.http_GET();
    } catch (error) {
        alert("veicoli non disponibili :/");
        return;
    }
    let container;

    if(lista.length == 0){
        container = document.createElement("h3");
        container.innerText = "nessuna prenotazione disponibile";
    }else{
        container = document.createElement("ul");
        lista.forEach(veicolo => {
            
            const li = document.createElement("li");
            li.setAttribute("id", veicolo.id);
            li.onclick = async () => {
                callback(li);
            };
            
            li.innerHTML = toString(veicolo);

            container.append(li);
        });
    }

    output.innerHTML = "";
    output.append(container);
}

function toString(veicolo){

    const content = "<li>Marca: " + veicolo.marca + "</li>" +
                    "<li>Modello: " + veicolo.modello + "</li>" +
                    "<li>Tipologia: " + veicolo.tipologia + "</li>" +
                    "<li>Targa: " + veicolo.targa + "</li>";
                
    return "<h3>ID: " +veicolo.id +"</h3><ul>" + content +"</ul>"; 
}

export async function getById(id, output = dom){

    let veicolo;
    try {
        veicolo = await Connection.http_GET("/veicoli/" + id);
    } catch (error) {
        alert("id non trovato :/");
        return;
    }
    
    output.innerHTML = toString(veicolo);
    formPrenota(veicolo, output);
}

function createInput(nome, type = "text", required = false,  valore = ""){
    const div = document.createElement("div")

    const label = document.createElement("label");
    label.setAttribute("for", nome);
    label.innerText = nome + ": ";
    div.append(label);

    const input = document.createElement("input");
    input.id = nome;
    input.name = nome;
    input.type = type;
    input.value = valore;
    input.required = required;
    div.append(input);
    return div;
}

async function getCookie(name){
    const cookie = await cookieStore.get(name);
    if(!cookie) throw new Error(name + " non trovato");
    return decodeURIComponent(cookie.value);
}

function formPrenota(veicolo, output = dom, callback = prenotazione){

    const form = document.createElement("form");
    const id = createInput("idVeicolo", "number", true, veicolo.id);
    id.hidden = true;
    id.readOnly = true;
    form.append(id);

    let input = createInput("nomePrenotazione", "text", true);
    form.append(input);

    const oggi = new Date().toISOString().split('T')[0];

    let input2 = createInput("dataInizio", "date", true, oggi);
    input2.lastChild.setAttribute("min", oggi);
    form.append(input2);

    let input3 = createInput("dataFine", "date",true);
    input3.lastChild.setAttribute("min", oggi);
    form.append(input3);

    let submit = createInput("invio", "submit", false, "prenota");
    form.append(submit.lastChild);

    form.onsubmit = async (e) => {
        e.preventDefault();
        const formData = new FormData(form);
        formData.append("action", 1);
        
        // 1. Convert FormData to a plain object
        const vault = {
            "data": Object.fromEntries(formData.entries()),
            "id": await getCookie("clientid")
        } 
        await callback(vault);
    };

    output.append(document.createElement("hr"))
    output.append(form);
}

async function prenotazione(vault) {

    const {payload, key} = await encrypt(JSON.stringify(vault));
   
    const param = {
        "vault": payload,
        "key": await asym_encrypt(key)
    };
    try{
        //get the token from the web server
        const token = await Connection.http_request_action({"solicit": param});
        
        //send the token to the DB
        const sendDB = await Connection.http_POST(token);
        const raw_data = await sendDB.response;

        const response = await decrypt(raw_data, key);
        if(response.result) alert("prenotazione aggiunta con successo!");
        else throw new Error();
    }catch(error){
        alert("errore: impossibile prenotare :/");
    }    
}

async function getPrenotazioni(){
    
    const clientid = await getCookie("clientid");
    const {payload, key} = await encrypt(clientid);
    const collect = {
        "collect": {
            "clientid": payload,
            "key": await asym_encrypt(key)
        }
    };

    const raw_response = await Connection.http_POST(collect, "/prenotazioni");
    const response = await decrypt(await raw_response.response, key);
    return response.result;
}



export async function showPrenotazioni(output = dom){
    history.pushState(null, null, '?prenotazioni=show');
    let prenotazioni;
    try{
        prenotazioni = await getPrenotazioni();
    } catch(error){
        alert("impossibile mostrare le prenotazioni :/");
        return;
    }

    let container;
    if(prenotazioni.length == 0){
        container = document.createElement("h3");
        container.innerText = "nessuna prenotazione disponibile";
    }else{
        container = document.createElement("ul");
        prenotazioni.forEach(p => {
            
            const li = document.createElement("li");
            const content = "<h2>prenotazione: " + p.nome + "</h2>" +
                            "<ul><li>Da: " + p.dataInizio + "</li>" +
                            "<li>A: " + p.dataFine + "</li>" +
                            "<li>Veicolo: " + toString(p.veicolo) + "</li></ul>";
                    
            li.innerHTML = content;
            container.append(li);
        });
    }

    dom.innerHTML = "";
    dom.append(container);
}

document.getElementById("footer_home").addEventListener("click", async () => await getAll())
document.getElementById("footer_prenotazioni").addEventListener("click", async () => await showPrenotazioni())