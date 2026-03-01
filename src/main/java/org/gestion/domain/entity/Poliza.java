package org.gestion.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.gestion.domain.enums.EstadoPoliza;
import org.gestion.domain.enums.TipoPoliza;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "polizas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Poliza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String numeroPoliza;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoPoliza tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPoliza estado;

    /** Nombre del tomador (inmobiliaria/copropiedad para colectivas, arrendatario para individuales) */
    @Column(nullable = false, length = 200)
    private String tomador;

    /** Nombre del asegurado */
    @Column(nullable = false, length = 200)
    private String asegurado;

    /** Nombre del beneficiario (arrendador) */
    @Column(nullable = false, length = 200)
    private String beneficiario;

    /** Fecha de inicio de la vigencia */
    @Column(nullable = false)
    private LocalDate fechaInicioVigencia;

    /** Fecha de fin de la vigencia */
    @Column(nullable = false)
    private LocalDate fechaFinVigencia;

    /** Número de meses de la vigencia */
    @Column(nullable = false)
    private Integer mesesVigencia;

    /** Valor del canon mensual de arrendamiento */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorCanon;

    /** Prima = valorCanon * mesesVigencia */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorPrima;

    @OneToMany(mappedBy = "poliza", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Riesgo> riesgos = new ArrayList<>();

    /** Número de renovaciones realizadas */
    @Column(nullable = false)
    @Builder.Default
    private Integer contadorRenovaciones = 0;
}

