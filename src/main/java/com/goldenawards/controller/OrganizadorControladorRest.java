package com.goldenawards.controller;

import com.goldenawards.model.Categoria;
import com.goldenawards.model.Edicion;
import com.goldenawards.model.Nominacion;
import com.goldenawards.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/organizador")
public class OrganizadorControladorRest {

    private static final List<String> ESTADOS = List.of("BORRADOR", "NOMINACION", "VOTACION", "CERRADA");

    private final EdicionRepository edicionRepository;
    private final CategoriaRepository categoriaRepository;
    private final NominacionRepository nominacionRepository;
    private final VotoRepository votoRepository;
    private final PuntajeRepository puntajeRepository;

    public OrganizadorControladorRest(EdicionRepository edicionRepository, CategoriaRepository categoriaRepository,
                                      NominacionRepository nominacionRepository, VotoRepository votoRepository,
                                      PuntajeRepository puntajeRepository) {
        this.edicionRepository = edicionRepository;
        this.categoriaRepository = categoriaRepository;
        this.nominacionRepository = nominacionRepository;
        this.votoRepository = votoRepository;
        this.puntajeRepository = puntajeRepository;
    }

    private ResponseEntity<Object> error(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("error", mensaje));
    }

    // ===================== EDICIONES =====================

    @GetMapping("/ediciones")
    public List<Edicion> listarEdiciones() {
        return edicionRepository.findAll();
    }

    @GetMapping("/ediciones/{id}")
    public ResponseEntity<Object> obtenerEdicion(@PathVariable String id) {
        Optional<Edicion> e = edicionRepository.findById(id);
        return e.<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> error(HttpStatus.NOT_FOUND, "Edicion no encontrada"));
    }

    @PostMapping("/ediciones")
    public ResponseEntity<Object> crearEdicion(@RequestBody Edicion datos) {
        if (datos.getNombre() == null || datos.getNombre().isBlank() || datos.getAnio() < 1900 || datos.getAnio() > 2200) {
            return error(HttpStatus.BAD_REQUEST, "Nombre y ano valido son obligatorios");
        }
        datos.setId(null);
        datos.setEstado("BORRADOR");
        datos.setResultadosPublicados(false);
        return ResponseEntity.status(HttpStatus.CREATED).body(edicionRepository.save(datos));
    }

    @PutMapping("/ediciones/{id}")
    public ResponseEntity<Object> actualizarEdicion(@PathVariable String id, @RequestBody Edicion datos) {
        Edicion e = edicionRepository.findById(id).orElse(null);
        if (e == null) {
            return error(HttpStatus.NOT_FOUND, "Edicion no encontrada");
        }
        if (datos.getNombre() == null || datos.getNombre().isBlank() || datos.getAnio() < 1900 || datos.getAnio() > 2200) {
            return error(HttpStatus.BAD_REQUEST, "Nombre y ano valido son obligatorios");
        }
        e.setNombre(datos.getNombre());
        e.setAnio(datos.getAnio());
        return ResponseEntity.ok(edicionRepository.save(e));
    }

    // Ejemplo: PUT /api/organizador/ediciones/{id}/estado?estado=VOTACION
    @PutMapping("/ediciones/{id}/estado")
    public ResponseEntity<Object> cambiarEstado(@PathVariable String id, @RequestParam String estado) {
        Edicion e = edicionRepository.findById(id).orElse(null);
        if (e == null) {
            return error(HttpStatus.NOT_FOUND, "Edicion no encontrada");
        }
        if (!ESTADOS.contains(estado)) {
            return error(HttpStatus.BAD_REQUEST, "Estado no valido. Usa: " + ESTADOS);
        }
        e.setEstado(estado);
        if (!"CERRADA".equals(estado)) {
            e.setResultadosPublicados(false);
        }
        return ResponseEntity.ok(edicionRepository.save(e));
    }

    @PutMapping("/ediciones/{id}/publicar")
    public ResponseEntity<Object> publicar(@PathVariable String id) {
        Edicion e = edicionRepository.findById(id).orElse(null);
        if (e == null) {
            return error(HttpStatus.NOT_FOUND, "Edicion no encontrada");
        }
        if (!"CERRADA".equals(e.getEstado())) {
            return error(HttpStatus.BAD_REQUEST, "Solo se pueden publicar resultados de una edicion CERRADA");
        }
        e.setResultadosPublicados(!e.isResultadosPublicados());
        return ResponseEntity.ok(edicionRepository.save(e));
    }

    @DeleteMapping("/ediciones/{id}")
    public ResponseEntity<Object> eliminarEdicion(@PathVariable String id) {
        if (!edicionRepository.existsById(id)) {
            return error(HttpStatus.NOT_FOUND, "Edicion no encontrada");
        }
        votoRepository.deleteByEdicionId(id);
        puntajeRepository.deleteByEdicionId(id);
        nominacionRepository.deleteByEdicionId(id);
        categoriaRepository.deleteByEdicionId(id);
        edicionRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("mensaje", "Edicion eliminada junto con sus categorias, nominaciones, votos y calificaciones"));
    }

    // ===================== CATEGORIAS =====================

    @GetMapping("/ediciones/{id}/categorias")
    public ResponseEntity<Object> categoriasDeEdicion(@PathVariable String id) {
        if (!edicionRepository.existsById(id)) {
            return error(HttpStatus.NOT_FOUND, "Edicion no encontrada");
        }
        return ResponseEntity.ok(categoriaRepository.findByEdicionId(id));
    }

    @PostMapping("/categorias")
    public ResponseEntity<Object> crearCategoria(@RequestBody Categoria datos) {
        if (datos.getNombre() == null || datos.getNombre().isBlank() || datos.getEdicionId() == null) {
            return error(HttpStatus.BAD_REQUEST, "El nombre y la edicionId son obligatorios");
        }
        Edicion e = edicionRepository.findById(datos.getEdicionId()).orElse(null);
        if (e == null) {
            return error(HttpStatus.NOT_FOUND, "Edicion no encontrada");
        }
        if (!"BORRADOR".equals(e.getEstado()) && !"NOMINACION".equals(e.getEstado())) {
            return error(HttpStatus.BAD_REQUEST, "Solo se pueden crear categorias en BORRADOR o NOMINACION");
        }
        datos.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaRepository.save(datos));
    }

    @PutMapping("/categorias/{id}")
    public ResponseEntity<Object> actualizarCategoria(@PathVariable String id, @RequestBody Categoria datos) {
        Categoria c = categoriaRepository.findById(id).orElse(null);
        if (c == null) {
            return error(HttpStatus.NOT_FOUND, "Categoria no encontrada");
        }
        if (datos.getNombre() == null || datos.getNombre().isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
        c.setNombre(datos.getNombre());
        c.setDescripcion(datos.getDescripcion());
        return ResponseEntity.ok(categoriaRepository.save(c));
    }

    @DeleteMapping("/categorias/{id}")
    public ResponseEntity<Object> eliminarCategoria(@PathVariable String id) {
        Categoria c = categoriaRepository.findById(id).orElse(null);
        if (c == null) {
            return error(HttpStatus.NOT_FOUND, "Categoria no encontrada");
        }
        Edicion e = edicionRepository.findById(c.getEdicionId()).orElse(null);
        if (e != null && !"BORRADOR".equals(e.getEstado()) && !"NOMINACION".equals(e.getEstado())) {
            return error(HttpStatus.BAD_REQUEST, "Solo se pueden eliminar categorias en BORRADOR o NOMINACION");
        }
        votoRepository.deleteByCategoriaId(id);
        puntajeRepository.deleteByCategoriaId(id);
        nominacionRepository.deleteByCategoriaId(id);
        categoriaRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("mensaje", "Categoria eliminada"));
    }

    // ===================== NOMINACIONES =====================

    // Ejemplo: GET /api/organizador/nominaciones?edicionId=...
    @GetMapping("/nominaciones")
    public List<Nominacion> listarNominaciones(@RequestParam(required = false) String edicionId) {
        return edicionId == null ? nominacionRepository.findAll() : nominacionRepository.findByEdicionId(edicionId);
    }

    // Ejemplo: PUT /api/organizador/nominaciones/{id}/estado?estado=APROBADA
    @PutMapping("/nominaciones/{id}/estado")
    public ResponseEntity<Object> estadoNominacion(@PathVariable String id, @RequestParam String estado) {
        Nominacion n = nominacionRepository.findById(id).orElse(null);
        if (n == null) {
            return error(HttpStatus.NOT_FOUND, "Nominacion no encontrada");
        }
        if (!"APROBADA".equals(estado) && !"RECHAZADA".equals(estado)) {
            return error(HttpStatus.BAD_REQUEST, "Estado no valido. Usa APROBADA o RECHAZADA");
        }
        Edicion e = edicionRepository.findById(n.getEdicionId()).orElse(null);
        if (e == null || (!"NOMINACION".equals(e.getEstado()) && !"VOTACION".equals(e.getEstado()))) {
            return error(HttpStatus.BAD_REQUEST, "Solo se puede aprobar o rechazar mientras la edicion esta en NOMINACION o VOTACION");
        }
        if ("RECHAZADA".equals(estado)
                && (votoRepository.existsByNominacionId(id) || puntajeRepository.existsByNominacionId(id))) {
            return error(HttpStatus.CONFLICT, "La nominacion ya recibio votos o calificaciones del jurado y no puede rechazarse");
        }
        n.setEstado(estado);
        return ResponseEntity.ok(nominacionRepository.save(n));
    }
}
