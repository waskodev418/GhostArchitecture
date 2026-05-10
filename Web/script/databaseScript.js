const urlDB = "http://localhost:9876/concessionario";
const urlPHP = "function/tokenGenerator.php";

export async function http_GET(url = "/veicoli"){
    const response = await fetch(urlDB + url,
    {
        method: "GET"  
    });
    
    if(response.ok) 
        return await response.json(); 
    else 
        throw new Error(response.statusText);
}

export async function http_POST(jsonData, url = "/prenotazioni/aggiungi"){
    const response = await fetch(urlDB + url,
    {
        method: "POST",
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(jsonData)
    });

    if(response.ok) 
        return await response.json(); 
    else 
        throw new Error(response.statusText);
}

export async function http_request_action(data){
    const response = await fetch(urlPHP,
    {
        method: "POST",
        headers:{
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data) 
    });

    if(response.ok) 
        return await response.json(); 
    else 
        throw new Error(response.statusText);
}
