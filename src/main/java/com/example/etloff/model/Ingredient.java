package com.example.etloff.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ingredient", indexes = {@Index(name = "idx_ingredient_nom", columnList = "nom", unique = true)})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ingredient_generator")
    @SequenceGenerator(name = "ingredient_generator", sequenceName = "ingredient_seq", allocationSize = 1000)
    private Long id;

    @Column(nullable = false, unique = true, length = 1000)
    private String nom;
}
