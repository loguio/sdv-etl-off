package com.example.etloff.repository;

import com.example.etloff.model.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByBrandNomIgnoreCaseOrderByNutritionGradeFrAsc(String brandNom, Pageable pageable);

    List<Product> findByCategoryNomIgnoreCaseOrderByNutritionGradeFrAsc(String categoryNom, Pageable pageable);

    List<Product> findByBrandNomIgnoreCaseAndCategoryNomIgnoreCaseOrderByNutritionGradeFrAsc(String brandNom, String categoryNom, Pageable pageable);
}
