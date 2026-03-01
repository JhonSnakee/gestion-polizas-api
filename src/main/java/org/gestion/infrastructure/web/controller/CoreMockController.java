package org.gestion.infrastructure.web.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.gestion.application.dto.request.CoreEventoRequest;
import org.gestion.application.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Mock del endpoint del CORE transaccional legado.
 * Simula el servicio agnóstico de edición disponibilizado
 * a través de capa media en WebLogic.
 *
 * NO requiere x-api-key (excluido del filtro de seguridad).
 */
@Slf4j
@RestController
@RequestMapping("/core-mock")
public class CoreMockController {

    /**
     * POST /core-mock/evento
     * Simula la recepción de un evento desde la API de pólizas hacia el CORE.
     * Registra en logs que la operación se intentó enviar al sistema CORE.
     */
    @PostMapping("/evento")
    public ResponseEntity<ApiResponse<Void>> recibirEvento(
            @Valid @RequestBody CoreEventoRequest request) {

        log.info("[CORE-MOCK] ✅ Evento recibido del sistema de pólizas -> evento: {}, polizaId: {}",
                request.getEvento(), request.getPolizaId());
        log.info("[CORE-MOCK] Simulando procesamiento en sistema CORE legado (WebLogic)...");
        log.info("[CORE-MOCK] Operación registrada exitosamente para polizaId: {}", request.getPolizaId());

        return ResponseEntity.ok(ApiResponse.ok(
                "Evento registrado en CORE. evento=" + request.getEvento()
                + ", polizaId=" + request.getPolizaId(), null));
    }
}

