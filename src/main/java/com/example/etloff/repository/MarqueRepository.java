package com.example.etloff.repository;

import com.example.etloff.model.Marque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MarqueRepository extends JpaRepository<Marque, Long> {
    Optional<Marque> findByNom(String nom);
}
