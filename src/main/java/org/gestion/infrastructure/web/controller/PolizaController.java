package org.gestion.infrastructure.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.gestion.application.dto.request.ActualizarPolizaRequest;
import org.gestion.application.dto.request.AgregarRiesgoRequest;
import org.gestion.application.dto.request.CrearPolizaRequest;
import org.gestion.application.dto.request.RenovarPolizaRequest;
import org.gestion.application.dto.response.ApiResponse;
import org.gestion.application.dto.response.PolizaResponse;
import org.gestion.application.dto.response.RiesgoResponse;
import org.gestion.application.service.PolizaService;
import org.gestion.domain.enums.EstadoPoliza;
import org.gestion.domain.enums.TipoPoliza;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de pólizas.
 * Requiere header: x-api-key: 123456
 */
@RestController
@RequestMapping("/api/v1/polizas")
@RequiredArgsConstructor
public class PolizaController {

    private final PolizaService polizaService;

    /**
     * GET /api/v1/polizas?tipo=INDIVIDUAL&estado=ACTIVA
     * Lista pólizas con filtros opcionales por tipo y estado.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PolizaResponse>>> listarPolizas(
            @RequestParam(required = false) TipoPoliza tipo,
            @RequestParam(required = false) EstadoPoliza estado) {

        List<PolizaResponse> polizas = polizaService.listarPolizas(tipo, estado);
        return ResponseEntity.ok(ApiResponse.ok("Pólizas obtenidas exitosamente", polizas));
    }

    /**
     * GET /api/v1/polizas/{id}
     * Obtiene el detalle de una póliza por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PolizaResponse>> obtenerPoliza(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Póliza obtenida exitosamente",
                polizaService.obtenerPoliza(id)));
    }

    /**
     * POST /api/v1/polizas
     * Crea una nueva póliza (individual o colectiva).
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PolizaResponse>> crearPoliza(
            @Valid @RequestBody CrearPolizaRequest request) {
        PolizaResponse response = polizaService.crearPoliza(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Póliza creada exitosamente", response));
    }

    /**
     * GET /api/v1/polizas/{id}/riesgos
     * Lista todos los riesgos asociados a una póliza.
     */
    @GetMapping("/{id}/riesgos")
    public ResponseEntity<ApiResponse<List<RiesgoResponse>>> listarRiesgos(@PathVariable Long id) {
        List<RiesgoResponse> riesgos = polizaService.listarRiesgos(id);
        return ResponseEntity.ok(ApiResponse.ok("Riesgos obtenidos exitosamente", riesgos));
    }

    /**
     * POST /api/v1/polizas/{id}/renovar
     * Renueva una póliza aplicando el incremento de IPC al canon y la prima.
     * Estado pasa a RENOVADA.
     */
    @PostMapping("/{id}/renovar")
    public ResponseEntity<ApiResponse<PolizaResponse>> renovarPoliza(
            @PathVariable Long id,
            @Valid @RequestBody RenovarPolizaRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Póliza renovada exitosamente",
                polizaService.renovarPoliza(id, request)));
    }

    /**
     * POST /api/v1/polizas/{id}/cancelar
     * Cancela una póliza y todos sus riesgos activos.
     */
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ApiResponse<PolizaResponse>> cancelarPoliza(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Póliza cancelada exitosamente",
                polizaService.cancelarPoliza(id)));
    }

    /**
     * PUT /api/v1/polizas/{id}
     * Modifica datos de una póliza existente (patch semántico).
     * No se pueden cambiar: tipo, número de póliza ni estado directamente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PolizaResponse>> actualizarPoliza(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarPolizaRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Póliza actualizada exitosamente",
                polizaService.actualizarPoliza(id, request)));
    }

    /**
     * POST /api/v1/polizas/{id}/riesgos
     * Agrega un riesgo a una póliza COLECTIVA.
     */
    @PostMapping("/{id}/riesgos")
    public ResponseEntity<ApiResponse<RiesgoResponse>> agregarRiesgo(
            @PathVariable Long id,
            @Valid @RequestBody AgregarRiesgoRequest request) {
        RiesgoResponse riesgo = polizaService.agregarRiesgo(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Riesgo agregado exitosamente", riesgo));
    }
}



