package com.example.etloff.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ProductDto {
    private Long id;
    private String nom;
    private String nutritionGradeFr;
    private String category;
    private String brand;
    private Double energie100g;
    private Double graisse100g;
    private Double sucres100g;
    private Double fibres100g;
    private Double proteines100g;
    private Double sel100g;
    private Boolean presenceHuilePalme;
    private List<String> ingredients;
    private List<String> allergens;
    private List<String> additives;
}
