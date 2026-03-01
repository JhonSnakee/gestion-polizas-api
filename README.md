# Gestion de Polizas - API REST

API REST para la gestion de polizas de arrendamiento de inmuebles (Individuales y Colectivas),
desarrollada con **Spring Boot 3.2.3**, **Java 21** y **H2 en memoria**.

---

## Estructura del proyecto

```
src/main/java/org/example/
├── Main.java                                  ← Punto de entrada Spring Boot
├── domain/
│   ├── entity/
│   │   ├── Poliza.java                        ← Entidad JPA Poliza
│   │   └── Riesgo.java                        ← Entidad JPA Riesgo
│   ├── enums/
│   │   ├── TipoPoliza.java                    ← INDIVIDUAL | COLECTIVA
│   │   ├── EstadoPoliza.java                  ← ACTIVA | RENOVADA | CANCELADA
│   │   └── EstadoRiesgo.java                  ← ACTIVO | CANCELADO
│   └── repository/
│       ├── PolizaRepository.java
│       └── RiesgoRepository.java
├── application/
│   ├── dto/
│   │   ├── request/
│   │   │   ├── CrearPolizaRequest.java
│   │   │   ├── AgregarRiesgoRequest.java
│   │   │   ├── RenovarPolizaRequest.java
│   │   │   └── CoreEventoRequest.java
│   │   └── response/
│   │       ├── ApiResponse.java               ← Wrapper generico de respuesta
│   │       ├── PolizaResponse.java
│   │       └── RiesgoResponse.java
│   ├── mapper/
│   │   └── PolizaMapper.java
│   └── service/
│       └── PolizaService.java                 ← Logica de negocio principal
└── infrastructure/
    ├── adapter/
    │   └── CoreTransaccionalAdapter.java      ← Adaptador al CORE legado (WebLogic)
    ├── config/
    │   └── DataLoader.java                    ← Datos de prueba al arrancar
    ├── exception/
    │   ├── BusinessRuleException.java
    │   ├── PolizaNotFoundException.java
    │   ├── RiesgoNotFoundException.java
    │   └── GlobalExceptionHandler.java
    ├── security/
    │   └── ApiKeyFilter.java                  ← Validacion header x-api-key
    └── web/controller/
        ├── PolizaController.java              ← Endpoints /api/v1/polizas
        ├── RiesgoController.java              ← Endpoints /api/v1/riesgos
        └── CoreMockController.java            ← Mock /core-mock/evento
```

---

## Requisitos

| Herramienta | Version minima |
|-------------|---------------|
| Java (JDK)  | 21            |
| Maven       | 3.8+          |

---

## Como ejecutar

### Opcion 1 — Desde IntelliJ IDEA
Abrir el proyecto y ejecutar la clase `Main.java` (boton Run).

### Opcion 2 — Maven desde terminal
```bash
mvn spring-boot:run
```

### Opcion 3 — JAR generado
```bash
mvn clean package
java -jar target/Gestion_Polizas-1.0.0.jar
```

La aplicacion arranca en `http://localhost:8080`

---

## Seguridad

Todos los endpoints `/api/v1/**` requieren el siguiente header:

```
x-api-key: 123456
```

El endpoint `/core-mock/**` esta excluido (simula un servicio interno).

---

## Endpoints disponibles

### Polizas — `/api/v1/polizas`

| Metodo | URL | Descripcion |
|--------|-----|-------------|
| `GET`  | `/api/v1/polizas` | Listar polizas (filtros: `?tipo=INDIVIDUAL&estado=ACTIVA`) |
| `GET`  | `/api/v1/polizas/{id}` | Obtener poliza por ID |
| `POST` | `/api/v1/polizas` | Crear nueva poliza |
| `PUT`  | `/api/v1/polizas/{id}` | Modificar poliza existente (patch semantico) |
| `GET`  | `/api/v1/polizas/{id}/riesgos` | Listar riesgos de una poliza |
| `POST` | `/api/v1/polizas/{id}/renovar` | Renovar poliza aplicando IPC |
| `POST` | `/api/v1/polizas/{id}/cancelar` | Cancelar poliza (cancela todos sus riesgos) |
| `POST` | `/api/v1/polizas/{id}/riesgos` | Agregar riesgo (solo COLECTIVA) |

### Riesgos — `/api/v1/riesgos`

| Metodo | URL | Descripcion |
|--------|-----|-------------|
| `POST` | `/api/v1/riesgos/{id}/cancelar` | Cancelar un riesgo individual (solo COLECTIVA) |

### Mock CORE — `/core-mock`

| Metodo | URL | Descripcion |
|--------|-----|-------------|
| `POST` | `/core-mock/evento` | Simula recepcion de evento por el CORE legado |

---

## Ejemplos de uso (curl)

### Listar todas las polizas
```bash
curl -X GET "http://localhost:8080/api/v1/polizas" \
  -H "x-api-key: 123456"
```

### Listar polizas COLECTIVAS ACTIVAS
```bash
curl -X GET "http://localhost:8080/api/v1/polizas?tipo=COLECTIVA&estado=ACTIVA" \
  -H "x-api-key: 123456"
```

### Crear una poliza individual
```bash
curl -X POST "http://localhost:8080/api/v1/polizas" \
  -H "x-api-key: 123456" \
  -H "Content-Type: application/json" \
  -d '{
    "numeroPoliza": "POL-IND-100",
    "tipo": "INDIVIDUAL",
    "tomador": "Ana Garcia",
    "asegurado": "Ana Garcia",
    "beneficiario": "Propietario Edificio Norte",
    "fechaInicioVigencia": "2025-03-01",
    "mesesVigencia": 12,
    "valorCanon": 1200000
  }'
```

### Modificar una poliza (patch semantico)
```bash
curl -X PUT "http://localhost:8080/api/v1/polizas/1" \
  -H "x-api-key: 123456" \
  -H "Content-Type: application/json" \
  -d '{
    "tomador": "Carlos Perez (actualizado)",
    "valorCanon": 1800000
  }'
```

### Renovar una poliza con IPC del 9.28%
```bash
curl -X POST "http://localhost:8080/api/v1/polizas/1/renovar" \
  -H "x-api-key: 123456" \
  -H "Content-Type: application/json" \
  -d '{ "ipcPorcentaje": 9.28 }'
```

### Cancelar una poliza
```bash
curl -X POST "http://localhost:8080/api/v1/polizas/1/cancelar" \
  -H "x-api-key: 123456"
```

### Agregar riesgo a poliza colectiva
```bash
curl -X POST "http://localhost:8080/api/v1/polizas/3/riesgos" \
  -H "x-api-key: 123456" \
  -H "Content-Type: application/json" \
  -d '{
    "descripcion": "Local comercial piso 2",
    "direccionInmueble": "Calle 100 # 15-20 Local 201",
    "nombreArrendatario": "Empresa ABC S.A.S",
    "fechaInicio": "2025-01-01",
    "fechaFin": "2026-01-01",
    "valorAsegurado": 4500000
  }'
```

### Cancelar un riesgo
```bash
curl -X POST "http://localhost:8080/api/v1/riesgos/2/cancelar" \
  -H "x-api-key: 123456"
```

### Enviar evento al mock del CORE
```bash
curl -X POST "http://localhost:8080/core-mock/evento" \
  -H "Content-Type: application/json" \
  -d '{ "evento": "ACTUALIZACION", "polizaId": 555 }'
```

---

## Consola H2 (Base de datos en memoria)

Disponible en: `http://localhost:8080/h2-console`

| Campo    | Valor                            |
|----------|----------------------------------|
| JDBC URL | `jdbc:h2:mem:polizasdb`          |
| Usuario  | `sa`                             |
| Password | *(vacio)*                        |

---

## Reglas de negocio implementadas

| Regla | Implementacion |
|-------|---------------|
| Una poliza INDIVIDUAL solo puede tener 1 riesgo | Validado en `agregarRiesgo()` — rechaza cualquier intento |
| No se puede renovar una poliza CANCELADA | Validado en `renovarPoliza()` |
| No se puede modificar una poliza CANCELADA | Validado en `actualizarPoliza()` |
| La cancelacion de una poliza cancela todos sus riesgos | Implementado en `cancelarPoliza()` |
| Solo se pueden agregar riesgos a polizas COLECTIVAS | Validado en `agregarRiesgo()` |
| La cancelacion individual de riesgos solo aplica a COLECTIVAS | Validado en `cancelarRiesgo()` |
| Renovacion ajusta canon y prima segun IPC | Formula: `nuevoCanon = canonActual * (1 + IPC/100)` |
| La prima es `valorCanon * mesesVigencia` | Calculado en creacion, renovacion y modificacion |
| Toda modificacion de estado notifica al CORE | Via `CoreTransaccionalAdapter` (CREACION, ACTUALIZACION, RENOVACION, CANCELACION) |

---

## Datos de prueba pre-cargados

Al iniciar la aplicacion se cargan automaticamente 4 polizas:

| ID | Numero | Tipo | Estado | Canon |
|----|--------|------|--------|-------|
| 1 | POL-IND-001 | INDIVIDUAL | ACTIVA | $1,500,000 |
| 2 | POL-IND-002 | INDIVIDUAL | CANCELADA | $900,000 |
| 3 | POL-COL-001 | COLECTIVA | ACTIVA | $2,000,000 |
| 4 | POL-COL-002 | COLECTIVA | RENOVADA | $2,120,000 |

---

## Tests de integracion

```bash
mvn test
```

**19 tests** cubriendo:

| # | Test | Verifica |
|---|------|---------|
| 1 | Sin API Key → 401 | Seguridad |
| 2 | API Key invalida → 401 | Seguridad |
| 3 | GET /polizas → lista completa | Listado |
| 4 | GET /polizas?tipo=INDIVIDUAL | Filtro por tipo |
| 5 | GET /polizas?estado=ACTIVA | Filtro por estado |
| 6 | GET /polizas/{id} → poliza correcta | Consulta por ID |
| 7 | GET /polizas/999999 → 404 | Not Found |
| 8 | POST /polizas/{id}/renovar con IPC 10% | Renovacion + recalculo |
| 9 | POST renovar poliza CANCELADA → 422 | Regla de negocio |
| 10 | POST /polizas/{id}/cancelar → cancela riesgos | Cancelacion en cascada |
| 11 | PUT /polizas/{id} actualiza canon y recalcula prima | Modificacion |
| 12 | PUT sobre poliza CANCELADA → 422 | Regla de negocio |
| 13 | POST /polizas/{id}/riesgos en COLECTIVA → 201 | Agregar riesgo |
| 14 | POST /polizas/{id}/riesgos en INDIVIDUAL → 422 | Validacion tipo |
| 15 | POST /polizas/{id}/riesgos en INDIVIDUAL (con riesgo existente) → 422 | Regla 1 riesgo |
| 16 | POST /riesgos/{id}/cancelar en COLECTIVA | Cancelar riesgo |
| 17 | POST /riesgos/{id}/cancelar ya CANCELADO → 422 | Estado invalido |
| 18 | POST /core-mock/evento sin API Key → 200 | Mock CORE |

