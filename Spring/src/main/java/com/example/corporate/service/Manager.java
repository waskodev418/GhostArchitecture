package com.example.corporate.service;

import com.example.corporate.model.filtri.Filtro;
import com.example.corporate.model.Prenotazione;
import com.example.corporate.model.Veicolo;
import com.example.corporate.model.security.ServerToken;
import com.example.corporate.repository.PrenotazioneRepository;
import com.example.corporate.repository.VeicoloRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;

@Service
public class Manager {
    private final PrenotazioneRepository PRENOTAZIONI;
    private final VeicoloRepository VEICOLI;

    public Manager(PrenotazioneRepository p, VeicoloRepository v){
        PRENOTAZIONI = p;
        VEICOLI = v;
    }

    public List<Veicolo> getAllVeicoli(){
        return VEICOLI.findAll();
    }

    public Veicolo getVeicoli(Long id){
        var res = VEICOLI.findById(id);
        return res.orElse(null);
    }

    @Transactional
    public boolean addVeicolo(Veicolo v){
        if(VEICOLI.findById(v.getId()).isEmpty()){
            VEICOLI.save(v);
            return true;
        } else{
            return false;
        }
    }

    @Transactional
    public boolean modifyVeicolo(Long id, Veicolo v){

        if(VEICOLI.findById(id).isPresent()){
            v.setId(id);
            VEICOLI.save(v);
            return true;
        } else{
            return false;
        }
    }


    public void removeVeicolo(Long id){
        VEICOLI.deleteById(id);
    }

    public boolean addPrenotazione(ServerToken token){
        try{
            long idVeicolo = Long.parseLong(token.getData("idVeicolo"));
            Veicolo veicolo = VEICOLI.findById(idVeicolo).orElse(null);
            if (veicolo == null) {
                return false;
            }

            var p = new Prenotazione(
                    token.getData("idRecord"),
                    token.getData("nome"),
                    LocalDate.parse(token.getData("dataInizio")),
                    LocalDate.parse(token.getData("dataFine")),
                    veicolo
            );
            PRENOTAZIONI.save(p);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    public List<Prenotazione> getAllPrenotazioni(String id){
        return PRENOTAZIONI.findByIdContaining(id);
    }

    public List<Veicolo> getVeicoloByFilter(Filtro<?> filtro) {
        String metodo = "findBy" + filtro.getTIPO();
        Class<?> classe = VEICOLI.getClass();

        try {
            Method m = classe.getMethod(metodo, filtro.getFiltro().getClass());
            return (List<Veicolo>) m.invoke(VEICOLI, filtro.getFiltro());
        } catch (Exception e) {
            return null;
        }
    }
}
