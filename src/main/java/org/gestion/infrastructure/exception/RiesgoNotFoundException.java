package org.gestion.infrastructure.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class RiesgoNotFoundException extends RuntimeException {

    public RiesgoNotFoundException(Long id) {
        super("Riesgo no encontrado con ID: " + id);
    }
}

