package com.example.corporate.repository;

import com.example.corporate.model.Veicolo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VeicoloRepository extends JpaRepository<Veicolo, Long> {
    List<Veicolo> findByMarca(String marca);
}
