package com.goldenawards.controller;

import com.goldenawards.model.Edicion;
import com.goldenawards.model.Noticia;
import com.goldenawards.model.Usuario;
import com.goldenawards.repository.EdicionRepository;
import com.goldenawards.repository.NoticiaRepository;
import com.goldenawards.repository.UsuarioRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Rol PERIODISTA (web): administra sus noticias (crear, editar, publicar).
 * La validacion y las reglas estan en PeriodistaControladorRest y se reutilizan aqui.
 */
@Controller
@RequestMapping("/periodista")
public class PeriodistaControladorWeb {

    private final PeriodistaControladorRest periodistaRest;
    private final UsuarioRepository usuarioRepository;
    private final NoticiaRepository noticiaRepository;
    private final EdicionRepository edicionRepository;

    public PeriodistaControladorWeb(PeriodistaControladorRest periodistaRest, UsuarioRepository usuarioRepository,
                                    NoticiaRepository noticiaRepository, EdicionRepository edicionRepository) {
        this.periodistaRest = periodistaRest;
        this.usuarioRepository = usuarioRepository;
        this.noticiaRepository = noticiaRepository;
        this.edicionRepository = edicionRepository;
    }

    private String resultado(RedirectAttributes ra, boolean exito, String mensaje, String destino) {
        ra.addFlashAttribute("mensaje", mensaje);
        ra.addFlashAttribute("tipo", exito ? "success" : "danger");
        return "redirect:" + destino;
    }

    private Usuario actual(Principal principal) {
        return usuarioRepository.findByUsername(principal.getName()).orElseThrow();
    }

    @GetMapping({"", "/"})
    public String inicio() {
        return "redirect:/periodista/noticias";
    }

    @GetMapping("/noticias")
    public String noticias(Principal principal, Model model) {
        List<Edicion> ediciones = edicionRepository.findAll().stream()
                .sorted(Comparator.comparingInt(Edicion::getAnio).reversed())
                .collect(Collectors.toList());
        model.addAttribute("noticias", periodistaRest.delAutor(actual(principal)));
        model.addAttribute("ediciones", ediciones);
        model.addAttribute("edicionesPorId", ediciones.stream()
                .collect(Collectors.toMap(Edicion::getId, e -> e)));
        return "periodista/noticias";
    }

    // Crea una noticia nueva (id vacio) o edita una propia
    @PostMapping("/noticias/guardar")
    public String guardar(@RequestParam(required = false) String id,
                          @RequestParam String titulo,
                          @RequestParam(required = false) String resumen,
                          @RequestParam String contenido,
                          @RequestParam(required = false) String edicionId,
                          Principal principal, RedirectAttributes ra) {
        String destino = "/periodista/noticias";
        String problema = periodistaRest.validarCampos(titulo, resumen, contenido, edicionId);
        if (problema != null) {
            return resultado(ra, false, problema, destino);
        }
        Usuario u = actual(principal);
        if (id == null || id.isBlank()) {
            periodistaRest.crearBorrador(u, titulo, resumen, contenido, edicionId);
            return resultado(ra, true, "Noticia guardada como borrador", destino);
        }
        Noticia n = noticiaRepository.findById(id).orElse(null);
        if (n == null) {
            return resultado(ra, false, "Noticia no encontrada", destino);
        }
        if (!periodistaRest.esAutor(n, u)) {
            return resultado(ra, false, "Solo el autor puede editar su noticia", destino);
        }
        periodistaRest.editar(n, titulo, resumen, contenido, edicionId);
        return resultado(ra, true, "Noticia actualizada", destino);
    }

    @PostMapping("/noticias/{id}/publicar")
    public String publicar(@PathVariable String id, Principal principal, RedirectAttributes ra) {
        String destino = "/periodista/noticias";
        Noticia n = noticiaRepository.findById(id).orElse(null);
        if (n == null) {
            return resultado(ra, false, "Noticia no encontrada", destino);
        }
        if (!periodistaRest.esAutor(n, actual(principal))) {
            return resultado(ra, false, "Solo el autor puede publicar su noticia", destino);
        }
        if ("PUBLICADA".equals(n.getEstado())) {
            return resultado(ra, false, "La noticia ya esta publicada", destino);
        }
        periodistaRest.publicar(n);
        return resultado(ra, true, "Noticia publicada: ya es visible para todos", destino);
    }
}
