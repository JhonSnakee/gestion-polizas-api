package org.gestion.infrastructure.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter de integración con el CORE transaccional legado disponibilizado
 * a través de capa media en WebLogic.
 *
 * En un entorno real, este adaptador realizaría una llamada HTTP/SOAP al
 * servicio agnóstico de edición expuesto por WebLogic. Para esta prueba,
 * registra la operación en logs simulando el contrato de integración.
 */
@Slf4j
@Component
public class CoreTransaccionalAdapter {

    /**
     * Notifica al CORE transaccional sobre una actualización de estado
     * de póliza o riesgo.
     *
     * @param evento    Tipo de evento (CREACION, ACTUALIZACION, CANCELACION, RENOVACION)
     * @param polizaId  Identificador de la póliza afectada
     */
    public void notificarEvento(String evento, Long polizaId) {
        log.info("[CORE-ADAPTER] Enviando evento al sistema CORE legado -> evento: {}, polizaId: {}",
                evento, polizaId);
        // TODO: Implementar llamada real al servicio WebLogic
        // Ejemplo de contrato:
        // POST http://weblogic-host:7001/core/seguros/edicion
        // Body: { "evento": "ACTUALIZACION", "polizaId": 555 }
        log.info("[CORE-ADAPTER] Evento registrado exitosamente en CORE -> polizaId: {}", polizaId);
    }
}

