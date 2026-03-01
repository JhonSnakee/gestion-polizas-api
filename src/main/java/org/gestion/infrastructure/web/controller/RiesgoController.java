package org.gestion.infrastructure.web.controller;

import lombok.RequiredArgsConstructor;
import org.gestion.application.dto.response.ApiResponse;
import org.gestion.application.dto.response.RiesgoResponse;
import org.gestion.application.service.PolizaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para operaciones directas sobre riesgos.
 * Requiere header: x-api-key: 123456
 */
@RestController
@RequestMapping("/api/v1/riesgos")
@RequiredArgsConstructor
public class RiesgoController {

    private final PolizaService polizaService;

    /**
     * POST /api/v1/riesgos/{id}/cancelar
     * Cancela un riesgo individual de una póliza COLECTIVA.
     */
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ApiResponse<RiesgoResponse>> cancelarRiesgo(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Riesgo cancelado exitosamente",
                polizaService.cancelarRiesgo(id)));
    }
}

