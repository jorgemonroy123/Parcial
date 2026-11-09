package com.goldenawards.controller;

import com.goldenawards.model.Usuario;
import com.goldenawards.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class InicioControladorWeb {

    private final UsuarioRepository usuarioRepository;
    private final JugadorRepository jugadorRepository;
    private final ParametroRepository parametroRepository;
    private final EdicionRepository edicionRepository;
    private final NominacionRepository nominacionRepository;
    private final VotoRepository votoRepository;
    private final PuntajeRepository puntajeRepository;
    private final NoticiaRepository noticiaRepository;
    private final AuditorControladorRest auditorRest;

    public InicioControladorWeb(UsuarioRepository usuarioRepository, JugadorRepository jugadorRepository,
                                ParametroRepository parametroRepository, EdicionRepository edicionRepository,
                                NominacionRepository nominacionRepository, VotoRepository votoRepository,
                                PuntajeRepository puntajeRepository, NoticiaRepository noticiaRepository,
                                AuditorControladorRest auditorRest) {
        this.usuarioRepository = usuarioRepository;
        this.jugadorRepository = jugadorRepository;
        this.parametroRepository = parametroRepository;
        this.edicionRepository = edicionRepository;
        this.nominacionRepository = nominacionRepository;
        this.votoRepository = votoRepository;
        this.puntajeRepository = puntajeRepository;
        this.noticiaRepository = noticiaRepository;
        this.auditorRest = auditorRest;
    }

    @GetMapping("/")
    public String inicio() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // Panel de bienvenida: las tarjetas cambian segun el rol del usuario
    @GetMapping("/panel")
    public String panel(Authentication auth, Model model) {
        Usuario u = usuarioRepository.findByUsername(auth.getName()).orElseThrow();
        List<Map<String, Object>> tarjetas = new ArrayList<>();
        String rolNombre;

        switch (u.getRol()) {
            case "ADMIN" -> {
                rolNombre = "Administrador";
                tarjetas.add(tarjeta("Usuarios", usuarioRepository.count(), "bi-people-fill", "/admin/usuarios", "Gestiona cuentas y roles"));
                tarjetas.add(tarjeta("Jugadores", jugadorRepository.count(), "bi-person-badge-fill", "/admin/jugadores", "Catalogo de jugadores"));
                tarjetas.add(tarjeta("Parametros", parametroRepository.count(), "bi-sliders", "/admin/parametros", "Configuracion global"));
            }
            case "ORGANIZADOR" -> {
                rolNombre = "Organizador";
                long pendientes = nominacionRepository.findAll().stream()
                        .filter(n -> "PENDIENTE".equals(n.getEstado())).count();
                tarjetas.add(tarjeta("Ediciones", edicionRepository.count(), "bi-trophy-fill", "/organizador/ediciones", "Ediciones, categorias y periodos"));
                tarjetas.add(tarjeta("Nominaciones pendientes", pendientes, "bi-patch-question-fill", "/organizador/nominaciones", "Aprueba o rechaza propuestas"));
                tarjetas.add(tarjeta("Resultados", edicionRepository.count(), "bi-bar-chart-fill", "/resultados", "Conteo en vivo"));
            }
            case "VOTANTE" -> {
                rolNombre = "Votante";
                tarjetas.add(tarjeta("Ediciones abiertas", edicionRepository.findByEstadoIn(List.of("NOMINACION", "VOTACION")).size(),
                        "bi-check2-square", "/votante/votar", "Vota o propone nominados"));
                tarjetas.add(tarjeta("Mis votos", votoRepository.findByVotanteId(u.getId()).size(),
                        "bi-clock-history", "/votante/historial", "Tu historial de participacion"));
                tarjetas.add(tarjeta("Resultados", edicionRepository.findAll().stream().filter(e -> e.isResultadosPublicados()).count(),
                        "bi-bar-chart-fill", "/resultados", "Rankings publicados"));
            }
            case "NOMINADO" -> {
                rolNombre = "Nominado";
                long mias = u.getJugadorId() == null ? 0 : nominacionRepository.findByJugadorId(u.getJugadorId()).size();
                tarjetas.add(tarjeta("Mis nominaciones", mias, "bi-star-fill", "/nominado/perfil", "Tu perfil y tus categorias"));
                tarjetas.add(tarjeta("Resultados", edicionRepository.findAll().stream().filter(e -> e.isResultadosPublicados()).count(),
                        "bi-bar-chart-fill", "/resultados", "Resultados oficiales"));
            }
            case "JURADO" -> {
                rolNombre = "Jurado";
                tarjetas.add(tarjeta("Ediciones por evaluar", edicionRepository.findByEstadoIn(List.of("VOTACION")).size(),
                        "bi-clipboard2-check-fill", "/jurado/evaluar", "Califica a los nominados de 1 a 10"));
                tarjetas.add(tarjeta("Mis calificaciones", puntajeRepository.findByJuradoId(u.getId()).size(),
                        "bi-clock-history", "/jurado/historial", "Tu historial de puntajes"));
                tarjetas.add(tarjeta("Noticias", noticiaRepository.findByEstado("PUBLICADA").size(),
                        "bi-newspaper", "/publico/noticias", "Lo último del premio"));
            }
            case "AUDITOR" -> {
                rolNombre = "Auditor";
                tarjetas.add(tarjeta("Registros de votación", auditoriaTotal(), "bi-journal-text",
                        "/auditor/registros", "Quién votó y cuándo (sin ver por quién)"));
                tarjetas.add(tarjeta("Anomalías", auditorRest.detectarAnomalias(null).size(), "bi-exclamation-triangle-fill",
                        "/auditor/anomalias", "Votos duplicados e intentos rechazados"));
                tarjetas.add(tarjeta("Exportar reporte", auditoriaTotal(), "bi-file-earmark-spreadsheet-fill",
                        "/auditor/exportar", "Descarga el registro en CSV"));
            }
            case "PERIODISTA" -> {
                rolNombre = "Periodista";
                tarjetas.add(tarjeta("Mis noticias", noticiaRepository.findByAutorId(u.getId()).size(),
                        "bi-pencil-square", "/periodista/noticias", "Redacta y publica tus crónicas"));
                tarjetas.add(tarjeta("Noticias publicadas", noticiaRepository.findByEstado("PUBLICADA").size(),
                        "bi-newspaper", "/publico/noticias", "Así las ve el público"));
            }
            case "MODERADOR" -> {
                rolNombre = "Moderador";
                tarjetas.add(tarjeta("Moderación", 0, "bi-shield-check", "/moderador/inicio", "Módulo del Sprint 2"));
                tarjetas.add(tarjeta("Noticias", noticiaRepository.findByEstado("PUBLICADA").size(),
                        "bi-newspaper", "/publico/noticias", "Lo último del premio"));
            }
            case "REPRESENTANTE" -> {
                rolNombre = "Representante del nominado";
                tarjetas.add(tarjeta("Mi nominado", 0, "bi-person-lines-fill", "/representante/inicio", "Módulo del Sprint 2"));
                tarjetas.add(tarjeta("Noticias", noticiaRepository.findByEstado("PUBLICADA").size(),
                        "bi-newspaper", "/publico/noticias", "Lo último del premio"));
            }
            case "SOPORTE" -> {
                rolNombre = "Soporte técnico";
                tarjetas.add(tarjeta("Tickets", 0, "bi-headset", "/soporte/inicio", "Módulo del Sprint 2"));
                tarjetas.add(tarjeta("Noticias", noticiaRepository.findByEstado("PUBLICADA").size(),
                        "bi-newspaper", "/publico/noticias", "Lo último del premio"));
            }
            default -> {
                rolNombre = "Publico";
                tarjetas.add(tarjeta("Ediciones publicadas", edicionRepository.findAll().stream().filter(e -> e.isResultadosPublicados()).count(),
                        "bi-bar-chart-fill", "/resultados", "Rankings y estadisticas"));
                tarjetas.add(tarjeta("Votos emitidos", votoRepository.count(),
                        "bi-heart-fill", "/resultados", "Participacion total"));
            }
        }

        model.addAttribute("usuario", u);
        model.addAttribute("rolNombre", rolNombre);
        model.addAttribute("tarjetas", tarjetas);
        return "panel";
    }

    private long auditoriaTotal() {
        return auditorRest.filtrar(null, null, null).size();
    }

    private Map<String, Object> tarjeta(String titulo, long valor, String icono, String enlace, String detalle) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("titulo", titulo);
        t.put("valor", valor);
        t.put("icono", icono);
        t.put("enlace", enlace);
        t.put("detalle", detalle);
        return t;
    }
}
