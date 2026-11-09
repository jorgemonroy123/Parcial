package com.goldenawards.controller;

import com.goldenawards.config.Auditoria;
import com.goldenawards.model.*;
import com.goldenawards.repository.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

// Rol JURADO (API): califica nominados de 1 a 10 durante el periodo de votacion
@RestController
@RequestMapping("/api/jurado")
public class JuradoControladorRest {

    private final UsuarioRepository usuarioRepository;
    private final EdicionRepository edicionRepository;
    private final CategoriaRepository categoriaRepository;
    private final NominacionRepository nominacionRepository;
    private final JugadorRepository jugadorRepository;
    private final PuntajeRepository puntajeRepository;
    private final RegistroAuditoriaRepository auditoriaRepository;

    public JuradoControladorRest(UsuarioRepository usuarioRepository, EdicionRepository edicionRepository,
                                 CategoriaRepository categoriaRepository, NominacionRepository nominacionRepository,
                                 JugadorRepository jugadorRepository, PuntajeRepository puntajeRepository,
                                 RegistroAuditoriaRepository auditoriaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.edicionRepository = edicionRepository;
        this.categoriaRepository = categoriaRepository;
        this.nominacionRepository = nominacionRepository;
        this.jugadorRepository = jugadorRepository;
        this.puntajeRepository = puntajeRepository;
        this.auditoriaRepository = auditoriaRepository;
    }

    private ResponseEntity<Object> error(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("error", mensaje));
    }

    private Usuario actual(Principal principal) {
        return usuarioRepository.findByUsername(principal.getName()).orElseThrow();
    }

    // Ediciones que estan en votacion (las unicas que el jurado puede calificar)
    @GetMapping("/ediciones")
    public List<Edicion> edicionesPorEvaluar() {
        return edicionRepository.findByEstadoIn(List.of("VOTACION"));
    }

    // Nominados aprobados de una edicion por categoria, con la calificacion que ya dio este jurado
    @GetMapping("/ediciones/{id}/nominados")
    public ResponseEntity<Object> nominados(@PathVariable String id, Principal principal) {
        if (!edicionRepository.existsById(id)) {
            return error(HttpStatus.NOT_FOUND, "Edicion no encontrada");
        }
        Usuario u = actual(principal);
        List<Map<String, Object>> respuesta = new ArrayList<>();
        for (Categoria c : categoriaRepository.findByEdicionId(id)) {
            List<Map<String, Object>> nominados = new ArrayList<>();
            for (Nominacion n : nominacionRepository.findByCategoriaIdAndEstado(c.getId(), "APROBADA")) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("nominacionId", n.getId());
                item.put("jugador", jugadorRepository.findById(n.getJugadorId()).orElse(null));
                item.put("miPuntaje", puntajeRepository.findByJuradoIdAndNominacionId(u.getId(), n.getId()).orElse(null));
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

    // Body: {"nominacionId": "...", "puntaje": 8, "comentario": "opcional"}
    // Si ya habias calificado a ese nominado, la calificacion se actualiza.
    @PostMapping("/puntajes")
    public ResponseEntity<Object> puntuar(@RequestBody Puntaje datos, Principal principal) {
        if (datos.getNominacionId() == null) {
            return error(HttpStatus.BAD_REQUEST, "nominacionId es obligatorio");
        }
        if (datos.getPuntaje() < 1 || datos.getPuntaje() > 10) {
            return error(HttpStatus.BAD_REQUEST, "El puntaje debe estar entre 1 y 10");
        }
        String texto = datos.getComentario() == null ? "" : datos.getComentario().trim();
        if (texto.length() > 300) {
            return error(HttpStatus.BAD_REQUEST, "El comentario no puede superar 300 caracteres");
        }
        Nominacion n = nominacionRepository.findById(datos.getNominacionId()).orElse(null);
        if (n == null || !"APROBADA".equals(n.getEstado())) {
            return error(HttpStatus.BAD_REQUEST, "Solo se puede calificar a nominados aprobados");
        }
        Edicion e = edicionRepository.findById(n.getEdicionId()).orElse(null);
        if (e == null || !"VOTACION".equals(e.getEstado())) {
            return error(HttpStatus.BAD_REQUEST, "La edicion no esta en periodo de votacion");
        }
        Usuario u = actual(principal);
        Puntaje p = puntajeRepository.findByJuradoIdAndNominacionId(u.getId(), n.getId()).orElse(null);
        boolean esNuevo = p == null;
        if (esNuevo) {
            p = new Puntaje();
            p.setEdicionId(n.getEdicionId());
            p.setCategoriaId(n.getCategoriaId());
            p.setNominacionId(n.getId());
            p.setJuradoId(u.getId());
        }
        p.setPuntaje(datos.getPuntaje());
        p.setComentario(texto);
        p.setFecha(LocalDateTime.now());
        Puntaje guardado;
        try {
            guardado = puntajeRepository.save(p);
        } catch (DuplicateKeyException ex) {
            return error(HttpStatus.CONFLICT, "Ya calificaste a este nominado; vuelve a intentarlo");
        }
        if (esNuevo) {
            // Auditoria: quien califico, cuando y en que categoria (sin nominado ni nota)
            Auditoria.registrar(auditoriaRepository, Auditoria.CALIFICACION, u, n.getEdicionId(), n.getCategoriaId(), Auditoria.CANAL_API);
        }
        return ResponseEntity.status(esNuevo ? HttpStatus.CREATED : HttpStatus.OK).body(guardado);
    }

    // Todas las calificaciones del jurado autenticado
    @GetMapping("/puntajes")
    public List<Puntaje> misPuntajes(Principal principal) {
        return puntajeRepository.findByJuradoId(actual(principal).getId());
    }
}
