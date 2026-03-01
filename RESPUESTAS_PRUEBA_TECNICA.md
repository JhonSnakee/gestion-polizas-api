# Prueba Tecnica Senior — Respuestas Modulos 1, 3, 4 y 5

---

## MODULO 1 — Diseno de Sistema (System Design)

### 1. Arquitectura de alto nivel

La plataforma se diseña como un sistema de microservicios desacoplados, expuestos
a traves de un API Gateway, con comunicacion sincrona REST hacia el frontend y
comunicacion asincrona (eventos) hacia el CORE transaccional legado.

```
                        ┌─────────────────────────────────────────────────┐
                        │                 CLIENTES                        │
                        │  Frontend Web / App Movil / Sistemas Terceros   │
                        └──────────────────┬──────────────────────────────┘
                                           │ HTTPS
                        ┌──────────────────▼──────────────────────────────┐
                        │              API GATEWAY                        │
                        │  - Enrutamiento                                 │
                        │  - Autenticacion (API Key / OAuth2)             │
                        │  - Rate Limiting / SSL Termination              │
                        │  - Versionamiento (/v1, /v2)                    │
                        └──┬──────────────┬──────────────┬────────────────┘
                           │              │              │
               ┌───────────▼──┐  ┌────────▼──────┐  ┌──▼───────────────────┐
               │   Servicio   │  │   Servicio    │  │  Servicio            │
               │  de Polizas  │  │  de Riesgos   │  │  de Notificaciones   │
               │  (REST API)  │  │  (REST API)   │  │  (Email / SMS)       │
               └──────┬───────┘  └───────┬───────┘  └──────────────────────┘
                      │                  │                      ▲
                      └────────┬─────────┘                      │
                               │ Publica eventos                │ Consume eventos
                        ┌──────▼──────────────────────────────┐ │
                        │         MESSAGE BROKER              │─┘
                        │         (Apache Kafka)              │
                        └──────────────┬──────────────────────┘
                                       │
                        ┌──────────────▼──────────────────────┐
                        │     ADAPTER / CAPA DE INTEGRACION   │
                        │  (Consumidor Kafka → llamada HTTP   │
                        │   al servicio WebLogic del CORE)    │
                        └──────────────┬──────────────────────┘
                                       │ HTTP/SOAP
                        ┌──────────────▼──────────────────────┐
                        │   CORE TRANSACCIONAL LEGADO         │
                        │   (Sistema de Seguros en WebLogic)  │
                        └─────────────────────────────────────┘

    ┌─────────────────────────────────────────────────────────┐
    │                BASE DE DATOS                            │
    │  PostgreSQL (principal) + Redis (cache)                 │
    │  - polizas, riesgos, auditoria                          │
    └─────────────────────────────────────────────────────────┘
```

---

### 2. Tres patrones de arquitectura seleccionados

#### a) Arquitectura Hexagonal (Ports & Adapters)

**Por que:** El negocio de polizas tiene reglas complejas (renovacion, tipos,
estados) que deben ser completamente independientes de la tecnologia. Con la
arquitectura hexagonal, el dominio (Poliza, Riesgo, reglas de negocio) no conoce
Spring, JPA ni ningun framework externo. Esto permite:
- Testear la logica de negocio sin levantar el contexto de Spring.
- Cambiar la base de datos, el ORM o el canal de notificacion sin tocar el dominio.
- Que el CORE legado sea simplemente un "adapter de salida" reemplazable.

**Como se aplica:** Las capas del proyecto son:
- **Dominio:** entidades, value objects, reglas de negocio puras.
- **Aplicacion:** casos de uso (PolizaService).
- **Infraestructura:** JPA repositories, filtros HTTP, adaptador CORE.

---

#### b) Event-Driven Architecture (EDA) con Apache Kafka

**Por que:** Todas las operaciones que modifican el estado de una poliza o riesgo
deben notificar al CORE legado sin acoplar el tiempo de respuesta de WebLogic
al del cliente. Si el CORE tiene latencia o esta caido, la operacion del
cliente no debe fallar. Kafka actua como buffer garantizado:

- La API de Polizas publica el evento (ej. `POLIZA_RENOVADA`) en un topico Kafka.
- El Adapter de integracion consume ese evento de forma asincrona y llama a WebLogic.
- Si WebLogic falla, Kafka reintenta automaticamente con politica de backoff.

**Beneficios adicionales:**
- El Servicio de Notificaciones (Email/SMS) consume el mismo topico sin acoplamiento.
- Auditoria completa de todos los eventos sin costo adicional.
- Escalabilidad horizontal del consumidor segun la carga.

---

#### c) CQRS (Command Query Responsibility Segregation)

**Por que:** Las consultas de polizas (listados por tipo/estado, reportes,
dashboards) tienen patrones de acceso muy diferentes a las escrituras. Con CQRS:

- **Commands** (crear, renovar, cancelar): escriben sobre la BD principal (PostgreSQL),
  con todas las validaciones de negocio y emision de eventos.
- **Queries** (listar, consultar): leen de una replica de lectura o de una proyeccion
  desnormalizada en Redis/Elasticsearch, sin contention con las escrituras.

**Beneficio directo:** El requerimiento de disponibilidad 24/7 se facilita porque
el lado de lectura puede escalar independientemente y seguir disponible incluso
durante ventanas de mantenimiento del lado de escritura.

---

### 3. Modelo de datos principal

```
POLIZA
------
id                    BIGINT PK AUTO
numero_poliza         VARCHAR(50) UNIQUE NOT NULL
tipo                  ENUM(INDIVIDUAL, COLECTIVA) NOT NULL
estado                ENUM(ACTIVA, RENOVADA, CANCELADA) NOT NULL
tomador               VARCHAR(200) NOT NULL
asegurado             VARCHAR(200) NOT NULL
beneficiario          VARCHAR(200) NOT NULL
fecha_inicio_vigencia DATE NOT NULL
fecha_fin_vigencia    DATE NOT NULL
meses_vigencia        INTEGER NOT NULL
valor_canon           DECIMAL(15,2) NOT NULL
valor_prima           DECIMAL(15,2) NOT NULL   -- canon * meses_vigencia
contador_renovaciones INTEGER DEFAULT 0
created_at            TIMESTAMP
updated_at            TIMESTAMP

RIESGO
------
id                    BIGINT PK AUTO
poliza_id             BIGINT FK → POLIZA.id NOT NULL
descripcion           VARCHAR(100) NOT NULL
direccion_inmueble    VARCHAR(300) NOT NULL
nombre_arrendatario   VARCHAR(200) NOT NULL
estado                ENUM(ACTIVO, CANCELADO) NOT NULL
fecha_inicio          DATE NOT NULL
fecha_fin             DATE NOT NULL
valor_asegurado       DECIMAL(15,2) NOT NULL
created_at            TIMESTAMP

AUDITORIA_EVENTO
----------------
id                    BIGINT PK AUTO
poliza_id             BIGINT NOT NULL
tipo_evento           VARCHAR(50) NOT NULL   -- CREACION, RENOVACION, CANCELACION
estado_anterior       VARCHAR(20)
estado_nuevo          VARCHAR(20)
usuario               VARCHAR(100)
fecha_evento          TIMESTAMP NOT NULL
detalle               TEXT

Restriccion de negocio en BD:
  - Una poliza INDIVIDUAL no puede tener mas de 1 riesgo ACTIVO
    (validado a nivel de servicio y con constraint de conteo)
```

---

### 4. Temas transversales

#### Escalabilidad
- **Horizontal:** Cada microservicio se despliega en contenedores Docker/Kubernetes
  con HPA (Horizontal Pod Autoscaler) basado en CPU y latencia.
- **Base de datos:** PostgreSQL con replica de lectura (read replica) para
  consultas. Particionamiento de tabla `polizas` por `tipo` o rango de fechas.
- **Cache:** Redis para listados frecuentes de polizas activas, con TTL de 5 min
  e invalidacion por evento.
- **Kafka:** Particionamiento por `polizaId` garantiza orden de eventos por poliza.

#### Logs y observabilidad
- **Logs estructurados:** JSON con campos: `traceId`, `spanId`, `polizaId`,
  `evento`, `usuario`, `duracion_ms`. Herramienta: Logback + ELK Stack.
- **Trazabilidad distribuida:** OpenTelemetry + Jaeger para trazar el flujo
  completo: API Gateway → Servicio → Kafka → Adapter → CORE.
- **Metricas:** Micrometer + Prometheus + Grafana. Dashboards para:
  - Tasa de errores por endpoint
  - Latencia p50/p95/p99
  - Polizas creadas/renovadas/canceladas por hora
  - Tiempo de procesamiento del evento en CORE
- **Alertas:** Alertmanager para notificar si tasa de error > 1% o
  latencia p99 > 2 segundos.
- **Health checks:** Spring Actuator con endpoints /health, /metrics, /info.

#### Tolerancia a fallos
- **Circuit Breaker:** Resilience4j en el adaptador de CORE. Si WebLogic falla,
  el circuito se abre y los eventos se acumulan en Kafka sin afectar al cliente.
- **Retry con backoff exponencial:** 3 reintentos con backoff 1s/2s/4s antes
  de enviar a Dead Letter Queue (DLQ).
- **Idempotencia:** Cada evento lleva un `eventId` UUID. El CORE y el Adapter
  verifican que no procesen el mismo evento dos veces.
- **Saga Pattern:** Para operaciones que involucran multiples servicios
  (ej. renovar poliza + notificar + actualizar CORE), se usa una saga orquestada
  con compensacion en caso de fallo parcial.

#### Versionamiento de APIs
- **URI Versioning:** `/api/v1/polizas`, `/api/v2/polizas`.
  Es el estandar mas explicito y compatible con API Gateways.
- **Politica de deprecacion:** Una version se mantiene al menos 6 meses tras
  publicar la siguiente, con header `Deprecation` y `Sunset` en las respuestas.
- **Contrato API-First:** Definicion en OpenAPI 3.0 (Swagger) como fuente de
  verdad, generando stubs del servidor y del cliente automaticamente.

---

### 5. Diagrama de componentes

```
┌───────────────────────────────────────────────────────────────────────────┐
│                         PLATAFORMA GESTION POLIZAS                        │
│                                                                           │
│  ┌──────────────┐    ┌──────────────────────────────────────────────┐     │
│  │              │    │           API GATEWAY (Kong / AWS API GW)    │     │
│  │   FRONTEND   │───▶│  Autenticacion | Routing | Rate Limit | SSL  │     │
│  │   (React)    │    └──────────────┬─────────────────┬─────────────┘     │
│  └──────────────┘                   │                 │                   │
│                           ┌─────────▼──────┐  ┌──────▼────────────┐       │
│                           │   SERVICIO     │  │    SERVICIO       │       │
│                           │   POLIZAS      │  │    RIESGOS        │       │
│                           │  (Spring Boot) │  │  (Spring Boot)    │       │
│                           └──────┬─────────┘  └──────┬────────────┘       │
│                                  │                    │                   │
│                           ┌──────▼────────────────────▼──────────────┐    │
│                           │           BASE DE DATOS                  │    │
│                           │   PostgreSQL (escritura) + Replica (lect)│    │
│                           │   Redis (cache de consultas frecuentes)  │    │
│                           └──────────────────────────────────────────┘    │
│                                  │                                        │
│                           ┌──────▼───────────────────────────────────┐    │
│                           │         MESSAGE BROKER (Kafka)           │    │
│                           │  Topics: poliza-eventos, riesgo-eventos  │    │
│                           └──────┬────────────────────┬──────────────┘    │
│                                  │                    │                   │
│                     ┌────────────▼──────┐  ┌─────────▼─────────────┐      │
│                     │  SERVICIO         │  │  ADAPTER CORE         │      │
│                     │  NOTIFICACIONES   │  │  (Kafka Consumer)     │      │
│                     │  Email / SMS      │  │  → HTTP WebLogic      │      │
│                     └───────────────────┘  └─────────┬─────────────┘      │
│                                                      │                    │
│                                                      │                    │
│                                                      │ HTTP/SOAP          │
│                                            ┌──────────▼──────────────────┐│
│                                            │   CORE TRANSACCIONAL        ││
│                                            │   LEGADO (WebLogic)         ││
│                                            │   Sistema de Seguros        ││
│                                            └─────────────────────────────┘│
└───────────────────────────────────────────────────────────────────────────┘
```

## MODULO 2 — Endpoints implementados

| Metodo | URL | Descripcion | Regla de negocio |
|--------|-----|-------------|-----------------|
| `GET`  | `/api/v1/polizas?tipo=&estado=` | Listar con filtros | — |
| `GET`  | `/api/v1/polizas/{id}` | Consultar por ID | 404 si no existe |
| `POST` | `/api/v1/polizas` | Crear poliza | Prima = canon × meses |
| `PUT`  | `/api/v1/polizas/{id}` | **Modificar** poliza | No si CANCELADA |
| `GET`  | `/api/v1/polizas/{id}/riesgos` | Listar riesgos | — |
| `POST` | `/api/v1/polizas/{id}/renovar` | Renovar + IPC | No si CANCELADA |
| `POST` | `/api/v1/polizas/{id}/cancelar` | Cancelar | Cancela todos los riesgos |
| `POST` | `/api/v1/polizas/{id}/riesgos` | Agregar riesgo | Solo COLECTIVA |
| `POST` | `/api/v1/riesgos/{id}/cancelar` | Cancelar riesgo | Solo COLECTIVA |
| `POST` | `/core-mock/evento` | Mock CORE legado | Sin autenticacion |

---

## MODULO 3 — Conocimientos en BBDD

```sql
SELECT o.order_id, o.order_date, c.customer_name, o.total_amount
FROM orders o
JOIN customers c ON o.customer_id = c.customer_id
WHERE c.country = 'Mexico';
```

### Estrategia 1 — Indices compuestos

El problema raiz es que la BD hace un full scan de 500,000 clientes para
filtrar por `country`, y luego otro full scan de 10 millones de pedidos
para el JOIN.

```sql
-- Indice en customers para el filtro de pais
CREATE INDEX idx_customers_country ON customers(country);

-- Indice compuesto en orders cubriendo las columnas del SELECT
-- Permite un "index-only scan": no necesita leer la tabla base
CREATE INDEX idx_orders_customer_covering
    ON orders(customer_id)
    INCLUDE (order_id, order_date, total_amount);
```

**Impacto esperado:** La BD primero encuentra los customer_id de Mexico
usando el indice de customers (< 1ms), luego hace un Nested Loop o
Hash Join usando el indice de orders por customer_id. El tiempo puede
pasar de decenas de segundos a milisegundos.

---

### Estrategia 2 — Tabla materializada / Vista pre-calculada

Para una consulta que se ejecuta "frecuentemente", calcularla on-demand
es ineficiente. Una vista materializada pre-computa el resultado:

```sql
-- PostgreSQL
CREATE MATERIALIZED VIEW mv_orders_mexico AS
SELECT
    o.order_id,
    o.order_date,
    c.customer_name,
    o.total_amount,
    c.customer_id
FROM orders o
JOIN customers c ON o.customer_id = c.customer_id
WHERE c.country = 'Mexico';

CREATE INDEX idx_mv_orders_mexico_date ON mv_orders_mexico(order_date);

-- Refrescar periodicamente (o con trigger en cambios)
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_orders_mexico;
```

**Cuando usarla:** Si los datos de Mexico no cambian en tiempo real
(ej. se pueden tolerar datos de hace 5-10 minutos), es la estrategia
mas agresiva: la consulta original pasa a ser un simple SELECT sobre
una tabla pequena y ya indexada.

**Alternativa en Oracle/SQL Server:** usar `DBMS_MVIEW` / Indexed Views.

---

### Estrategia 3 — Particionamiento de tabla

Con 10 millones de registros que crecen constantemente, el problema
se agravara. El particionamiento divide fisicamente la tabla para que
la BD solo lea el segmento relevante:

```sql
-- Particionar orders por customer_id (hash) o por order_date (rango)
-- Ejemplo de particion por rango de fecha (PostgreSQL)
CREATE TABLE orders (
    order_id      BIGINT,
    order_date    DATE NOT NULL,
    customer_id   BIGINT,
    total_amount  DECIMAL(15,2)
) PARTITION BY RANGE (order_date);

CREATE TABLE orders_2024 PARTITION OF orders
    FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');

CREATE TABLE orders_2025 PARTITION OF orders
    FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
```

**Estrategia combinada:** Si la consulta siempre filtra por un rango de
fechas reciente, la BD descarta particiones historicas automaticamente
(partition pruning), reduciendo la data escaneada de 10M a, por ejemplo,
500K registros del periodo actual.

**Estrategia adicional — Desnormalizacion controlada:**
Agregar la columna `country` directamente en la tabla `orders`:

```sql
ALTER TABLE orders ADD COLUMN customer_country VARCHAR(50);
-- Poblar con un backfill
UPDATE orders o SET customer_country = c.country
FROM customers c WHERE o.customer_id = c.customer_id;

CREATE INDEX idx_orders_country ON orders(customer_country);
```

Esto elimina el JOIN completamente. La consulta pasa a ser:

```sql
SELECT order_id, order_date, customer_name, total_amount
FROM orders
WHERE customer_country = 'Mexico';
```

El trade-off es redundancia de dato vs. performance de lectura.

---

## MODULO 4 — Git y GitHub (Versionamiento)

### Escenario
Estoy en `feature/new-login` y necesito incorporar urgentemente un commit
especifico de `main` (correccion de seguridad) sin traer todo el historial.

### Estrategia: `git cherry-pick`

```bash
# 1. Identificar el hash del commit critico en main
git log main --oneline
# Supongamos que el commit es: a3f8c21 "fix: security vulnerability in auth"

# 2. Asegurarme de estar en mi rama de feature
git checkout feature/new-login

# 3. Aplicar SOLO ese commit especifico sobre mi rama actual
git cherry-pick a3f8c21

# 4. Si hay conflictos, resolverlos y luego:
git cherry-pick --continue

# 5. Pushear los cambios
git push origin feature/new-login
```

### Por que cherry-pick y no merge o rebase?

| Comando | Comportamiento | Por que NO en este caso |
|---------|---------------|------------------------|
| `git merge main` | Trae TODOS los commits de main | Incorporaria funcionalidades no probadas ni revisadas |
| `git rebase main` | Reaplica toda la rama sobre main | Mismo problema: trae todos los cambios de main |
| `git cherry-pick <hash>` | Aplica EXACTAMENTE un commit | Es la herramienta precisa para este escenario |

### Consideraciones adicionales

- Si son multiples commits contiguos: `git cherry-pick a3f8c21..b9d4e33`
- Documentar en el mensaje del commit que es un cherry-pick de seguridad:
  `git cherry-pick -x a3f8c21` (el flag `-x` agrega referencia al commit original).
- Al hacer el PR de `feature/new-login`, el revisor vera el commit de seguridad
  ya integrado, evitando duplicados cuando finalmente se mergee con main.

---

## MODULO 5 — Liderazgo Tecnico y Gestion

### Contexto
Equipo de 8 desarrolladores, 40% deuda tecnica, 10 incidentes criticos en
el ultimo mes, entrega en 3 semanas, sin estandares, 2 juniors con brechas.

---

### 1. Las 5 prioridades en las primeras 2 semanas

**Prioridad 1 — Estabilizar la produccion (dias 1-3)**
Los 10 incidentes del mes indican que el sistema no es confiable. Antes de
cualquier funcionalidad nueva, asigno 2 desarrolladores senior exclusivamente
a identificar y resolver los incidentes recurrentes. Creo un "mapa de calor"
de incidentes para encontrar los 2-3 componentes que causan el 80% de los
problemas (regla de Pareto). Sin estabilidad, todo lo demas es construccion
sobre arena.

**Prioridad 2 — Conocimiento del estado real del sistema (dias 1-5)**
Realizo un tech audit con el equipo: mapeo de servicios criticos, nivel de
cobertura de tests, identificacion de la deuda tecnica mas riesgosa (no toda
la deuda es igual). Quiero saber: ¿que deuda tiene probabilidad alta de
convertirse en el incidente 11? Esa es la deuda que ataco primero.

**Prioridad 3 — Establecer el proceso minimo de trabajo (dia 2)**
Defino e implemento inmediatamente: rama de proteccion en main, PR obligatorio
con al menos 1 revisor, y pipeline CI basico (build + tests automaticos antes
de mergear). Esto evita que la deuda crezca mientras trabajamos en reducirla.
Sin este piso, cualquier mejora que hagamos sera erosionada por el siguiente
deploy apresurado.

**Prioridad 4 — Negociar el alcance de la entrega de 3 semanas (dias 1-2)**
Me reuno con el negocio para presentar un "release plan" realista. No prometo
todo; priorizo el 20% de funcionalidades que generan el 80% del valor.
Propongo un MVP con las funcionalidades criticas listas en 3 semanas y el
resto en un sprint siguiente. Es mejor entregar algo funcionando que prometer
todo y entregar nada confiable.

**Prioridad 5 — Plan individual para los juniors (semana 1)**
Asigno un senior como mentor a cada junior con un plan concreto: pair
programming en tickets reales (no ejercicios), revision de sus PRs con
feedback constructivo y escrito. El objetivo es que en 2 semanas puedan
resolver tickets de complejidad media de forma autonoma.

---

### 2. Organizacion del equipo

**Estructura propuesta: squads funcionales + rotacion de guardia**

```
Squad A — Estabilizacion (2 senior)
  → Resolucion de incidentes activos
  → Reduccion de deuda tecnica critica
  → Duracion: 2-3 semanas hasta estabilizar

Squad B — Nueva funcionalidad (3 mid + 1 junior con mentor)
  → Desarrollo del feature requerido por negocio
  → El junior hace pair programming con un mid
  → PRs revisados por el senior de guardia

Soporte transversal (1 senior + 1 junior)
  → Guardia rotativa de produccion (semanas alternas)
  → Documentacion tecnica
  → Mejora del pipeline CI/CD
```

**Ceremonias minimas obligatorias:**
- Daily de 15 min (enfoque en bloqueos, no en status)
- Planning semanal de 1 hora (prioridades claras)
- Retrospectiva quincenal (mejora del proceso)
- Tech review mensual (decision colectiva sobre deuda tecnica)

---

### 3. Metricas para evaluar el desempenio del area

**Metricas de calidad del codigo:**
- Cobertura de tests unitarios (objetivo: >70% en servicios criticos)
- Numero de bugs encontrados en produccion por sprint (tendencia descendente)
- Tiempo medio de resolucion de incidentes (MTTR — Mean Time To Recover)
- Deuda tecnica medida en horas (SonarQube — tendencia descendente)

**Metricas de velocidad y entrega:**
- Lead time: tiempo desde que se crea un ticket hasta que llega a produccion
- Deployment frequency: cuantos deploys exitosos por semana
- Change failure rate: % de deploys que causan incidentes
- Cycle time por tipo de ticket (bug, feature, mejora)

**Metricas del proceso:**
- Tiempo promedio de revision de PR (objetivo: < 24 horas)
- % de PRs que pasan CI sin correcciones en el primer intento
- Numero de incidentes criticos por mes (objetivo: reduccion del 50% en 2 meses)

**Metricas de desarrollo del equipo:**
- Autonomia de juniors: tickets resueltos sin escalamiento (tendencia creciente)
- Participacion en code reviews (todos los miembros deben revisar, no solo los seniors)

---

### 4. Practicas tecnicas obligatorias

**a) Control de versiones y CI/CD**
- Rama `main` protegida: ningun push directo, solo merge via PR aprobado.
- Pipeline CI obligatorio: compilacion + tests + analisis de codigo (SonarQube)
  antes de permitir el merge.
- Convension de commits: Conventional Commits (`feat:`, `fix:`, `refactor:`).

**b) Calidad de codigo**
- Code review obligatorio con checklist: legibilidad, cobertura de edge cases,
  seguridad, performance, adherencia a patrones del equipo.
- Tests unitarios obligatorios para toda logica de negocio nueva.
- Regla del "boy scout": si tocas un archivo, dejalo mejor de como lo encontraste.
- Limites de complejidad ciclomatica en SonarQube (alerta si > 10).

**c) Documentacion**
- Todo endpoint nuevo debe tener contrato OpenAPI actualizado.
- ADR (Architecture Decision Record) para decisiones tecnicas relevantes.
- README actualizado con instrucciones de ejecucion local.

**d) Seguridad**
- Escaneo de dependencias vulnerables en CI (OWASP Dependency Check).
- Ningun secreto en el codigo fuente (usar variables de entorno / Vault).
- Revision de seguridad en PRs que toquen autenticacion o datos sensibles.

---

### 5. Como gestionar la presion del negocio sin comprometer la calidad

**Principio base: transparencia sobre compromisos.**

El negocio no tiene problema con los limites tecnicos; tiene problema con la
falta de visibilidad. Mi enfoque:

**Paso 1 — Hablar el idioma del negocio, no de la tecnologia.**
No digo "tenemos deuda tecnica". Digo: "el 40% de los servicios criticos tiene
un riesgo alto de incidentes en produccion, y eso puede impactar la entrega o
la experiencia del cliente. Aqui esta el plan para mitigarlo en paralelo."

**Paso 2 — Proponer opciones, no problemas.**
En vez de "no podemos hacer todo en 3 semanas", presento:
- Opcion A: Entregamos el 100% del feature en 3 semanas, con riesgo alto de
  calidad y posibles incidentes.
- Opcion B: Entregamos el MVP (funcionalidades criticas) en 3 semanas con
  calidad alta, y el resto en la semana 5. (Esta es mi recomendacion).
- Opcion C: Entregamos todo en 5 semanas con calidad completa.

El negocio elige con informacion completa. El lider tecnico no dice NO;
presenta los trade-offs.

**Paso 3 — Cadencia de comunicacion proactiva.**
Actualizo al negocio cada semana con un status claro: % de avance, riesgos
identificados, ajustes al plan. La sorpresa es el peor enemigo de la confianza.

**Paso 4 — "Calidad no es opcional, es velocidad sostenible".**
Explico con datos: los 10 incidentes del mes anterior costaron X horas de
hotfix, X horas de soporte y X impacto en usuarios. Invertir en calidad
ahora reduce ese costo en los proximos meses. La velocidad sin calidad
es deuda que se paga con intereses.

**Paso 5 — Compromisos realistas y cumplidos.**
Prefiero prometer menos y entregar mas que al reves. La confianza del negocio
en el equipo tecnico se construye con consistencia, no con promesas heroicas.

