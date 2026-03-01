package org.gestion.application.dto.response;

import lombok.Builder;
import lombok.Data;
import org.gestion.domain.enums.EstadoPoliza;
import org.gestion.domain.enums.TipoPoliza;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class PolizaResponse {

    private Long id;
    private String numeroPoliza;
    private TipoPoliza tipo;
    private EstadoPoliza estado;
    private String tomador;
    private String asegurado;
    private String beneficiario;
    private LocalDate fechaInicioVigencia;
    private LocalDate fechaFinVigencia;
    private Integer mesesVigencia;
    private BigDecimal valorCanon;
    private BigDecimal valorPrima;
    private Integer contadorRenovaciones;
    private Integer totalRiesgosActivos;
}

