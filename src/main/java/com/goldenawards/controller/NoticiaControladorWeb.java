package com.goldenawards.controller;

import com.goldenawards.model.Edicion;
import com.goldenawards.model.Noticia;
import com.goldenawards.repository.EdicionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Pagina PUBLICA de noticias (no requiere iniciar sesion). REQ-PER-002.
 * Reutiliza NoticiaControladorRest para decidir que noticias son visibles.
 */
@Controller
@RequestMapping("/publico/noticias")
public class NoticiaControladorWeb {

    private final NoticiaControladorRest noticiaRest;
    private final EdicionRepository edicionRepository;

    public NoticiaControladorWeb(NoticiaControladorRest noticiaRest, EdicionRepository edicionRepository) {
        this.noticiaRest = noticiaRest;
        this.edicionRepository = edicionRepository;
    }

    @GetMapping
    public String lista(@RequestParam(required = false) String edicionId, Model model) {
        List<Edicion> ediciones = edicionRepository.findAll().stream()
                .sorted(Comparator.comparingInt(Edicion::getAnio).reversed())
                .collect(Collectors.toList());
        model.addAttribute("noticias", noticiaRest.publicadas(edicionId).stream()
                .map(n -> noticiaRest.ficha(n, false)).collect(Collectors.toList()));
        model.addAttribute("ediciones", ediciones);
        model.addAttribute("edicionId", edicionId == null ? "" : edicionId);
        return "publico/noticias";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable String id, Model model) {
        Noticia n = noticiaRest.visible(id);
        if (n == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Noticia no encontrada");
        }
        model.addAttribute("noticia", noticiaRest.ficha(n, true));
        return "publico/noticia";
    }
}
