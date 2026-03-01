package org.gestion.infrastructure.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PolizaNotFoundException extends RuntimeException {

    public PolizaNotFoundException(Long id) {
        super("Póliza no encontrada con ID: " + id);
    }
}

