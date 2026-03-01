package org.gestion.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gestion.domain.entity.Poliza;
import org.gestion.domain.entity.Riesgo;
import org.gestion.domain.enums.EstadoPoliza;
import org.gestion.domain.enums.EstadoRiesgo;
import org.gestion.domain.enums.TipoPoliza;
import org.gestion.domain.repository.PolizaRepository;
import org.gestion.domain.repository.RiesgoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataLoader {

    @Bean
    CommandLineRunner initData(PolizaRepository polizaRepo, RiesgoRepository riesgoRepo) {
        return args -> {
            log.info("[INIT] Cargando datos de prueba...");

            // Póliza Individual 1 - ACTIVA
            Poliza individual1 = polizaRepo.save(Poliza.builder()
                    .numeroPoliza("POL-IND-001")
                    .tipo(TipoPoliza.INDIVIDUAL)
                    .estado(EstadoPoliza.ACTIVA)
                    .tomador("Carlos Pérez")
                    .asegurado("Carlos Pérez")
                    .beneficiario("Inmobiliaria Norte S.A.S")
                    .fechaInicioVigencia(LocalDate.of(2025, 1, 1))
                    .fechaFinVigencia(LocalDate.of(2026, 1, 1))
                    .mesesVigencia(12)
                    .valorCanon(new BigDecimal("1500000.00"))
                    .valorPrima(new BigDecimal("18000000.00"))
                    .build());

            // Riesgo único de la póliza individual
            riesgoRepo.save(Riesgo.builder()
                    .descripcion("Apartamento 301 - Arrendamiento residencial")
                    .direccionInmueble("Calle 80 # 15-30 Apto 301, Bogotá")
                    .nombreArrendatario("Carlos Pérez")
                    .estado(EstadoRiesgo.ACTIVO)
                    .fechaInicio(LocalDate.of(2025, 1, 1))
                    .fechaFin(LocalDate.of(2026, 1, 1))
                    .valorAsegurado(new BigDecimal("1500000.00"))
                    .poliza(individual1)
                    .build());

            // Póliza Individual 2 - CANCELADA
            polizaRepo.save(Poliza.builder()
                    .numeroPoliza("POL-IND-002")
                    .tipo(TipoPoliza.INDIVIDUAL)
                    .estado(EstadoPoliza.CANCELADA)
                    .tomador("María López")
                    .asegurado("María López")
                    .beneficiario("Arrendador Luis Martínez")
                    .fechaInicioVigencia(LocalDate.of(2024, 6, 1))
                    .fechaFinVigencia(LocalDate.of(2025, 6, 1))
                    .mesesVigencia(12)
                    .valorCanon(new BigDecimal("900000.00"))
                    .valorPrima(new BigDecimal("10800000.00"))
                    .build());

            // Póliza Colectiva 1 - ACTIVA (Inmobiliaria)
            Poliza colectiva1 = polizaRepo.save(Poliza.builder()
                    .numeroPoliza("POL-COL-001")
                    .tipo(TipoPoliza.COLECTIVA)
                    .estado(EstadoPoliza.ACTIVA)
                    .tomador("Inmobiliaria Central S.A.S")
                    .asegurado("Inmobiliaria Central S.A.S")
                    .beneficiario("Propietarios afiliados Inmobiliaria Central")
                    .fechaInicioVigencia(LocalDate.of(2025, 1, 1))
                    .fechaFinVigencia(LocalDate.of(2026, 1, 1))
                    .mesesVigencia(12)
                    .valorCanon(new BigDecimal("2000000.00"))
                    .valorPrima(new BigDecimal("24000000.00"))
                    .build());

            // Riesgos de la póliza colectiva
            riesgoRepo.save(Riesgo.builder()
                    .descripcion("Local comercial piso 1 - Arrendamiento comercial")
                    .direccionInmueble("Carrera 7 # 45-20 Local 101, Bogotá")
                    .nombreArrendatario("Tienda XYZ Ltda.")
                    .estado(EstadoRiesgo.ACTIVO)
                    .fechaInicio(LocalDate.of(2025, 1, 1))
                    .fechaFin(LocalDate.of(2026, 1, 1))
                    .valorAsegurado(new BigDecimal("3000000.00"))
                    .poliza(colectiva1)
                    .build());

            riesgoRepo.save(Riesgo.builder()
                    .descripcion("Apartamento 502 - Arrendamiento residencial")
                    .direccionInmueble("Av. El Dorado # 68-20 Apto 502, Bogotá")
                    .nombreArrendatario("Pedro Ramírez")
                    .estado(EstadoRiesgo.ACTIVO)
                    .fechaInicio(LocalDate.of(2025, 1, 1))
                    .fechaFin(LocalDate.of(2026, 1, 1))
                    .valorAsegurado(new BigDecimal("1800000.00"))
                    .poliza(colectiva1)
                    .build());

            // Póliza Colectiva 2 - RENOVADA
            Poliza colectiva2 = polizaRepo.save(Poliza.builder()
                    .numeroPoliza("POL-COL-002")
                    .tipo(TipoPoliza.COLECTIVA)
                    .estado(EstadoPoliza.RENOVADA)
                    .tomador("Administración Torres del Norte")
                    .asegurado("Administración Torres del Norte")
                    .beneficiario("Copropietarios Torres del Norte")
                    .fechaInicioVigencia(LocalDate.of(2026, 1, 1))
                    .fechaFinVigencia(LocalDate.of(2027, 1, 1))
                    .mesesVigencia(12)
                    .valorCanon(new BigDecimal("2120000.00"))
                    .valorPrima(new BigDecimal("25440000.00"))
                    .contadorRenovaciones(1)
                    .build());

            riesgoRepo.save(Riesgo.builder()
                    .descripcion("Bodega piso -1 - Almacenamiento")
                    .direccionInmueble("Calle 100 # 20-50 Bodega 3, Bogotá")
                    .nombreArrendatario("Logística Rápida S.A.")
                    .estado(EstadoRiesgo.ACTIVO)
                    .fechaInicio(LocalDate.of(2026, 1, 1))
                    .fechaFin(LocalDate.of(2027, 1, 1))
                    .valorAsegurado(new BigDecimal("5000000.00"))
                    .poliza(colectiva2)
                    .build());

            log.info("[INIT] Datos de prueba cargados exitosamente.");
        };
    }
}

