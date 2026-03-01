package org.gestion.application.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AgregarRiesgoRequest {

    @NotBlank(message = "La descripción del riesgo es obligatoria")
    private String descripcion;

    @NotBlank(message = "La dirección del inmueble es obligatoria")
    private String direccionInmueble;

    @NotBlank(message = "El nombre del arrendatario es obligatorio")
    private String nombreArrendatario;

    @NotNull(message = "La fecha de inicio del riesgo es obligatoria")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin del riesgo es obligatoria")
    private LocalDate fechaFin;

    @NotNull(message = "El valor asegurado es obligatorio")
    @DecimalMin(value = "0.01", message = "El valor asegurado debe ser mayor a cero")
    private BigDecimal valorAsegurado;
}

