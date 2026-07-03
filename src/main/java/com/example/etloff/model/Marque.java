package com.example.etloff.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "marque", indexes = {@Index(name = "idx_marque_nom", columnList = "nom", unique = true)})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Marque {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "marque_generator")
    @SequenceGenerator(name = "marque_generator", sequenceName = "marque_seq", allocationSize = 100)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;
}
