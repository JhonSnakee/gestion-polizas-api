package org.gestion.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gestion.application.dto.request.ActualizarPolizaRequest;
import org.gestion.application.dto.request.AgregarRiesgoRequest;
import org.gestion.application.dto.request.CrearPolizaRequest;
import org.gestion.application.dto.request.RenovarPolizaRequest;
import org.gestion.application.dto.response.PolizaResponse;
import org.gestion.application.dto.response.RiesgoResponse;
import org.gestion.application.mapper.PolizaMapper;
import org.gestion.domain.entity.Poliza;
import org.gestion.domain.entity.Riesgo;
import org.gestion.domain.enums.EstadoPoliza;
import org.gestion.domain.enums.EstadoRiesgo;
import org.gestion.domain.enums.TipoPoliza;
import org.gestion.domain.repository.PolizaRepository;
import org.gestion.domain.repository.RiesgoRepository;
import org.gestion.infrastructure.adapter.CoreTransaccionalAdapter;
import org.gestion.infrastructure.exception.BusinessRuleException;
import org.gestion.infrastructure.exception.PolizaNotFoundException;
import org.gestion.infrastructure.exception.RiesgoNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolizaService {

    private final PolizaRepository polizaRepository;
    private final RiesgoRepository riesgoRepository;
    private final PolizaMapper polizaMapper;
    private final CoreTransaccionalAdapter coreAdapter;

    // ─────────────────────────────────────────────────────────────────────────
    // CONSULTAS
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PolizaResponse> listarPolizas(TipoPoliza tipo, EstadoPoliza estado) {
        List<Poliza> polizas;
        if (tipo != null && estado != null) {
            polizas = polizaRepository.findByTipoAndEstado(tipo, estado);
        } else if (tipo != null) {
            polizas = polizaRepository.findByTipo(tipo);
        } else if (estado != null) {
            polizas = polizaRepository.findByEstado(estado);
        } else {
            polizas = polizaRepository.findAll();
        }
        return polizas.stream().map(polizaMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PolizaResponse obtenerPoliza(Long id) {
        return polizaMapper.toResponse(buscarPolizaOLanzar(id));
    }

    @Transactional(readOnly = true)
    public List<RiesgoResponse> listarRiesgos(Long polizaId) {
        buscarPolizaOLanzar(polizaId);
        return riesgoRepository.findByPolizaId(polizaId)
                .stream()
                .map(polizaMapper::toRiesgoResponse)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREACIÓN
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public PolizaResponse crearPoliza(CrearPolizaRequest request) {
        BigDecimal prima = request.getValorCanon()
                .multiply(BigDecimal.valueOf(request.getMesesVigencia()))
                .setScale(2, RoundingMode.HALF_UP);

        Poliza poliza = Poliza.builder()
                .numeroPoliza(request.getNumeroPoliza())
                .tipo(request.getTipo())
                .estado(EstadoPoliza.ACTIVA)
                .tomador(request.getTomador())
                .asegurado(request.getAsegurado())
                .beneficiario(request.getBeneficiario())
                .fechaInicioVigencia(request.getFechaInicioVigencia())
                .fechaFinVigencia(request.getFechaInicioVigencia().plusMonths(request.getMesesVigencia()))
                .mesesVigencia(request.getMesesVigencia())
                .valorCanon(request.getValorCanon())
                .valorPrima(prima)
                .build();

        poliza = polizaRepository.save(poliza);
        log.info("[POLIZA] Póliza creada -> id: {}, tipo: {}, numero: {}",
                poliza.getId(), poliza.getTipo(), poliza.getNumeroPoliza());
        coreAdapter.notificarEvento("CREACION", poliza.getId());
        return polizaMapper.toResponse(poliza);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODIFICACIÓN
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public PolizaResponse actualizarPoliza(Long id, ActualizarPolizaRequest request) {
        Poliza poliza = buscarPolizaOLanzar(id);

        if (poliza.getEstado() == EstadoPoliza.CANCELADA) {
            throw new BusinessRuleException("No se puede modificar la póliza ID " + id + " porque está CANCELADA.");
        }

        // Patch semántico: solo actualiza los campos presentes en el request
        if (request.getTomador() != null && !request.getTomador().isBlank()) {
            poliza.setTomador(request.getTomador());
        }
        if (request.getAsegurado() != null && !request.getAsegurado().isBlank()) {
            poliza.setAsegurado(request.getAsegurado());
        }
        if (request.getBeneficiario() != null && !request.getBeneficiario().isBlank()) {
            poliza.setBeneficiario(request.getBeneficiario());
        }
        if (request.getValorCanon() != null) {
            poliza.setValorCanon(request.getValorCanon());
            // Recalcular prima con el nuevo canon
            BigDecimal nuevaPrima = request.getValorCanon()
                    .multiply(BigDecimal.valueOf(poliza.getMesesVigencia()))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            poliza.setValorPrima(nuevaPrima);
        }
        if (request.getMesesVigencia() != null) {
            poliza.setMesesVigencia(request.getMesesVigencia());
            // Recalcular prima con los nuevos meses
            BigDecimal nuevaPrima = poliza.getValorCanon()
                    .multiply(BigDecimal.valueOf(request.getMesesVigencia()))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            poliza.setValorPrima(nuevaPrima);
            poliza.setFechaFinVigencia(poliza.getFechaInicioVigencia().plusMonths(request.getMesesVigencia()));
        }
        if (request.getFechaInicioVigencia() != null) {
            poliza.setFechaInicioVigencia(request.getFechaInicioVigencia());
            poliza.setFechaFinVigencia(request.getFechaInicioVigencia().plusMonths(poliza.getMesesVigencia()));
        }

        polizaRepository.save(poliza);
        log.info("[POLIZA] Póliza actualizada -> id: {}", id);
        coreAdapter.notificarEvento("ACTUALIZACION", id);
        return polizaMapper.toResponse(poliza);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RENOVACIÓN
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public PolizaResponse renovarPoliza(Long id, RenovarPolizaRequest request) {
        Poliza poliza = buscarPolizaOLanzar(id);

        if (poliza.getEstado() == EstadoPoliza.CANCELADA) {
            throw new BusinessRuleException(
                    "No se puede renovar la póliza ID " + id + " porque está CANCELADA.");
        }

        BigDecimal factor = BigDecimal.ONE.add(
                request.getIpcPorcentaje().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));

        BigDecimal nuevoCanon = poliza.getValorCanon().multiply(factor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal nuevaPrima = nuevoCanon.multiply(BigDecimal.valueOf(poliza.getMesesVigencia()))
                .setScale(2, RoundingMode.HALF_UP);

        poliza.setValorCanon(nuevoCanon);
        poliza.setValorPrima(nuevaPrima);
        poliza.setFechaInicioVigencia(poliza.getFechaFinVigencia());
        poliza.setFechaFinVigencia(poliza.getFechaFinVigencia().plusMonths(poliza.getMesesVigencia()));
        poliza.setEstado(EstadoPoliza.RENOVADA);
        poliza.setContadorRenovaciones(poliza.getContadorRenovaciones() + 1);

        polizaRepository.save(poliza);
        log.info("[POLIZA] Póliza renovada -> id: {}, nuevoCanon: {}, nuevaPrima: {}, IPC: {}%",
                id, nuevoCanon, nuevaPrima, request.getIpcPorcentaje());
        coreAdapter.notificarEvento("RENOVACION", id);
        return polizaMapper.toResponse(poliza);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CANCELACIÓN
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public PolizaResponse cancelarPoliza(Long id) {
        Poliza poliza = buscarPolizaOLanzar(id);

        if (poliza.getEstado() == EstadoPoliza.CANCELADA) {
            throw new BusinessRuleException("La póliza ID " + id + " ya se encuentra CANCELADA.");
        }

        // Cancelar todos los riesgos activos
        poliza.getRiesgos().stream()
                .filter(r -> r.getEstado() == EstadoRiesgo.ACTIVO)
                .forEach(r -> {
                    r.setEstado(EstadoRiesgo.CANCELADO);
                    log.info("[RIESGO] Riesgo cancelado por cancelación de póliza -> riesgoId: {}", r.getId());
                });

        poliza.setEstado(EstadoPoliza.CANCELADA);
        polizaRepository.save(poliza);
        log.info("[POLIZA] Póliza cancelada -> id: {}", id);
        coreAdapter.notificarEvento("CANCELACION", id);
        return polizaMapper.toResponse(poliza);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GESTIÓN DE RIESGOS
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public RiesgoResponse agregarRiesgo(Long polizaId, AgregarRiesgoRequest request) {
        Poliza poliza = buscarPolizaOLanzar(polizaId);

        if (poliza.getTipo() == TipoPoliza.INDIVIDUAL) {
            throw new BusinessRuleException(
                    "Una póliza INDIVIDUAL solo puede tener 1 riesgo. " +
                    "Solo las pólizas COLECTIVAS admiten agregar riesgos.");
        }
        if (poliza.getEstado() == EstadoPoliza.CANCELADA) {
            throw new BusinessRuleException(
                    "No se pueden agregar riesgos a una póliza CANCELADA.");
        }

        Riesgo riesgo = Riesgo.builder()
                .descripcion(request.getDescripcion())
                .direccionInmueble(request.getDireccionInmueble())
                .nombreArrendatario(request.getNombreArrendatario())
                .estado(EstadoRiesgo.ACTIVO)
                .fechaInicio(request.getFechaInicio())
                .fechaFin(request.getFechaFin())
                .valorAsegurado(request.getValorAsegurado())
                .poliza(poliza)
                .build();

        riesgo = riesgoRepository.save(riesgo);
        log.info("[RIESGO] Riesgo agregado -> riesgoId: {}, polizaId: {}", riesgo.getId(), polizaId);
        coreAdapter.notificarEvento("ACTUALIZACION", polizaId);
        return polizaMapper.toRiesgoResponse(riesgo);
    }

    @Transactional
    public RiesgoResponse cancelarRiesgo(Long riesgoId) {
        Riesgo riesgo = riesgoRepository.findById(riesgoId)
                .orElseThrow(() -> new RiesgoNotFoundException(riesgoId));

        if (riesgo.getEstado() == EstadoRiesgo.CANCELADO) {
            throw new BusinessRuleException("El riesgo ID " + riesgoId + " ya está CANCELADO.");
        }

        Poliza poliza = riesgo.getPoliza();
        if (poliza.getTipo() != TipoPoliza.COLECTIVA) {
            throw new BusinessRuleException(
                    "Solo se pueden cancelar riesgos de forma individual en pólizas COLECTIVAS.");
        }

        riesgo.setEstado(EstadoRiesgo.CANCELADO);
        riesgoRepository.save(riesgo);
        log.info("[RIESGO] Riesgo cancelado -> riesgoId: {}, polizaId: {}", riesgoId, poliza.getId());
        coreAdapter.notificarEvento("ACTUALIZACION", poliza.getId());
        return polizaMapper.toRiesgoResponse(riesgo);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS PRIVADOS
    // ─────────────────────────────────────────────────────────────────────────

    private Poliza buscarPolizaOLanzar(Long id) {
        return polizaRepository.findById(id)
                .orElseThrow(() -> new PolizaNotFoundException(id));
    }
}

