package com.example.etloff.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "additif", indexes = {@Index(name = "idx_additif_nom", columnList = "nom", unique = true)})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Additif {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "additif_generator")
    @SequenceGenerator(name = "additif_generator", sequenceName = "additif_seq", allocationSize = 500)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;
}
