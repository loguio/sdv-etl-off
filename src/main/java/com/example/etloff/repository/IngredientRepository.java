package com.example.etloff.repository;

import com.example.etloff.model.Ingredient;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    Optional<Ingredient> findByNom(String nom);

    @Query("SELECT i.nom as name, COUNT(p) as count FROM Product p JOIN p.ingredients i GROUP BY i.nom ORDER BY COUNT(p) DESC")
    List<Object[]> findTopIngredients(Pageable pageable);
}
