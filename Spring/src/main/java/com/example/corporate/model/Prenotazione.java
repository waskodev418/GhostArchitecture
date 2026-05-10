package com.example.corporate.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "prenotazioni")
public class Prenotazione {
    @Id
    @JsonIgnore
    private String id;
    @Column(name = "dipendente_nome")
    private String nome;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Column(name = "data_inizio")
    private LocalDate dataInizio;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Column(name = "data_fine")
    private LocalDate dataFine;
    @ManyToOne
    @JoinColumn(name = "veicolo_id")
    private Veicolo veicolo;

    public Prenotazione(String id, String nome, LocalDate dataInizio, LocalDate dataFine, Veicolo veicolo){
        setId(id);
        setDataFine(dataFine);
        setDataInizio(dataInizio);
        setNome(nome);
        setVeicolo(veicolo);
    }

    public Prenotazione() {

    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public LocalDate getDataInizio() {
        return dataInizio;
    }

    public void setDataInizio(LocalDate dataInizio) {
        this.dataInizio = dataInizio;
    }

    public LocalDate getDataFine() {
        return dataFine;
    }

    public void setDataFine(LocalDate dataFine) {
        this.dataFine = dataFine;
    }

    public Veicolo getVeicolo() {
        return veicolo;
    }

    public void setVeicolo(Veicolo veicolo) {
        this.veicolo = veicolo;
    }
}

