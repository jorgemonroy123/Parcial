package com.goldenawards;

import com.goldenawards.config.SecurityConfig;
import com.goldenawards.repository.*;
import com.goldenawards.security.jwt.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T008 - Pruebas de acceso denegado (403) por rol.
 * Trazabilidad: REQ-ROL-002 y NFR-SEG-001.
 * Carga solo la capa web con la seguridad real y los repositorios simulados (no necesita MongoDB).
 */
@WebMvcTest
@Import({SecurityConfig.class, JwtUtil.class})
class AccesoPorRolTest {

    @Autowired
    private MockMvc mvc;

    // Los controladores necesitan estos repositorios; aqui son simulados
    @MockBean private UsuarioRepository usuarioRepository;
    @MockBean private JugadorRepository jugadorRepository;
    @MockBean private EdicionRepository edicionRepository;
    @MockBean private CategoriaRepository categoriaRepository;
    @MockBean private NominacionRepository nominacionRepository;
    @MockBean private VotoRepository votoRepository;
    @MockBean private ParametroRepository parametroRepository;
    @MockBean private PuntajeRepository puntajeRepository;
    @MockBean private FeedbackRepository feedbackRepository;
    @MockBean private RegistroAuditoriaRepository registroAuditoriaRepository;
    @MockBean private NoticiaRepository noticiaRepository;

    // Roles del sistema (los 10 que cuentan) y una ruta web y una de API de cada uno
    private static final List<String> ROLES = List.of("ADMIN", "ORGANIZADOR", "VOTANTE", "NOMINADO", "JURADO",
            "AUDITOR", "MODERADOR", "PERIODISTA", "REPRESENTANTE", "SOPORTE");

    private static final Map<String, List<String>> RUTAS = new LinkedHashMap<>();

    static {
        RUTAS.put("ADMIN", List.of("/admin/usuarios", "/api/admin/usuarios"));
        RUTAS.put("ORGANIZADOR", List.of("/organizador/ediciones", "/api/organizador/ediciones"));
        RUTAS.put("VOTANTE", List.of("/votante/historial", "/api/votante/votos"));
        RUTAS.put("NOMINADO", List.of("/nominado/perfil", "/api/nominado/perfil"));
        RUTAS.put("JURADO", List.of("/jurado/historial", "/api/jurado/puntajes"));
        RUTAS.put("AUDITOR", List.of("/auditor/registros", "/api/auditor/registros"));
        RUTAS.put("PERIODISTA", List.of("/periodista/noticias", "/api/periodista/noticias"));
        RUTAS.put("MODERADOR", List.of("/moderador/inicio"));
        RUTAS.put("REPRESENTANTE", List.of("/representante/inicio"));
        RUTAS.put("SOPORTE", List.of("/soporte/inicio"));
    }

    // Cada ruta con cada rol que NO es su propietario
    static Stream<Arguments> rolAjeno() {
        List<Arguments> casos = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : RUTAS.entrySet()) {
            for (String ruta : e.getValue()) {
                for (String rol : ROLES) {
                    if (!rol.equals(e.getKey())) {
                        casos.add(arguments(rol, ruta));
                    }
                }
            }
        }
        return casos.stream();
    }

    // Cada ruta con su rol propietario
    static Stream<Arguments> rolPropietario() {
        List<Arguments> casos = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : RUTAS.entrySet()) {
            for (String ruta : e.getValue()) {
                casos.add(arguments(e.getKey(), ruta));
            }
        }
        return casos.stream();
    }

    @ParameterizedTest(name = "{0} NO puede entrar a {1} (403)")
    @MethodSource("rolAjeno")
    void unRolAjenoRecibe403(String rol, String ruta) throws Exception {
        mvc.perform(get(ruta).with(user("prueba").roles(rol)))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} SI pasa la seguridad en {1}")
    @MethodSource("rolPropietario")
    void elRolPropietarioNoRecibe403(String rol, String ruta) {
        // El controlador corre con repositorios simulados y puede fallar por falta de datos;
        // lo unico que se comprueba aqui es que la SEGURIDAD no lo bloquea.
        try {
            int estado = mvc.perform(get(ruta).with(user("prueba").roles(rol)))
                    .andReturn().getResponse().getStatus();
            assertNotEquals(403, estado, "el rol " + rol + " no debe recibir 403 en " + ruta);
        } catch (Exception errorDelControlador) {
            // no fue la seguridad: la peticion llego al controlador
        }
    }

    @Test
    void lasNoticiasPublicasNoPidenSesion() throws Exception {
        mvc.perform(get("/api/publico/noticias")).andExpect(status().isOk());
    }

    @Test
    void unaNoticiaQueNoEstaPublicadaResponde404() throws Exception {
        // REQ-PER-002: el repositorio simulado no devuelve la noticia, igual que un borrador
        mvc.perform(get("/api/publico/noticias/inexistente")).andExpect(status().isNotFound());
    }
}
