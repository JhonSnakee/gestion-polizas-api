package org.gestion.application.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.gestion.domain.enums.TipoPoliza;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CrearPolizaRequest {

    @NotBlank(message = "El número de póliza es obligatorio")
    private String numeroPoliza;

    @NotNull(message = "El tipo de póliza es obligatorio")
    private TipoPoliza tipo;

    @NotBlank(message = "El tomador es obligatorio")
    private String tomador;

    @NotBlank(message = "El asegurado es obligatorio")
    private String asegurado;

    @NotBlank(message = "El beneficiario es obligatorio")
    private String beneficiario;

    @NotNull(message = "La fecha de inicio de vigencia es obligatoria")
    private LocalDate fechaInicioVigencia;

    @NotNull(message = "Los meses de vigencia son obligatorios")
    @Min(value = 1, message = "La vigencia mínima es 1 mes")
    private Integer mesesVigencia;

    @NotNull(message = "El valor del canon es obligatorio")
    @DecimalMin(value = "0.01", message = "El valor del canon debe ser mayor a cero")
    private BigDecimal valorCanon;
}

