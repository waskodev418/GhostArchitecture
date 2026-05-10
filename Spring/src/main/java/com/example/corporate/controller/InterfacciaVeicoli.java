package com.example.corporate.controller;
import com.example.corporate.model.filtri.Filtro;
import com.example.corporate.model.Veicolo;
import com.example.corporate.service.Manager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Constructor;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/concessionario")
public class InterfacciaVeicoli {

    private final Manager SERVICE;
    public InterfacciaVeicoli(Manager service){
        this.SERVICE = service;
    }

    @GetMapping("/veicoli")
    public ResponseEntity<List<Veicolo>> getAll(){
        var res = SERVICE.getAllVeicoli();
        return ResponseEntity.ok(res);
    }

    @GetMapping("/veicoli/{id}")
    public  ResponseEntity<Veicolo> getVeicolo(@PathVariable Long id){
        var res = SERVICE.getVeicoli(id);
        return (res == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(res);
    }

    @PostMapping("/veicoli/filtro")
    public ResponseEntity<List<Veicolo>> getVeicolo(@RequestBody FiltroRequest richiesta){
        try {
            String tipo = richiesta.tipo().substring(0, 1).toUpperCase() +
                    richiesta.tipo().substring(1).toLowerCase();

            String nomeFiltro = "com.example.corporatev2.com.example.corporate.model.filtri.Filtro" + tipo.trim();

            Class<?> classe = Class.forName(nomeFiltro);
            Constructor<?> constructor = classe.getConstructor(richiesta.valore().getClass());
            Filtro<?> filtro = (Filtro<?>) constructor.newInstance(richiesta.valore());

            var res = SERVICE.getVeicoloByFilter(filtro);
            return (res == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/veicoli")
    public ResponseEntity<Boolean> addVeicolo(@RequestBody Veicolo veicolo) {
        boolean res = SERVICE.addVeicolo(veicolo);
        return (!res) ? ResponseEntity.notFound().build() : ResponseEntity.ok(res);
    }

    @PutMapping("/veicoli/{id}")
    public ResponseEntity<Boolean> modifyVeicolo(@PathVariable Long id, @RequestBody Veicolo veicolo){
        boolean res = SERVICE.modifyVeicolo(id, veicolo);
        return (!res) ? ResponseEntity.notFound().build() : ResponseEntity.ok(res);
    }

    @DeleteMapping("/veicoli/{id}")
    public void deleteVeicolo(@PathVariable long id){
        SERVICE.removeVeicolo(id);
    }
}
