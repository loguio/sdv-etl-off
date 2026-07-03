package com.example.etloff.repository;

import com.example.etloff.model.Allergene;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AllergeneRepository extends JpaRepository<Allergene, Long> {
    Optional<Allergene> findByNom(String nom);

    @Query("SELECT a.nom as name, COUNT(p) as count FROM Product p JOIN p.allergens a GROUP BY a.nom ORDER BY COUNT(p) DESC")
    List<Object[]> findTopAllergens(Pageable pageable);
}
