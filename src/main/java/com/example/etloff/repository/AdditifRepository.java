package com.example.etloff.repository;

import com.example.etloff.model.Additif;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdditifRepository extends JpaRepository<Additif, Long> {
    Optional<Additif> findByNom(String nom);

    @Query("SELECT ad.nom as name, COUNT(p) as count FROM Product p JOIN p.additives ad GROUP BY ad.nom ORDER BY COUNT(p) DESC")
    List<Object[]> findTopAdditives(Pageable pageable);
}
