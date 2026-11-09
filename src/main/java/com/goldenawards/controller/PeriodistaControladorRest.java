package com.goldenawards.controller;

import com.goldenawards.model.Noticia;
import com.goldenawards.model.Usuario;
import com.goldenawards.repository.EdicionRepository;
import com.goldenawards.repository.NoticiaRepository;
import com.goldenawards.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Rol PERIODISTA (API): crea, edita y publica SUS noticias.
 * Trazabilidad: REQ-PER-001 (publicar), REQ-PER-002 (el borrador no se ve en la pagina publica),
 * REQ-PER-003 (autor, fecha y edicion). El periodista no puede borrar noticias ni tocar las de otros.
 * El controlador web reutiliza los metodos publicos de esta clase.
 */
@RestController
@RequestMapping("/api/periodista")
public class PeriodistaControladorRest {

    public static final int MAX_TITULO = 120;
    public static final int MAX_RESUMEN = 280;
    public static final int MAX_CONTENIDO = 5000;

    private final UsuarioRepository usuarioRepository;
    private final NoticiaRepository noticiaRepository;
    private final EdicionRepository edicionRepository;

    public PeriodistaControladorRest(UsuarioRepository usuarioRepository, NoticiaRepository noticiaRepository,
                                     EdicionRepository edicionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.noticiaRepository = noticiaRepository;
        this.edicionRepository = edicionRepository;
    }

    private ResponseEntity<Object> error(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("error", mensaje));
    }

    private Usuario actual(Principal principal) {
        return usuarioRepository.findByUsername(principal.getName()).orElseThrow();
    }

    // ---------- logica reutilizada por el controlador web ----------

    // Devuelve el mensaje de error o null si los campos son validos
    public String validarCampos(String titulo, String resumen, String contenido, String edicionId) {
        if (titulo == null || titulo.isBlank()) {
            return "El titulo es obligatorio";
        }
        if (titulo.trim().length() > MAX_TITULO) {
            return "El titulo no puede superar " + MAX_TITULO + " caracteres";
        }
        if (contenido == null || contenido.isBlank()) {
            return "El contenido es obligatorio";
        }
        if (contenido.trim().length() > MAX_CONTENIDO) {
            return "El contenido no puede superar " + MAX_CONTENIDO + " caracteres";
        }
        if (resumen != null && resumen.trim().length() > MAX_RESUMEN) {
            return "El resumen no puede superar " + MAX_RESUMEN + " caracteres";
        }
        if (edicionId != null && !edicionId.isBlank() && !edicionRepository.existsById(edicionId)) {
            return "La edicion indicada no existe";
        }
        return null;
    }

    public boolean esAutor(Noticia noticia, Usuario usuario) {
        return noticia.getAutorId() != null && noticia.getAutorId().equals(usuario.getId());
    }

    // Crea una noticia en BORRADOR a nombre del periodista (REQ-PER-003: autor, fecha y edicion)
    public Noticia crearBorrador(Usuario autor, String titulo, String resumen, String contenido, String edicionId) {
        Noticia n = new Noticia();
        n.setAutorId(autor.getId());
        n.setAutorNombre(autor.getNombre());
        n.setEstado("BORRADOR");
        n.setFechaCreacion(LocalDateTime.now());
        aplicarCambios(n, titulo, resumen, contenido, edicionId);
        return noticiaRepository.save(n);
    }

    public Noticia editar(Noticia n, String titulo, String resumen, String contenido, String edicionId) {
        aplicarCambios(n, titulo, resumen, contenido, edicionId);
        return noticiaRepository.save(n);
    }

    private void aplicarCambios(Noticia n, String titulo, String resumen, String contenido, String edicionId) {
        n.setTitulo(titulo.trim());
        n.setResumen(resumen == null || resumen.isBlank() ? null : resumen.trim());
        n.setContenido(contenido.trim());
        n.setEdicionId(edicionId == null || edicionId.isBlank() ? null : edicionId);
    }

    // Pasa BORRADOR -> PUBLICADA y guarda la fecha de publicacion
    public Noticia publicar(Noticia n) {
        n.setEstado("PUBLICADA");
        n.setFechaPublicacion(LocalDateTime.now());
        return noticiaRepository.save(n);
    }

    public List<Noticia> delAutor(Usuario autor) {
        return noticiaRepository.findByAutorId(autor.getId()).stream()
                .sorted(Comparator.comparing(Noticia::getFechaCreacion,
                        Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder())).reversed())
                .collect(Collectors.toList());
    }

    // ---------- endpoints ----------

    // Mis noticias (borradores y publicadas)
    @GetMapping("/noticias")
    public List<Noticia> misNoticias(Principal principal) {
        return delAutor(actual(principal));
    }

    // Body: {"titulo": "...", "resumen": "opcional", "contenido": "...", "edicionId": "opcional"}
    @PostMapping("/noticias")
    public ResponseEntity<Object> crear(@RequestBody Noticia datos, Principal principal) {
        String problema = validarCampos(datos.getTitulo(), datos.getResumen(), datos.getContenido(), datos.getEdicionId());
        if (problema != null) {
            return error(HttpStatus.BAD_REQUEST, problema);
        }
        Noticia creada = crearBorrador(actual(principal), datos.getTitulo(), datos.getResumen(),
                datos.getContenido(), datos.getEdicionId());
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PutMapping("/noticias/{id}")
    public ResponseEntity<Object> actualizar(@PathVariable String id, @RequestBody Noticia datos, Principal principal) {
        Noticia n = noticiaRepository.findById(id).orElse(null);
        if (n == null) {
            return error(HttpStatus.NOT_FOUND, "Noticia no encontrada");
        }
        if (!esAutor(n, actual(principal))) {
            return error(HttpStatus.FORBIDDEN, "Solo el autor puede editar su noticia");
        }
        String problema = validarCampos(datos.getTitulo(), datos.getResumen(), datos.getContenido(), datos.getEdicionId());
        if (problema != null) {
            return error(HttpStatus.BAD_REQUEST, problema);
        }
        return ResponseEntity.ok(editar(n, datos.getTitulo(), datos.getResumen(), datos.getContenido(), datos.getEdicionId()));
    }

    @PutMapping("/noticias/{id}/publicar")
    public ResponseEntity<Object> publicarNoticia(@PathVariable String id, Principal principal) {
        Noticia n = noticiaRepository.findById(id).orElse(null);
        if (n == null) {
            return error(HttpStatus.NOT_FOUND, "Noticia no encontrada");
        }
        if (!esAutor(n, actual(principal))) {
            return error(HttpStatus.FORBIDDEN, "Solo el autor puede publicar su noticia");
        }
        if ("PUBLICADA".equals(n.getEstado())) {
            return error(HttpStatus.CONFLICT, "La noticia ya esta publicada");
        }
        return ResponseEntity.ok(publicar(n));
    }
}
