package org.gestion.application.mapper;

import org.gestion.application.dto.response.PolizaResponse;
import org.gestion.application.dto.response.RiesgoResponse;
import org.gestion.domain.entity.Poliza;
import org.gestion.domain.entity.Riesgo;
import org.gestion.domain.enums.EstadoRiesgo;
import org.springframework.stereotype.Component;

@Component
public class PolizaMapper {

    public PolizaResponse toResponse(Poliza poliza) {
        int riesgosActivos = (int) poliza.getRiesgos().stream()
                .filter(r -> r.getEstado() == EstadoRiesgo.ACTIVO)
                .count();

        return PolizaResponse.builder()
                .id(poliza.getId())
                .numeroPoliza(poliza.getNumeroPoliza())
                .tipo(poliza.getTipo())
                .estado(poliza.getEstado())
                .tomador(poliza.getTomador())
                .asegurado(poliza.getAsegurado())
                .beneficiario(poliza.getBeneficiario())
                .fechaInicioVigencia(poliza.getFechaInicioVigencia())
                .fechaFinVigencia(poliza.getFechaFinVigencia())
                .mesesVigencia(poliza.getMesesVigencia())
                .valorCanon(poliza.getValorCanon())
                .valorPrima(poliza.getValorPrima())
                .contadorRenovaciones(poliza.getContadorRenovaciones())
                .totalRiesgosActivos(riesgosActivos)
                .build();
    }

    public RiesgoResponse toRiesgoResponse(Riesgo riesgo) {
        return RiesgoResponse.builder()
                .id(riesgo.getId())
                .descripcion(riesgo.getDescripcion())
                .direccionInmueble(riesgo.getDireccionInmueble())
                .nombreArrendatario(riesgo.getNombreArrendatario())
                .estado(riesgo.getEstado())
                .fechaInicio(riesgo.getFechaInicio())
                .fechaFin(riesgo.getFechaFin())
                .valorAsegurado(riesgo.getValorAsegurado())
                .polizaId(riesgo.getPoliza().getId())
                .build();
    }
}

