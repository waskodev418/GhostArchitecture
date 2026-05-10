package com.example.corporate.model;

import jakarta.persistence.*;

@Entity
@Table(name = "veicoli")
public class Veicolo{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String targa;
    private String marca;
    private String modello;
    private String tipologia;

    public Veicolo(){}

    public Veicolo(Long id, String targa, String marca,String modello, String tipologia) {
        setId(id);
        setTarga(targa);
        setMarca(marca);
        setModello(modello);
        setTipologia(tipologia);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTarga(String targa) {
        this.targa = targa;
    }

    public String getTarga() {
        return targa;
    }

    public String getTipologia() {
        return tipologia;
    }

    public void setTipologia(String tipologia) {
        this.tipologia = tipologia;
    }

    public String getModello() {
        return modello;
    }

    public void setModello(String modello) {
        this.modello = modello;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }
}

