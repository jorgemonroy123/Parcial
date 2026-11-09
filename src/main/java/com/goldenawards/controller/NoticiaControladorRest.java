package com.goldenawards.controller;

import com.goldenawards.model.Edicion;
import com.goldenawards.model.Noticia;
import com.goldenawards.repository.EdicionRepository;
import com.goldenawards.repository.NoticiaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Noticias PUBLICAS (sin iniciar sesion). REQ-PER-002: solo se exponen las noticias PUBLICADAS;
 * un borrador responde 404 aunque se conozca su id. El controlador web reutiliza estos metodos.
 */
@RestController
@RequestMapping("/api/publico/noticias")
public class NoticiaControladorRest {

    private final NoticiaRepository noticiaRepository;
    private final EdicionRepository edicionRepository;

    public NoticiaControladorRest(NoticiaRepository noticiaRepository, EdicionRepository edicionRepository) {
        this.noticiaRepository = noticiaRepository;
        this.edicionRepository = edicionRepository;
    }

    private String fecha(LocalDateTime f) {
        if (f == null) {
            return "";
        }
        String s = f.toString().replace('T', ' ');
        return s.length() > 16 ? s.substring(0, 16) : s;
    }

    // Noticias publicadas, las mas recientes primero; filtro opcional por edicion
    public List<Noticia> publicadas(String edicionId) {
        return noticiaRepository.findByEstado("PUBLICADA").stream()
                .filter(n -> edicionId == null || edicionId.isBlank() || edicionId.equals(n.getEdicionId()))
                .sorted(Comparator.comparing(Noticia::getFechaPublicacion,
                        Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder())).reversed())
                .collect(Collectors.toList());
    }

    // Una noticia solo es visible si esta PUBLICADA; si no, se devuelve null
    public Noticia visible(String id) {
        return noticiaRepository.findById(id)
                .filter(n -> "PUBLICADA".equals(n.getEstado()))
                .orElse(null);
    }

    public Map<String, Object> ficha(Noticia n, boolean conContenido) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", n.getId());
        m.put("titulo", n.getTitulo());
        m.put("resumen", n.getResumen());
        if (conContenido) {
            m.put("contenido", n.getContenido());
        }
        m.put("autor", n.getAutorNombre());
        m.put("edicion", n.getEdicionId() == null ? null
                : edicionRepository.findById(n.getEdicionId()).map(Edicion::getNombre).orElse(null));
        m.put("fechaPublicacion", fecha(n.getFechaPublicacion()));
        return m;
    }

    @GetMapping
    public List<Map<String, Object>> listar(@RequestParam(required = false) String edicionId) {
        return publicadas(edicionId).stream().map(n -> ficha(n, false)).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> detalle(@PathVariable String id) {
        Noticia n = visible(id);
        if (n == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Noticia no encontrada"));
        }
        return ResponseEntity.ok(ficha(n, true));
    }
}
