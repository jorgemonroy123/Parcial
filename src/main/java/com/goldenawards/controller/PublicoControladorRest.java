package com.goldenawards.controller;

import com.goldenawards.model.*;
import com.goldenawards.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/publico")
public class PublicoControladorRest {

    private final NominacionRepository nominacionRepository;
    private final JugadorRepository jugadorRepository;
    private final EdicionRepository edicionRepository;
    private final CategoriaRepository categoriaRepository;
    private final FeedbackRepository feedbackRepository;

    public PublicoControladorRest(NominacionRepository nominacionRepository, JugadorRepository jugadorRepository,
                                  EdicionRepository edicionRepository, CategoriaRepository categoriaRepository,
                                  FeedbackRepository feedbackRepository) {
        this.nominacionRepository = nominacionRepository;
        this.jugadorRepository = jugadorRepository;
        this.edicionRepository = edicionRepository;
        this.categoriaRepository = categoriaRepository;
        this.feedbackRepository = feedbackRepository;
    }

    @GetMapping("/catalogo")
    public List<Map<String, Object>> catalogo(
            @RequestParam(required = false) String edicionId,
            @RequestParam(required = false) String categoriaId,
            @RequestParam(required = false) String estado) {
        List<Nominacion> nominaciones;
        if (categoriaId != null && estado != null) {
            nominaciones = nominacionRepository.findByCategoriaIdAndEstado(categoriaId, estado);
        } else if (edicionId != null) {
            nominaciones = nominacionRepository.findByEdicionId(edicionId);
        } else if (estado != null) {
            nominaciones = nominacionRepository.findByEstado(estado);
        } else {
            nominaciones = nominacionRepository.findAll();
        }
        Map<String, Jugador> jmap = new HashMap<>();
        for (Jugador j : jugadorRepository.findAll()) jmap.put(j.getId(), j);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Nominacion n : nominaciones) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("nominacion", n);
            m.put("jugador", jmap.get(n.getJugadorId()));
            out.add(m);
        }
        return out;
    }

    @GetMapping("/jugadores/{id}")
    public ResponseEntity<Jugador> jugador(@PathVariable String id) {
        return jugadorRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/feedback")
    public Feedback feedback(@RequestBody Feedback f) {
        f.setFecha(LocalDateTime.now());
        if (f.getAutorNombre() == null || f.getAutorNombre().isBlank()) f.setAutorNombre("Anónimo");
        edicionRepository.findById(f.getEdicionId()).ifPresent(e -> f.setEdicionNombre(e.getNombre()));
        return feedbackRepository.save(f);
    }

    @GetMapping("/estadisticas")
    public Map<String, Object> estadisticas() {
        List<Nominacion> todas = nominacionRepository.findAll();
        Map<String, String> catNombres = new HashMap<>();
        for (Categoria c : categoriaRepository.findAll()) catNombres.put(c.getId(), c.getNombre());
        Map<String, Long> porCategoria = todas.stream()
                .collect(Collectors.groupingBy(n -> catNombres.getOrDefault(n.getCategoriaId(), "N/A"), Collectors.counting()));
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("porCategoria", porCategoria);
        resp.put("totalNominaciones", todas.size());
        resp.put("totalJugadores", jugadorRepository.count());
        return resp;
    }
}
