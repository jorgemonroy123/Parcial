package com.goldenawards.controller;

import com.goldenawards.model.*;
import com.goldenawards.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Área pública (visitantes sin registro de votante).
 * Catálogo con filtros, comparador, feedback y estadísticas.
 */
@Controller
@RequestMapping("/publico")
public class PublicoControladorWeb {

    private final NominacionRepository nominacionRepository;
    private final JugadorRepository jugadorRepository;
    private final EdicionRepository edicionRepository;
    private final CategoriaRepository categoriaRepository;
    private final FeedbackRepository feedbackRepository;
    private final VotoRepository votoRepository;

    public PublicoControladorWeb(NominacionRepository nominacionRepository, JugadorRepository jugadorRepository,
                                 EdicionRepository edicionRepository, CategoriaRepository categoriaRepository,
                                 FeedbackRepository feedbackRepository, VotoRepository votoRepository) {
        this.nominacionRepository = nominacionRepository;
        this.jugadorRepository = jugadorRepository;
        this.edicionRepository = edicionRepository;
        this.categoriaRepository = categoriaRepository;
        this.feedbackRepository = feedbackRepository;
        this.votoRepository = votoRepository;
    }

    // ---- Catálogo con filtros ----
    @GetMapping({"", "/", "/catalogo"})
    public String catalogo(@RequestParam(required = false) String edicionId,
                           @RequestParam(required = false) String categoriaId,
                           @RequestParam(required = false) String estado,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false, defaultValue = "nombre") String orden,
                           Model model) {
        List<Edicion> ediciones = edicionRepository.findAll();
        List<Categoria> categorias = categoriaId != null || edicionId != null
                ? (edicionId != null ? categoriaRepository.findByEdicionId(edicionId) : categoriaRepository.findAll())
                : categoriaRepository.findAll();

        List<Nominacion> nominaciones;
        if (categoriaId != null && !categoriaId.isBlank() && estado != null && !estado.isBlank()) {
            nominaciones = nominacionRepository.findByCategoriaIdAndEstado(categoriaId, estado);
        } else if (edicionId != null && !edicionId.isBlank() && estado != null && !estado.isBlank()) {
            nominaciones = nominacionRepository.findByEdicionIdAndEstado(edicionId, estado);
        } else if (edicionId != null && !edicionId.isBlank()) {
            nominaciones = nominacionRepository.findByEdicionId(edicionId);
        } else if (estado != null && !estado.isBlank()) {
            nominaciones = nominacionRepository.findByEstado(estado);
        } else {
            nominaciones = nominacionRepository.findAll();
        }

        if (categoriaId != null && !categoriaId.isBlank() && (estado == null || estado.isBlank())) {
            nominaciones = nominaciones.stream()
                    .filter(n -> categoriaId.equals(n.getCategoriaId()))
                    .collect(Collectors.toList());
        }

        Map<String, Jugador> jugadores = new HashMap<>();
        for (Jugador j : jugadorRepository.findAll()) jugadores.put(j.getId(), j);

        Map<String, String> catNombres = new HashMap<>();
        for (Categoria c : categoriaRepository.findAll()) catNombres.put(c.getId(), c.getNombre());

        Map<String, String> edNombres = new HashMap<>();
        for (Edicion e : ediciones) edNombres.put(e.getId(), e.getNombre());

        List<Map<String, Object>> filas = new ArrayList<>();
        for (Nominacion n : nominaciones) {
            Jugador j = jugadores.get(n.getJugadorId());
            if (j == null) continue;
            if (q != null && !q.isBlank()) {
                String qq = q.toLowerCase();
                boolean match = (j.getNombre() != null && j.getNombre().toLowerCase().contains(qq))
                        || (j.getEquipo() != null && j.getEquipo().toLowerCase().contains(qq))
                        || (j.getPais() != null && j.getPais().toLowerCase().contains(qq));
                if (!match) continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("nominacion", n);
            row.put("jugador", j);
            row.put("categoriaNombre", catNombres.getOrDefault(n.getCategoriaId(), "—"));
            row.put("edicionNombre", edNombres.getOrDefault(n.getEdicionId(), "—"));
            filas.add(row);
        }

        Comparator<Map<String, Object>> cmp;
        switch (orden) {
            case "equipo":
                cmp = Comparator.comparing(m -> {
                    Jugador j = (Jugador) m.get("jugador");
                    return j.getEquipo() != null ? j.getEquipo() : "";
                }, String.CASE_INSENSITIVE_ORDER);
                break;
            case "estado":
                cmp = Comparator.comparing(m -> ((Nominacion) m.get("nominacion")).getEstado());
                break;
            default:
                cmp = Comparator.comparing(m -> {
                    Jugador j = (Jugador) m.get("jugador");
                    return j.getNombre() != null ? j.getNombre() : "";
                }, String.CASE_INSENSITIVE_ORDER);
        }
        filas.sort(cmp);

        model.addAttribute("filas", filas);
        model.addAttribute("ediciones", ediciones);
        model.addAttribute("categorias", categorias);
        model.addAttribute("edicionId", edicionId);
        model.addAttribute("categoriaId", categoriaId);
        model.addAttribute("estado", estado);
        model.addAttribute("q", q);
        model.addAttribute("orden", orden);
        return "publico/catalogo";
    }

    // ---- Comparador 2-3 nominados misma categoría ----
    @GetMapping("/comparar")
    public String compararForm(@RequestParam(required = false) String categoriaId, Model model) {
        model.addAttribute("categorias", categoriaRepository.findAll());
        model.addAttribute("categoriaId", categoriaId);
        List<Map<String, Object>> opciones = new ArrayList<>();
        if (categoriaId != null && !categoriaId.isBlank()) {
            List<Nominacion> noms = nominacionRepository.findByCategoriaIdAndEstado(categoriaId, "APROBADA");
            if (noms.isEmpty()) {
                noms = nominacionRepository.findAll().stream()
                        .filter(n -> categoriaId.equals(n.getCategoriaId()))
                        .collect(Collectors.toList());
            }
            Map<String, Jugador> mapJ = new HashMap<>();
            for (Jugador j : jugadorRepository.findAll()) mapJ.put(j.getId(), j);
            for (Nominacion n : noms) {
                Jugador j = mapJ.get(n.getJugadorId());
                if (j == null) continue;
                Map<String, Object> o = new LinkedHashMap<>();
                o.put("nominacionId", n.getId());
                o.put("jugadorId", j.getId());
                o.put("nombre", j.getNombre());
                o.put("equipo", j.getEquipo());
                opciones.add(o);
            }
        }
        model.addAttribute("opciones", opciones);
        return "publico/comparar";
    }

    @PostMapping("/comparar")
    public String compararResultado(@RequestParam String categoriaId,
                                    @RequestParam List<String> jugadorIds,
                                    Model model, RedirectAttributes ra) {
        if (jugadorIds == null || jugadorIds.size() < 2 || jugadorIds.size() > 3) {
            ra.addFlashAttribute("mensaje", "Selecciona 2 o 3 nominados de la misma categoría");
            ra.addFlashAttribute("tipo", "warning");
            return "redirect:/publico/comparar?categoriaId=" + categoriaId;
        }
        List<Jugador> seleccion = new ArrayList<>();
        for (String id : jugadorIds) {
            jugadorRepository.findById(id).ifPresent(seleccion::add);
        }
        Categoria cat = categoriaRepository.findById(categoriaId).orElse(null);
        model.addAttribute("jugadores", seleccion);
        model.addAttribute("categoria", cat);
        return "publico/comparar-resultado";
    }

    // ---- Feedback / encuesta ----
    @GetMapping("/feedback")
    public String feedbackForm(Model model) {
        model.addAttribute("ediciones", edicionRepository.findAll());
        model.addAttribute("feedbacks", feedbackRepository.findAllByOrderByFechaDesc());
        return "publico/feedback";
    }

    @PostMapping("/feedback")
    public String feedbackSubmit(@RequestParam String edicionId,
                                 @RequestParam int estrellas,
                                 @RequestParam(required = false) String comentario,
                                 @RequestParam(required = false) String autorNombre,
                                 RedirectAttributes ra) {
        if (estrellas < 1 || estrellas > 5) {
            ra.addFlashAttribute("mensaje", "La calificación debe ser entre 1 y 5 estrellas");
            ra.addFlashAttribute("tipo", "danger");
            return "redirect:/publico/feedback";
        }
        Edicion ed = edicionRepository.findById(edicionId).orElse(null);
        Feedback f = new Feedback();
        f.setEdicionId(edicionId);
        f.setEdicionNombre(ed != null ? ed.getNombre() : edicionId);
        f.setEstrellas(estrellas);
        f.setComentario(comentario);
        f.setAutorNombre(autorNombre != null && !autorNombre.isBlank() ? autorNombre : "Anónimo");
        f.setFecha(LocalDateTime.now());
        feedbackRepository.save(f);
        ra.addFlashAttribute("mensaje", "¡Gracias por tu opinión!");
        ra.addFlashAttribute("tipo", "success");
        return "redirect:/publico/feedback";
    }

    // ---- Estadísticas interactivas ----
    @GetMapping("/estadisticas")
    public String estadisticas(Model model) {
        List<Nominacion> todas = nominacionRepository.findAll();
        Map<String, String> catNombres = new HashMap<>();
        for (Categoria c : categoriaRepository.findAll()) catNombres.put(c.getId(), c.getNombre());
        Map<String, String> edNombres = new HashMap<>();
        for (Edicion e : edicionRepository.findAll()) edNombres.put(e.getId(), e.getNombre());

        // Nominaciones por categoría
        Map<String, Long> porCategoria = todas.stream()
                .collect(Collectors.groupingBy(n -> catNombres.getOrDefault(n.getCategoriaId(), "Sin categoría"), Collectors.counting()));

        // Nominaciones por edición
        Map<String, Long> porEdicion = todas.stream()
                .collect(Collectors.groupingBy(n -> edNombres.getOrDefault(n.getEdicionId(), "Sin edición"), Collectors.counting()));

        // Por estado
        Map<String, Long> porEstado = todas.stream()
                .collect(Collectors.groupingBy(n -> n.getEstado() != null ? n.getEstado() : "N/A", Collectors.counting()));

        // Votos por categoría (si existen)
        Map<String, Long> votosPorCat = new LinkedHashMap<>();
        try {
            for (Voto v : votoRepository.findAll()) {
                String nombre = catNombres.getOrDefault(v.getCategoriaId(), v.getCategoriaId());
                votosPorCat.merge(nombre, 1L, Long::sum);
            }
        } catch (Exception ignored) {}

        model.addAttribute("labelsCategoria", new ArrayList<>(porCategoria.keySet()));
        model.addAttribute("dataCategoria", new ArrayList<>(porCategoria.values()));
        model.addAttribute("labelsEdicion", new ArrayList<>(porEdicion.keySet()));
        model.addAttribute("dataEdicion", new ArrayList<>(porEdicion.values()));
        model.addAttribute("labelsEstado", new ArrayList<>(porEstado.keySet()));
        model.addAttribute("dataEstado", new ArrayList<>(porEstado.values()));
        model.addAttribute("labelsVotos", new ArrayList<>(votosPorCat.keySet()));
        model.addAttribute("dataVotos", new ArrayList<>(votosPorCat.values()));
        model.addAttribute("totalNominaciones", todas.size());
        model.addAttribute("totalJugadores", jugadorRepository.count());
        model.addAttribute("totalFeedbacks", feedbackRepository.count());
        return "publico/estadisticas";
    }
}
