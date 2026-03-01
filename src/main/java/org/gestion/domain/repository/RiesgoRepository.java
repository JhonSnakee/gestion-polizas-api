package org.gestion.domain.repository;

import org.gestion.domain.entity.Riesgo;
import org.gestion.domain.enums.EstadoRiesgo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RiesgoRepository extends JpaRepository<Riesgo, Long> {

    List<Riesgo> findByPolizaId(Long polizaId);

    List<Riesgo> findByPolizaIdAndEstado(Long polizaId, EstadoRiesgo estado);

    long countByPolizaIdAndEstado(Long polizaId, EstadoRiesgo estado);
}

