package com.goldenawards.controller;

import com.goldenawards.model.*;
import com.goldenawards.repository.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Rol JURADO: califica de 1 a 10 a los nominados aprobados mientras la edicion esta en VOTACION.
 * Su calificacion se combina con los votos del publico en el ranking (ver config/Ponderacion).
 */
@Controller
@RequestMapping("/jurado")
public class JuradoControladorWeb {

    private final UsuarioRepository usuarioRepository;
    private final EdicionRepository edicionRepository;
    private final CategoriaRepository categoriaRepository;
    private final NominacionRepository nominacionRepository;
    private final JugadorRepository jugadorRepository;
    private final PuntajeRepository puntajeRepository;

    public JuradoControladorWeb(UsuarioRepository usuarioRepository, EdicionRepository edicionRepository,
                                CategoriaRepository categoriaRepository, NominacionRepository nominacionRepository,
                                JugadorRepository jugadorRepository, PuntajeRepository puntajeRepository) {
        this.usuarioRepository = usuarioRepository;
        this.edicionRepository = edicionRepository;
        this.categoriaRepository = categoriaRepository;
        this.nominacionRepository = nominacionRepository;
        this.jugadorRepository = jugadorRepository;
        this.puntajeRepository = puntajeRepository;
    }

    private String resultado(RedirectAttributes ra, boolean exito, String mensaje, String destino) {
        ra.addFlashAttribute("mensaje", mensaje);
        ra.addFlashAttribute("tipo", exito ? "success" : "danger");
        return "redirect:" + destino;
    }

    private Usuario actual(Principal principal) {
        return usuarioRepository.findByUsername(principal.getName()).orElseThrow();
    }

    // ===================== EVALUAR =====================

    @GetMapping("/evaluar")
    public String evaluar(Principal principal, Model model) {
        Usuario u = actual(principal);
        List<Edicion> abiertas = edicionRepository.findByEstadoIn(List.of("VOTACION")).stream()
                .sorted(Comparator.comparingInt(Edicion::getAnio).reversed())
                .collect(Collectors.toList());

        Map<String, List<Categoria>> categoriasPorEdicion = new HashMap<>();
        Map<String, List<Nominacion>> nominacionesPorCategoria = new HashMap<>();
        for (Edicion e : abiertas) {
            List<Categoria> categorias = categoriaRepository.findByEdicionId(e.getId());
            categoriasPorEdicion.put(e.getId(), categorias);
            for (Categoria c : categorias) {
                nominacionesPorCategoria.put(c.getId(),
                        nominacionRepository.findByCategoriaIdAndEstado(c.getId(), "APROBADA"));
            }
        }
        // nominacionId -> calificacion que este jurado ya dio
        Map<String, Puntaje> misPuntajes = puntajeRepository.findByJuradoId(u.getId()).stream()
                .collect(Collectors.toMap(Puntaje::getNominacionId, p -> p, (a, b) -> a));

        model.addAttribute("ediciones", abiertas);
        model.addAttribute("categoriasPorEdicion", categoriasPorEdicion);
        model.addAttribute("nominacionesPorCategoria", nominacionesPorCategoria);
        model.addAttribute("misPuntajes", misPuntajes);
        model.addAttribute("jugadoresPorId", jugadorRepository.findAll().stream()
                .collect(Collectors.toMap(Jugador::getId, j -> j)));
        return "jurado/evaluar";
    }

    // Crea la calificacion o, si ya existia, la actualiza (mientras la edicion siga en VOTACION)
    @PostMapping("/puntuar")
    public String puntuar(@RequestParam String nominacionId,
                          @RequestParam int puntaje,
                          @RequestParam(required = false) String comentario,
                          Principal principal, RedirectAttributes ra) {
        String destino = "/jurado/evaluar";
        if (puntaje < 1 || puntaje > 10) {
            return resultado(ra, false, "El puntaje debe estar entre 1 y 10", destino);
        }
        String texto = comentario == null ? "" : comentario.trim();
        if (texto.length() > 300) {
            return resultado(ra, false, "El comentario no puede superar 300 caracteres", destino);
        }
        Nominacion n = nominacionRepository.findById(nominacionId).orElse(null);
        if (n == null || !"APROBADA".equals(n.getEstado())) {
            return resultado(ra, false, "Solo se puede calificar a nominados aprobados", destino);
        }
        Edicion e = edicionRepository.findById(n.getEdicionId()).orElse(null);
        if (e == null || !"VOTACION".equals(e.getEstado())) {
            return resultado(ra, false, "La edicion no esta en periodo de votacion", destino);
        }
        Usuario u = actual(principal);
        Puntaje p = puntajeRepository.findByJuradoIdAndNominacionId(u.getId(), nominacionId).orElse(null);
        boolean esNuevo = p == null;
        if (esNuevo) {
            p = new Puntaje();
            p.setEdicionId(n.getEdicionId());
            p.setCategoriaId(n.getCategoriaId());
            p.setNominacionId(n.getId());
            p.setJuradoId(u.getId());
        }
        p.setPuntaje(puntaje);
        p.setComentario(texto);
        p.setFecha(LocalDateTime.now());
        try {
            puntajeRepository.save(p);
        } catch (DuplicateKeyException ex) {
            return resultado(ra, false, "Ya calificaste a este nominado; vuelve a intentarlo", destino);
        }
        return resultado(ra, true, esNuevo ? "Calificacion registrada" : "Calificacion actualizada", destino);
    }

    // ===================== HISTORIAL =====================

    @GetMapping("/historial")
    public String historial(Principal principal, Model model) {
        Usuario u = actual(principal);
        List<Puntaje> puntajes = puntajeRepository.findByJuradoId(u.getId()).stream()
                .sorted(Comparator.comparing(Puntaje::getFecha).reversed())
                .collect(Collectors.toList());
        model.addAttribute("puntajes", puntajes);
        model.addAttribute("edicionesPorId", edicionRepository.findAll().stream()
                .collect(Collectors.toMap(Edicion::getId, e -> e)));
        model.addAttribute("categoriasPorId", categoriaRepository.findAll().stream()
                .collect(Collectors.toMap(Categoria::getId, c -> c)));
        model.addAttribute("jugadoresPorId", jugadorRepository.findAll().stream()
                .collect(Collectors.toMap(Jugador::getId, j -> j)));
        model.addAttribute("nominacionesPorId", nominacionRepository.findAll().stream()
                .collect(Collectors.toMap(Nominacion::getId, n -> n)));
        return "jurado/historial";
    }
}
