package org.gestion.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.gestion.domain.enums.EstadoRiesgo;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "riesgos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Riesgo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String descripcion;

    /** Dirección del inmueble asegurado */
    @Column(nullable = false, length = 300)
    private String direccionInmueble;

    /** Nombre del arrendatario asegurado en este riesgo */
    @Column(nullable = false, length = 200)
    private String nombreArrendatario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRiesgo estado;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorAsegurado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poliza_id", nullable = false)
    private Poliza poliza;
}

