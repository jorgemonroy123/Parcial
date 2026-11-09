package com.goldenawards.controller;

import com.goldenawards.model.Categoria;
import com.goldenawards.model.Edicion;
import com.goldenawards.model.RegistroAuditoria;
import com.goldenawards.repository.CategoriaRepository;
import com.goldenawards.repository.EdicionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Rol AUDITOR (web, solo lectura): registro de actividad, anomalias y exportacion.
 * La logica de filtros, anomalias y CSV esta en AuditorControladorRest y se reutiliza aqui.
 */
@Controller
@RequestMapping("/auditor")
public class AuditorControladorWeb {

    private final AuditorControladorRest auditorRest;
    private final EdicionRepository edicionRepository;
    private final CategoriaRepository categoriaRepository;

    public AuditorControladorWeb(AuditorControladorRest auditorRest, EdicionRepository edicionRepository,
                                 CategoriaRepository categoriaRepository) {
        this.auditorRest = auditorRest;
        this.edicionRepository = edicionRepository;
        this.categoriaRepository = categoriaRepository;
    }

    private List<Edicion> edicionesOrdenadas() {
        return edicionRepository.findAll().stream()
                .sorted(Comparator.comparingInt(Edicion::getAnio).reversed())
                .collect(Collectors.toList());
    }

    @GetMapping({"", "/"})
    public String inicio() {
        return "redirect:/auditor/registros";
    }

    @GetMapping("/registros")
    public String registros(@RequestParam(required = false) String edicionId,
                            @RequestParam(required = false) String categoriaId,
                            @RequestParam(required = false) String tipo,
                            @RequestParam(required = false) Integer limite,
                            Model model) {
        List<RegistroAuditoria> todos = auditorRest.filtrar(edicionId, categoriaId, tipo);
        int tope = auditorRest.normalizarLimite(limite);
        List<RegistroAuditoria> visibles = todos.stream().limit(tope).collect(Collectors.toList());

        // Si la categoria elegida no pertenece a la edicion elegida, se ignora en el selector
        List<Categoria> categorias = categoriaRepository.findAll().stream()
                .filter(c -> edicionId == null || edicionId.isBlank() || edicionId.equals(c.getEdicionId()))
                .collect(Collectors.toList());

        model.addAttribute("filas", auditorRest.filas(visibles));
        model.addAttribute("total", todos.size());
        model.addAttribute("mostrados", visibles.size());
        model.addAttribute("limite", tope);
        model.addAttribute("resumen", auditorRest.resumen(todos));
        model.addAttribute("ediciones", edicionesOrdenadas());
        model.addAttribute("categorias", categorias);
        model.addAttribute("edicionId", edicionId == null ? "" : edicionId);
        model.addAttribute("categoriaId", categoriaId == null ? "" : categoriaId);
        model.addAttribute("tipo", tipo == null ? "" : tipo);
        return "auditor/registros";
    }

    @GetMapping("/anomalias")
    public String anomalias(@RequestParam(required = false) String edicionId, Model model) {
        model.addAttribute("anomalias", auditorRest.detectarAnomalias(edicionId));
        model.addAttribute("ediciones", edicionesOrdenadas());
        model.addAttribute("edicionId", edicionId == null ? "" : edicionId);
        return "auditor/anomalias";
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(@RequestParam(required = false) String edicionId,
                                           @RequestParam(required = false) String categoriaId,
                                           @RequestParam(required = false) String tipo) {
        return auditorRest.exportar(edicionId, categoriaId, tipo);
    }
}
