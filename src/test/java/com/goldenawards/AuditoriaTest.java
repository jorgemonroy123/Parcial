package com.goldenawards;

import com.goldenawards.controller.AuditorControladorRest;
import com.goldenawards.controller.JuradoControladorRest;
import com.goldenawards.controller.VotanteControladorRest;
import com.goldenawards.model.*;
import com.goldenawards.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * T016 - Pruebas de aceptacion del Auditor (Sprint 1).
 *  AC-AUD-001.1: un voto crea un registro con usuario, fecha y categoria, SIN el nominado elegido.
 *  AC-AUD-004.1: la exportacion filtrada por edicion contiene solo los registros de esa edicion.
 *  AC-AUD-003: se detectan votos duplicados e intentos rechazados.
 *  NFR-AUD-001: el registro no tiene ningun campo que identifique al nominado.
 *  NFR-REN-001: el listado tiene un limite por consulta.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditoriaTest {

    @Mock private UsuarioRepository usuarios;
    @Mock private EdicionRepository ediciones;
    @Mock private CategoriaRepository categorias;
    @Mock private NominacionRepository nominaciones;
    @Mock private VotoRepository votos;
    @Mock private JugadorRepository jugadores;
    @Mock private PuntajeRepository puntajes;
    @Mock private RegistroAuditoriaRepository auditoria;

    private final Principal votantePrincipal = () -> "votante";
    private final Principal juradoPrincipal = () -> "jurado";

    private Usuario votante;
    private Usuario jurado;
    private Nominacion nominacion;
    private Edicion edicion;

    @BeforeEach
    void preparar() {
        votante = usuario("U1", "votante", "VOTANTE");
        jurado = usuario("U2", "jurado", "JURADO");

        nominacion = new Nominacion();
        nominacion.setId("N1");
        nominacion.setEdicionId("E1");
        nominacion.setCategoriaId("C1");
        nominacion.setJugadorId("J1");
        nominacion.setEstado("APROBADA");

        edicion = new Edicion();
        edicion.setId("E1");
        edicion.setNombre("Golden Awards 2026");
        edicion.setEstado("VOTACION");

        when(usuarios.findByUsername("votante")).thenReturn(Optional.of(votante));
        when(usuarios.findByUsername("jurado")).thenReturn(Optional.of(jurado));
        when(nominaciones.findById("N1")).thenReturn(Optional.of(nominacion));
        when(ediciones.findById("E1")).thenReturn(Optional.of(edicion));
        when(votos.save(any(Voto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(puntajes.save(any(Puntaje.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Usuario usuario(String id, String username, String rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setUsername(username);
        u.setRol(rol);
        return u;
    }

    private RegistroAuditoria registro(String tipo, String username, String edicionId, String categoriaId, int minuto) {
        RegistroAuditoria r = new RegistroAuditoria();
        r.setTipo(tipo);
        r.setUsuarioId("id-" + username);
        r.setUsername(username);
        r.setRol("VOTANTE");
        r.setEdicionId(edicionId);
        r.setCategoriaId(categoriaId);
        r.setCanal("WEB");
        r.setFecha(LocalDateTime.of(2026, 11, 10, 10, minuto));
        return r;
    }

    private RegistroAuditoria registroGuardado() {
        ArgumentCaptor<RegistroAuditoria> captor = ArgumentCaptor.forClass(RegistroAuditoria.class);
        verify(auditoria).save(captor.capture());
        return captor.getValue();
    }

    // ---------- AC-AUD-001.1 ----------

    @Test
    void unVotoCreaUnRegistroConUsuarioFechaYCategoriaSinElNominado() {
        VotanteControladorRest controlador =
                new VotanteControladorRest(usuarios, ediciones, categorias, nominaciones, votos, jugadores, auditoria);
        Voto pedido = new Voto();
        pedido.setNominacionId("N1");

        ResponseEntity<Object> respuesta = controlador.votar(pedido, votantePrincipal);

        assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
        RegistroAuditoria r = registroGuardado();
        assertEquals("VOTO", r.getTipo());
        assertEquals("U1", r.getUsuarioId());
        assertEquals("votante", r.getUsername());
        assertEquals("E1", r.getEdicionId());
        assertEquals("C1", r.getCategoriaId());
        assertEquals("API", r.getCanal());
        assertNotNull(r.getFecha());
    }

    @Test
    void unSegundoVotoSeRechazaYQuedaComoIntentoDuplicado() {
        when(votos.existsByVotanteIdAndCategoriaId("U1", "C1")).thenReturn(true);
        VotanteControladorRest controlador =
                new VotanteControladorRest(usuarios, ediciones, categorias, nominaciones, votos, jugadores, auditoria);
        Voto pedido = new Voto();
        pedido.setNominacionId("N1");

        ResponseEntity<Object> respuesta = controlador.votar(pedido, votantePrincipal);

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        verify(votos, never()).save(any(Voto.class));
        assertEquals("INTENTO_DUPLICADO", registroGuardado().getTipo());
    }

    @Test
    void unaCalificacionDelJuradoSeRegistraSinNominadoNiNota() {
        when(puntajes.findByJuradoIdAndNominacionId("U2", "N1")).thenReturn(Optional.empty());
        JuradoControladorRest controlador =
                new JuradoControladorRest(usuarios, ediciones, categorias, nominaciones, jugadores, puntajes, auditoria);
        Puntaje pedido = new Puntaje();
        pedido.setNominacionId("N1");
        pedido.setPuntaje(8);

        ResponseEntity<Object> respuesta = controlador.puntuar(pedido, juradoPrincipal);

        assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
        RegistroAuditoria r = registroGuardado();
        assertEquals("CALIFICACION", r.getTipo());
        assertEquals("U2", r.getUsuarioId());
        assertEquals("C1", r.getCategoriaId());
    }

    @Test
    void corregirUnaCalificacionYaExistenteNoCreaOtroRegistro() {
        Puntaje existente = new Puntaje();
        existente.setNominacionId("N1");
        existente.setPuntaje(5);
        when(puntajes.findByJuradoIdAndNominacionId("U2", "N1")).thenReturn(Optional.of(existente));
        JuradoControladorRest controlador =
                new JuradoControladorRest(usuarios, ediciones, categorias, nominaciones, jugadores, puntajes, auditoria);
        Puntaje pedido = new Puntaje();
        pedido.setNominacionId("N1");
        pedido.setPuntaje(9);

        ResponseEntity<Object> respuesta = controlador.puntuar(pedido, juradoPrincipal);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        verify(auditoria, never()).save(any(RegistroAuditoria.class));
    }

    // ---------- NFR-AUD-001 ----------

    @Test
    void elRegistroNoTieneNingunCampoQueIdentifiqueAlNominadoNiLaNota() {
        for (Field campo : RegistroAuditoria.class.getDeclaredFields()) {
            String nombre = campo.getName().toLowerCase();
            assertFalse(nombre.contains("nomin"), "campo prohibido: " + campo.getName());
            assertFalse(nombre.contains("jugador"), "campo prohibido: " + campo.getName());
            assertFalse(nombre.contains("puntaje"), "campo prohibido: " + campo.getName());
        }
    }

    // ---------- AC-AUD-004.1 ----------

    @Test
    void laExportacionPorEdicionTraeSoloLosRegistrosDeEsaEdicion() {
        RegistroAuditoria a = registro("VOTO", "ana", "E1", "C1", 1);
        RegistroAuditoria b = registro("VOTO", "beto", "E1", "C1", 2);
        RegistroAuditoria deOtraEdicion = registro("VOTO", "carla", "E2", "C9", 3);
        when(auditoria.findByEdicionId("E1")).thenReturn(List.of(a, b));
        when(auditoria.findAll()).thenReturn(List.of(a, b, deOtraEdicion));
        when(ediciones.findAll()).thenReturn(List.of(edicion));
        Categoria cat = new Categoria();
        cat.setId("C1");
        cat.setNombre("Balon de Oro");
        when(categorias.findAll()).thenReturn(List.of(cat));
        AuditorControladorRest auditor = new AuditorControladorRest(auditoria, ediciones, categorias);

        ResponseEntity<byte[]> respuesta = auditor.exportar("E1", null, null);

        String csv = new String(respuesta.getBody(), StandardCharsets.UTF_8).replace("\uFEFF", "");
        String[] lineas = csv.trim().split("\n");
        assertEquals(3, lineas.length, "encabezado + 2 registros");
        assertTrue(lineas[0].startsWith("Fecha;Tipo;Usuario"));
        assertTrue(csv.contains("ana") && csv.contains("beto"));
        assertFalse(csv.contains("carla"), "no debe incluir otra edicion");
        assertNotNull(respuesta.getHeaders().getContentDisposition().getFilename());
        assertTrue(respuesta.getHeaders().getContentDisposition().getFilename().endsWith(".csv"));
    }

    // ---------- REQ-AUD-003 ----------

    @Test
    void seDetectanVotosDuplicadosEIntentosRechazados() {
        when(auditoria.findAll()).thenReturn(List.of(
                registro("VOTO", "ana", "E1", "C1", 1),
                registro("VOTO", "ana", "E1", "C1", 2),                 // mismo usuario, misma categoria
                registro("VOTO", "beto", "E1", "C1", 3),                // normal
                registro("INTENTO_DUPLICADO", "carla", "E1", "C1", 4))); // intento rechazado
        when(ediciones.findAll()).thenReturn(List.of(edicion));
        AuditorControladorRest auditor = new AuditorControladorRest(auditoria, ediciones, categorias);

        List<Map<String, Object>> anomalias = auditor.detectarAnomalias(null);

        assertEquals(2, anomalias.size());
        assertTrue(anomalias.stream().anyMatch(a -> "VOTO_DUPLICADO".equals(a.get("anomalia"))
                && "ana".equals(a.get("usuario")) && Long.valueOf(2).equals(a.get("cantidad"))));
        assertTrue(anomalias.stream().anyMatch(a -> "INTENTO_DUPLICADO".equals(a.get("anomalia"))
                && "carla".equals(a.get("usuario"))));
    }

    // ---------- NFR-REN-001 ----------

    @Test
    void elListadoTieneUnLimitePorConsulta() {
        AuditorControladorRest auditor = new AuditorControladorRest(auditoria, ediciones, categorias);
        assertEquals(AuditorControladorRest.LIMITE_POR_DEFECTO, auditor.normalizarLimite(null));
        assertEquals(AuditorControladorRest.LIMITE_POR_DEFECTO, auditor.normalizarLimite(0));
        assertEquals(AuditorControladorRest.LIMITE_MAXIMO, auditor.normalizarLimite(50000));
        assertEquals(25, auditor.normalizarLimite(25));
    }
}
