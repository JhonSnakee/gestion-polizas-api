package org.gestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gestion.application.dto.request.ActualizarPolizaRequest;
import org.gestion.application.dto.request.AgregarRiesgoRequest;
import org.gestion.application.dto.request.RenovarPolizaRequest;
import org.gestion.domain.entity.Poliza;
import org.gestion.domain.entity.Riesgo;
import org.gestion.domain.enums.EstadoPoliza;
import org.gestion.domain.enums.EstadoRiesgo;
import org.gestion.domain.enums.TipoPoliza;
import org.gestion.domain.repository.PolizaRepository;
import org.gestion.domain.repository.RiesgoRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("Tests de integración - API Gestión de Pólizas")
class PolizaControllerIntegrationTest {

    private static final String API_KEY = "123456";
    private static final String API_KEY_HEADER = "x-api-key";
    private static final String BASE_URL = "/api/v1/polizas";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PolizaRepository polizaRepository;

    @Autowired
    private RiesgoRepository riesgoRepository;

    private Poliza polizaIndividual;
    private Poliza polizaColectiva;

    @BeforeEach
    void setUp() {
        riesgoRepository.deleteAll();
        polizaRepository.deleteAll();

        polizaIndividual = polizaRepository.save(Poliza.builder()
                .numeroPoliza("TEST-IND-001")
                .tipo(TipoPoliza.INDIVIDUAL)
                .estado(EstadoPoliza.ACTIVA)
                .tomador("Juan Test")
                .asegurado("Juan Test")
                .beneficiario("Propietario Test")
                .fechaInicioVigencia(LocalDate.of(2025, 1, 1))
                .fechaFinVigencia(LocalDate.of(2026, 1, 1))
                .mesesVigencia(12)
                .valorCanon(new BigDecimal("1000000.00"))
                .valorPrima(new BigDecimal("12000000.00"))
                .build());

        polizaColectiva = polizaRepository.save(Poliza.builder()
                .numeroPoliza("TEST-COL-001")
                .tipo(TipoPoliza.COLECTIVA)
                .estado(EstadoPoliza.ACTIVA)
                .tomador("Inmobiliaria Test")
                .asegurado("Inmobiliaria Test")
                .beneficiario("Propietarios Test")
                .fechaInicioVigencia(LocalDate.of(2025, 1, 1))
                .fechaFinVigencia(LocalDate.of(2026, 1, 1))
                .mesesVigencia(12)
                .valorCanon(new BigDecimal("2000000.00"))
                .valorPrima(new BigDecimal("24000000.00"))
                .build());
    }

    // ─── Seguridad ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /polizas sin API Key debe retornar 401")
    void sinApiKey_debeRetornar401() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /polizas con API Key inválida debe retornar 401")
    void conApiKeyInvalida_debeRetornar401() throws Exception {
        mockMvc.perform(get(BASE_URL).header(API_KEY_HEADER, "wrong-key"))
                .andExpect(status().isUnauthorized());
    }

    // ─── Listar pólizas ────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /polizas debe retornar todas las pólizas")
    void listarPolizas_debeRetornarTodasLasPolizas() throws Exception {
        mockMvc.perform(get(BASE_URL).header(API_KEY_HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("GET /polizas?tipo=INDIVIDUAL debe filtrar por tipo")
    void listarPolizas_filtrarPorTipoIndividual() throws Exception {
        mockMvc.perform(get(BASE_URL).header(API_KEY_HEADER, API_KEY)
                        .param("tipo", "INDIVIDUAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].tipo", is("INDIVIDUAL")));
    }

    @Test
    @DisplayName("GET /polizas?estado=ACTIVA debe filtrar por estado")
    void listarPolizas_filtrarPorEstadoActiva() throws Exception {
        mockMvc.perform(get(BASE_URL).header(API_KEY_HEADER, API_KEY)
                        .param("estado", "ACTIVA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    // ─── Renovar póliza ────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /polizas/{id}/renovar debe renovar con IPC y cambiar estado a RENOVADA")
    void renovarPoliza_debeAplicarIpcYCambiarEstado() throws Exception {
        RenovarPolizaRequest req = new RenovarPolizaRequest();
        req.setIpcPorcentaje(new BigDecimal("10.00"));

        mockMvc.perform(post(BASE_URL + "/" + polizaIndividual.getId() + "/renovar")
                        .header(API_KEY_HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.estado", is("RENOVADA")))
                .andExpect(jsonPath("$.data.valorCanon", is(1100000.00)))
                .andExpect(jsonPath("$.data.contadorRenovaciones", is(1)));
    }

    @Test
    @DisplayName("POST /polizas/{id}/renovar debe fallar si la póliza está CANCELADA")
    void renovarPolizaCancelada_debeLanzarBusinessRuleException() throws Exception {
        polizaIndividual.setEstado(EstadoPoliza.CANCELADA);
        polizaRepository.save(polizaIndividual);

        RenovarPolizaRequest req = new RenovarPolizaRequest();
        req.setIpcPorcentaje(new BigDecimal("5.00"));

        mockMvc.perform(post(BASE_URL + "/" + polizaIndividual.getId() + "/renovar")
                        .header(API_KEY_HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // ─── Cancelar póliza ───────────────────────────────────────────────────

    @Test
    @DisplayName("POST /polizas/{id}/cancelar debe cancelar póliza y todos sus riesgos")
    void cancelarPoliza_debeCancelarPolizaYRiesgos() throws Exception {
        // Agregar riesgo activo a la colectiva
        riesgoRepository.save(Riesgo.builder()
                .descripcion("Riesgo test")
                .direccionInmueble("Calle Test 123")
                .nombreArrendatario("Arrendatario Test")
                .estado(EstadoRiesgo.ACTIVO)
                .fechaInicio(LocalDate.of(2025, 1, 1))
                .fechaFin(LocalDate.of(2026, 1, 1))
                .valorAsegurado(new BigDecimal("1000000"))
                .poliza(polizaColectiva)
                .build());

        mockMvc.perform(post(BASE_URL + "/" + polizaColectiva.getId() + "/cancelar")
                        .header(API_KEY_HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado", is("CANCELADA")))
                .andExpect(jsonPath("$.data.totalRiesgosActivos", is(0)));
    }

    // ─── Riesgos en colectiva ──────────────────────────────────────────────

    @Test
    @DisplayName("POST /polizas/{id}/riesgos debe agregar riesgo a póliza COLECTIVA")
    void agregarRiesgo_enColectiva_debeRetornar201() throws Exception {
        AgregarRiesgoRequest req = new AgregarRiesgoRequest();
        req.setDescripcion("Riesgo nuevo");
        req.setDireccionInmueble("Av. Siempre Viva 123");
        req.setNombreArrendatario("Homero Simpson");
        req.setFechaInicio(LocalDate.of(2025, 1, 1));
        req.setFechaFin(LocalDate.of(2026, 1, 1));
        req.setValorAsegurado(new BigDecimal("1500000"));

        mockMvc.perform(post(BASE_URL + "/" + polizaColectiva.getId() + "/riesgos")
                        .header(API_KEY_HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.descripcion", is("Riesgo nuevo")));
    }

    @Test
    @DisplayName("POST /polizas/{id}/riesgos debe fallar si la póliza es INDIVIDUAL")
    void agregarRiesgo_enIndividual_debeLanzarBusinessRuleException() throws Exception {
        AgregarRiesgoRequest req = new AgregarRiesgoRequest();
        req.setDescripcion("Riesgo inválido");
        req.setDireccionInmueble("Calle 1 # 2-3");
        req.setNombreArrendatario("Arrendatario X");
        req.setFechaInicio(LocalDate.of(2025, 1, 1));
        req.setFechaFin(LocalDate.of(2026, 1, 1));
        req.setValorAsegurado(new BigDecimal("500000"));

        mockMvc.perform(post(BASE_URL + "/" + polizaIndividual.getId() + "/riesgos")
                        .header(API_KEY_HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // ─── Mock CORE ────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /core-mock/evento debe registrar el evento sin requerir API key")
    void coreMock_debeRegistrarEventoSinApiKey() throws Exception {
        String body = "{\"evento\":\"ACTUALIZACION\",\"polizaId\":555}";

        mockMvc.perform(post("/core-mock/evento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    // ─── Obtener póliza por ID ─────────────────────────────────────────────

    @Test
    @DisplayName("GET /polizas/{id} debe retornar la póliza correcta")
    void obtenerPolizaPorId_debeRetornarPoliza() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + polizaIndividual.getId())
                        .header(API_KEY_HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.tipo", is("INDIVIDUAL")))
                .andExpect(jsonPath("$.data.numeroPoliza", is("TEST-IND-001")));
    }

    @Test
    @DisplayName("GET /polizas/{id} con id inexistente debe retornar 404")
    void obtenerPolizaPorId_inexistente_debeRetornar404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/999999")
                        .header(API_KEY_HEADER, API_KEY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // ─── Modificar póliza ─────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /polizas/{id} debe actualizar el canon y recalcular prima")
    void actualizarPoliza_debeActualizarCanonYRecalcularPrima() throws Exception {
        ActualizarPolizaRequest req = new ActualizarPolizaRequest();
        req.setValorCanon(new BigDecimal("1500000.00"));

        mockMvc.perform(put(BASE_URL + "/" + polizaIndividual.getId())
                        .header(API_KEY_HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.valorCanon", is(1500000.00)))
                .andExpect(jsonPath("$.data.valorPrima", is(18000000.00)));
    }

    @Test
    @DisplayName("PUT /polizas/{id} sobre póliza CANCELADA debe retornar 422")
    void actualizarPolizaCancelada_debeRetornar422() throws Exception {
        polizaIndividual.setEstado(EstadoPoliza.CANCELADA);
        polizaRepository.save(polizaIndividual);

        ActualizarPolizaRequest req = new ActualizarPolizaRequest();
        req.setTomador("Nuevo Tomador");

        mockMvc.perform(put(BASE_URL + "/" + polizaIndividual.getId())
                        .header(API_KEY_HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // ─── Cancelar riesgo directo ───────────────────────────────────────────

    @Test
    @DisplayName("POST /riesgos/{id}/cancelar debe cancelar un riesgo de póliza COLECTIVA")
    void cancelarRiesgo_enColectiva_debeCancelarRiesgo() throws Exception {
        // Crear riesgo en colectiva
        Riesgo riesgo = riesgoRepository.save(Riesgo.builder()
                .descripcion("Riesgo a cancelar")
                .direccionInmueble("Calle 50 # 10-20")
                .nombreArrendatario("Arrendatario Test")
                .estado(EstadoRiesgo.ACTIVO)
                .fechaInicio(LocalDate.of(2025, 1, 1))
                .fechaFin(LocalDate.of(2026, 1, 1))
                .valorAsegurado(new BigDecimal("2000000"))
                .poliza(polizaColectiva)
                .build());

        mockMvc.perform(post("/api/v1/riesgos/" + riesgo.getId() + "/cancelar")
                        .header(API_KEY_HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.estado", is("CANCELADO")));
    }

    @Test
    @DisplayName("POST /riesgos/{id}/cancelar sobre riesgo ya cancelado debe retornar 422")
    void cancelarRiesgoYaCancelado_debeRetornar422() throws Exception {
        Riesgo riesgo = riesgoRepository.save(Riesgo.builder()
                .descripcion("Riesgo cancelado")
                .direccionInmueble("Av. Test 456")
                .nombreArrendatario("Arrendatario Y")
                .estado(EstadoRiesgo.CANCELADO)
                .fechaInicio(LocalDate.of(2025, 1, 1))
                .fechaFin(LocalDate.of(2026, 1, 1))
                .valorAsegurado(new BigDecimal("1000000"))
                .poliza(polizaColectiva)
                .build());

        mockMvc.perform(post("/api/v1/riesgos/" + riesgo.getId() + "/cancelar")
                        .header(API_KEY_HEADER, API_KEY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // ─── Regla: póliza INDIVIDUAL = 1 riesgo máximo ───────────────────────

    @Test
    @DisplayName("POST /polizas/{id}/riesgos en INDIVIDUAL siempre debe retornar 422")
    void agregarRiesgoEnIndividualConRiesgoExistente_debeRetornar422() throws Exception {
        // Agregar riesgo previo a la individual (simulando estado con riesgo)
        riesgoRepository.save(Riesgo.builder()
                .descripcion("Riesgo existente")
                .direccionInmueble("Calle 1 # 1-1")
                .nombreArrendatario("Arrendatario Existente")
                .estado(EstadoRiesgo.ACTIVO)
                .fechaInicio(LocalDate.of(2025, 1, 1))
                .fechaFin(LocalDate.of(2026, 1, 1))
                .valorAsegurado(new BigDecimal("1000000"))
                .poliza(polizaIndividual)
                .build());

        AgregarRiesgoRequest req = new AgregarRiesgoRequest();
        req.setDescripcion("Segundo riesgo no permitido");
        req.setDireccionInmueble("Calle 2 # 2-2");
        req.setNombreArrendatario("Segundo Arrendatario");
        req.setFechaInicio(LocalDate.of(2025, 1, 1));
        req.setFechaFin(LocalDate.of(2026, 1, 1));
        req.setValorAsegurado(new BigDecimal("500000"));

        mockMvc.perform(post(BASE_URL + "/" + polizaIndividual.getId() + "/riesgos")
                        .header(API_KEY_HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success", is(false)));
    }
}

