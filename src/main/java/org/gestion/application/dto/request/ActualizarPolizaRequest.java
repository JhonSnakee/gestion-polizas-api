package org.gestion.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para modificar datos de una póliza existente.
 * Solo se permiten modificar campos que no comprometen la integridad
 * del contrato de seguro (no se puede cambiar tipo, ni número de póliza).
 * Todos los campos son opcionales: solo se actualiza lo que se envía (patch semántico).
 */
@Data
public class ActualizarPolizaRequest {

    private String tomador;

    private String asegurado;

    private String beneficiario;

    @Min(value = 1, message = "La vigencia minima es 1 mes")
    private Integer mesesVigencia;

    @DecimalMin(value = "0.01", message = "El valor del canon debe ser mayor a cero")
    private BigDecimal valorCanon;

    private LocalDate fechaInicioVigencia;
}

