package com.example.etloff.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "product")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"ingredients", "allergens", "additives"})
@EqualsAndHashCode(exclude = {"ingredients", "allergens", "additives"})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_generator")
    @SequenceGenerator(name = "product_generator", sequenceName = "product_seq", allocationSize = 1000)
    private Long id;

    private String nom;
    private String nutritionGradeFr;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Categorie category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Marque brand;

    private Double energie100g;
    private Double graisse100g;
    private Double sucres100g;
    private Double fibres100g;
    private Double proteines100g;
    private Double sel100g;

    private Boolean presenceHuilePalme;

    // Vitamins and minerals
    private Double vitA100g;
    private Double vitD100g;
    private Double vitE100g;
    private Double vitK100g;
    private Double vitC100g;
    private Double vitB1100g;
    private Double vitB2100g;
    private Double vitPP100g;
    private Double vitB6100g;
    private Double vitB9100g;
    private Double vitB12100g;
    private Double calcium100g;
    private Double magnesium100g;
    private Double iron100g;
    private Double fer100g;
    private Double betaCarotene100g;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "product_ingredients",
        joinColumns = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "ingredient_id"),
        indexes = {
            @Index(name = "idx_product_ing_product", columnList = "product_id"),
            @Index(name = "idx_product_ing_ingredient", columnList = "ingredient_id")
        }
    )
    @Builder.Default
    private Set<Ingredient> ingredients = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "product_allergens",
        joinColumns = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "allergene_id"),
        indexes = {
            @Index(name = "idx_product_alg_product", columnList = "product_id"),
            @Index(name = "idx_product_alg_allergene", columnList = "allergene_id")
        }
    )
    @Builder.Default
    private Set<Allergene> allergens = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "product_additives",
        joinColumns = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "additif_id"),
        indexes = {
            @Index(name = "idx_product_add_product", columnList = "product_id"),
            @Index(name = "idx_product_add_additif", columnList = "additif_id")
        }
    )
    @Builder.Default
    private Set<Additif> additives = new HashSet<>();
}
