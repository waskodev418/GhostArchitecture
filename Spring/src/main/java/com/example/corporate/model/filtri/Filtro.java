package com.example.corporate.model.filtri;

public abstract class Filtro<F> {

    private final String TIPO;
    private F filtro;

    public Filtro(String tipo, F filtro){
        this.TIPO = tipo;
        this.filtro = filtro;
    }

    public String getTIPO() {
        return TIPO;
    }

    public F getFiltro() {
        return filtro;
    }

    public void setFiltro(F filtro) {
        this.filtro = filtro;
    }
}
