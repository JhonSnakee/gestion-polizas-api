package org.gestion.application.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RenovarPolizaRequest {

    @NotNull(message = "El porcentaje de IPC es obligatorio")
    @DecimalMin(value = "0.01", message = "El IPC debe ser mayor a cero")
    @DecimalMax(value = "100.00", message = "El IPC no puede superar el 100%")
    private BigDecimal ipcPorcentaje;
}

