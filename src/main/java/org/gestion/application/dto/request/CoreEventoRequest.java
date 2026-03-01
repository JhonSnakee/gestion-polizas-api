package org.gestion.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CoreEventoRequest {

    @NotBlank(message = "El evento es obligatorio")
    private String evento;

    @NotNull(message = "El ID de póliza es obligatorio")
    private Long polizaId;
}

