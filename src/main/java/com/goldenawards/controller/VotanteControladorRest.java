package com.goldenawards.controller;

import com.goldenawards.model.*;
import com.goldenawards.repository.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/votante")
public class VotanteControladorRest {

    private final UsuarioRepository usuarioRepository;
    private final EdicionRepository edicionRepository;
    private final CategoriaRepository categoriaRepository;
    private final NominacionRepository nominacionRepository;
    private final VotoRepository votoRepository;
    private final JugadorRepository jugadorRepository;

    public VotanteControladorRest(UsuarioRepository usuarioRepository, EdicionRepository edicionRepository,
                                  CategoriaRepository categoriaRepository, NominacionRepository nominacionRepository,
                                  VotoRepository votoRepository, JugadorRepository jugadorRepository) {
        this.usuarioRepository = usuarioRepository;
        this.edicionRepository = edicionRepository;
        this.categoriaRepository = categoriaRepository;
        this.nominacionRepository = nominacionRepository;
        this.votoRepository = votoRepository;
        this.jugadorRepository = jugadorRepository;
    }

    private ResponseEntity<Object> error(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("error", mensaje));
    }

    private Usuario actual(Principal principal) {
        return usuarioRepository.findByUsername(principal.getName()).orElseThrow();
    }

    // Ediciones con periodo de nominacion o votacion abierto
    @GetMapping("/ediciones")
    public List<Edicion> edicionesAbiertas() {
        return edicionRepository.findByEstadoIn(List.of("NOMINACION", "VOTACION"));
    }

    // Nominados aprobados de una edicion, agrupados por categoria
    @GetMapping("/ediciones/{id}/nominados")
    public ResponseEntity<Object> nominados(@PathVariable String id) {
        if (!edicionRepository.existsById(id)) {
            return error(HttpStatus.NOT_FOUND, "Edicion no encontrada");
        }
        List<Map<String, Object>> respuesta = new ArrayList<>();
        for (Categoria c : categoriaRepository.findByEdicionId(id)) {
            List<Map<String, Object>> nominados = new ArrayList<>();
            for (Nominacion n : nominacionRepository.findByCategoriaIdAndEstado(c.getId(), "APROBADA")) {
                Jugador j = jugadorRepository.findById(n.getJugadorId()).orElse(null);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("nominacionId", n.getId());
                item.put("jugador", j);
                nominados.add(item);
            }
            Map<String, Object> categoria = new LinkedHashMap<>();
            categoria.put("categoriaId", c.getId());
            categoria.put("categoria", c.getNombre());
            categoria.put("nominados", nominados);
            respuesta.add(categoria);
        }
        return ResponseEntity.ok(respuesta);
    }

    // Body: {"categoriaId": "...", "jugadorId": "..."}
    @PostMapping("/nominaciones")
    public ResponseEntity<Object> proponer(@RequestBody Nominacion datos, Principal principal) {
        if (datos.getCategoriaId() == null || datos.getJugadorId() == null) {
            return error(HttpStatus.BAD_REQUEST, "categoriaId y jugadorId son obligatorios");
        }
        Categoria c = categoriaRepository.findById(datos.getCategoriaId()).orElse(null);
        if (c == null) {
            return error(HttpStatus.NOT_FOUND, "Categoria no encontrada");
        }
        Edicion e = edicionRepository.findById(c.getEdicionId()).orElse(null);
        if (e == null || !"NOMINACION".equals(e.getEstado())) {
            return error(HttpStatus.BAD_REQUEST, "El periodo de nominacion de esta edicion no esta abierto");
        }
        if (!jugadorRepository.existsById(datos.getJugadorId())) {
            return error(HttpStatus.NOT_FOUND, "Jugador no encontrado");
        }
        if (nominacionRepository.existsByCategoriaIdAndJugadorId(datos.getCategoriaId(), datos.getJugadorId())) {
            return error(HttpStatus.CONFLICT, "Ese jugador ya fue propuesto en la categoria");
        }
        Nominacion n = new Nominacion();
        n.setEdicionId(c.getEdicionId());
        n.setCategoriaId(datos.getCategoriaId());
        n.setJugadorId(datos.getJugadorId());
        n.setPropuestoPorId(actual(principal).getId());
        n.setEstado("PENDIENTE");
        n.setFecha(LocalDateTime.now());
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(nominacionRepository.save(n));
        } catch (DuplicateKeyException ex) {
            return error(HttpStatus.CONFLICT, "Ese jugador ya fue propuesto en la categoria");
        }
    }

    // Body: {"nominacionId": "..."}
    @PostMapping("/votos")
    public ResponseEntity<Object> votar(@RequestBody Voto datos, Principal principal) {
        if (datos.getNominacionId() == null) {
            return error(HttpStatus.BAD_REQUEST, "nominacionId es obligatorio");
        }
        Nominacion n = nominacionRepository.findById(datos.getNominacionId()).orElse(null);
        if (n == null || !"APROBADA".equals(n.getEstado())) {
            return error(HttpStatus.BAD_REQUEST, "Solo se puede votar por nominados aprobados");
        }
        Edicion e = edicionRepository.findById(n.getEdicionId()).orElse(null);
        if (e == null || !"VOTACION".equals(e.getEstado())) {
            return error(HttpStatus.BAD_REQUEST, "El periodo de votacion de esta edicion no esta abierto");
        }
        Usuario u = actual(principal);
        if (votoRepository.existsByVotanteIdAndCategoriaId(u.getId(), n.getCategoriaId())) {
            return error(HttpStatus.CONFLICT, "Ya emitiste tu voto en esta categoria");
        }
        Voto v = new Voto();
        v.setEdicionId(n.getEdicionId());
        v.setCategoriaId(n.getCategoriaId());
        v.setNominacionId(n.getId());
        v.setVotanteId(u.getId());
        v.setFecha(LocalDateTime.now());
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(votoRepository.save(v));
        } catch (DuplicateKeyException ex) {
            return error(HttpStatus.CONFLICT, "Ya emitiste tu voto en esta categoria");
        }
    }

    // Historial de votos del votante autenticado
    @GetMapping("/votos")
    public List<Voto> misVotos(Principal principal) {
        return votoRepository.findByVotanteId(actual(principal).getId());
    }

    // Nominaciones propuestas por el votante autenticado
    @GetMapping("/nominaciones")
    public List<Nominacion> misPropuestas(Principal principal) {
        return nominacionRepository.findByPropuestoPorId(actual(principal).getId());
    }
}
