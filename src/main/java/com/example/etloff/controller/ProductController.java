package com.example.etloff.controller;

import com.example.etloff.dto.ProductDto;
import com.example.etloff.model.Additif;
import com.example.etloff.model.Allergene;
import com.example.etloff.model.Ingredient;
import com.example.etloff.model.Product;
import com.example.etloff.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;

    @GetMapping("/top-by-brand")
    @Transactional(readOnly = true)
    public List<ProductDto> getTopByBrand(
            @RequestParam String brand,
            @RequestParam(defaultValue = "10") int limit) {
        
        Pageable pageable = PageRequest.of(0, limit);
        List<Product> products = productRepository.findByBrandNomIgnoreCaseOrderByNutritionGradeFrAsc(brand, pageable);
        return products.stream().map(this::toDto).collect(Collectors.toList());
    }

    @GetMapping("/top-by-category")
    @Transactional(readOnly = true)
    public List<ProductDto> getTopByCategory(
            @RequestParam String category,
            @RequestParam(defaultValue = "10") int limit) {
        
        Pageable pageable = PageRequest.of(0, limit);
        List<Product> products = productRepository.findByCategoryNomIgnoreCaseOrderByNutritionGradeFrAsc(category, pageable);
        return products.stream().map(this::toDto).collect(Collectors.toList());
    }

    @GetMapping("/top-by-brand-category")
    @Transactional(readOnly = true)
    public List<ProductDto> getTopByBrandCategory(
            @RequestParam String brand,
            @RequestParam String category,
            @RequestParam(defaultValue = "10") int limit) {
        
        Pageable pageable = PageRequest.of(0, limit);
        List<Product> products = productRepository.findByBrandNomIgnoreCaseAndCategoryNomIgnoreCaseOrderByNutritionGradeFrAsc(brand, category, pageable);
        return products.stream().map(this::toDto).collect(Collectors.toList());
    }

    private ProductDto toDto(Product p) {
        return ProductDto.builder()
                .id(p.getId())
                .nom(p.getNom())
                .nutritionGradeFr(p.getNutritionGradeFr())
                .category(p.getCategory() != null ? p.getCategory().getNom() : null)
                .brand(p.getBrand() != null ? p.getBrand().getNom() : null)
                .energie100g(p.getEnergie100g())
                .graisse100g(p.getGraisse100g())
                .sucres100g(p.getSucres100g())
                .fibres100g(p.getFibres100g())
                .proteines100g(p.getProteines100g())
                .sel100g(p.getSel100g())
                .presenceHuilePalme(p.getPresenceHuilePalme())
                .ingredients(p.getIngredients().stream().map(Ingredient::getNom).collect(Collectors.toList()))
                .allergens(p.getAllergens().stream().map(Allergene::getNom).collect(Collectors.toList()))
                .additives(p.getAdditives().stream().map(Additif::getNom).collect(Collectors.toList()))
                .build();
    }
}
