package com.example.etloff.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categorie", indexes = {@Index(name = "idx_categorie_nom", columnList = "nom", unique = true)})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Categorie {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "categorie_generator")
    @SequenceGenerator(name = "categorie_generator", sequenceName = "categorie_seq", allocationSize = 100)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;
}
