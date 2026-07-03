package com.example.etloff.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "allergene", indexes = {@Index(name = "idx_allergene_nom", columnList = "nom", unique = true)})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Allergene {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "allergene_generator")
    @SequenceGenerator(name = "allergene_generator", sequenceName = "allergene_seq", allocationSize = 500)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;
}
