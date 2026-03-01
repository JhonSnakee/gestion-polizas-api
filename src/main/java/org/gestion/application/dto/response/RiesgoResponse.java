package org.gestion.application.dto.response;

import lombok.Builder;
import lombok.Data;
import org.gestion.domain.enums.EstadoRiesgo;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class RiesgoResponse {

    private Long id;
    private String descripcion;
    private String direccionInmueble;
    private String nombreArrendatario;
    private EstadoRiesgo estado;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private BigDecimal valorAsegurado;
    private Long polizaId;
}

