package com.goldenawards.controller;

import com.goldenawards.model.*;
import com.goldenawards.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/organizador")
public class OrganizadorControladorWeb {

    private static final List<String> ESTADOS = List.of("BORRADOR", "NOMINACION", "VOTACION", "CERRADA");

    private final EdicionRepository edicionRepository;
    private final CategoriaRepository categoriaRepository;
    private final NominacionRepository nominacionRepository;
    private final VotoRepository votoRepository;
    private final JugadorRepository jugadorRepository;
    private final UsuarioRepository usuarioRepository;

    public OrganizadorControladorWeb(EdicionRepository edicionRepository, CategoriaRepository categoriaRepository,
                                     NominacionRepository nominacionRepository, VotoRepository votoRepository,
                                     JugadorRepository jugadorRepository, UsuarioRepository usuarioRepository) {
        this.edicionRepository = edicionRepository;
        this.categoriaRepository = categoriaRepository;
        this.nominacionRepository = nominacionRepository;
        this.votoRepository = votoRepository;
        this.jugadorRepository = jugadorRepository;
        this.usuarioRepository = usuarioRepository;
    }

    private String resultado(RedirectAttributes ra, boolean exito, String mensaje, String destino) {
        ra.addFlashAttribute("mensaje", mensaje);
        ra.addFlashAttribute("tipo", exito ? "success" : "danger");
        return "redirect:" + destino;
    }

    private List<Edicion> edicionesOrdenadas() {
        return edicionRepository.findAll().stream()
                .sorted(Comparator.comparingInt(Edicion::getAnio).reversed())
                .collect(Collectors.toList());
    }

    // ===================== EDICIONES =====================

    @GetMapping("/ediciones")
    public String ediciones(Model model) {
        Map<String, Long> pendientes = nominacionRepository.findAll().stream()
                .filter(n -> "PENDIENTE".equals(n.getEstado()))
                .collect(Collectors.groupingBy(Nominacion::getEdicionId, Collectors.counting()));
        model.addAttribute("ediciones", edicionesOrdenadas());
        model.addAttribute("pendientes", pendientes);
        return "organizador/ediciones";
    }

    @PostMapping("/ediciones/guardar")
    public String guardarEdicion(@RequestParam(required = false) String id,
                                 @RequestParam String nombre,
                                 @RequestParam int anio,
                                 RedirectAttributes ra) {
        String destino = "/organizador/ediciones";
        if (anio < 1900 || anio > 2200) {
            return resultado(ra, false, "El ano no es valido", destino);
        }
        boolean esNuevo = id == null || id.isBlank();
        Edicion e;
        if (esNuevo) {
            e = new Edicion();
            e.setEstado("BORRADOR");
            e.setResultadosPublicados(false);
        } else {
            e = edicionRepository.findById(id).orElse(null);
            if (e == null) {
                return resultado(ra, false, "Edicion no encontrada", destino);
            }
        }
        e.setNombre(nombre.trim());
        e.setAnio(anio);
        edicionRepository.save(e);
        return resultado(ra, true, esNuevo ? "Edicion creada en estado BORRADOR" : "Edicion actualizada", destino);
    }

    @PostMapping("/ediciones/{id}/estado")
    public String cambiarEstado(@PathVariable String id, @RequestParam String estado, RedirectAttributes ra) {
        String destino = "/organizador/ediciones";
        Edicion e = edicionRepository.findById(id).orElse(null);
        if (e == null) {
            return resultado(ra, false, "Edicion no encontrada", destino);
        }
        if (!ESTADOS.contains(estado)) {
            return resultado(ra, false, "Estado no valido", destino);
        }
        e.setEstado(estado);
        if (!"CERRADA".equals(estado)) {
            e.setResultadosPublicados(false);
        }
        edicionRepository.save(e);
        return resultado(ra, true, "La edicion ahora esta en estado " + estado, destino);
    }

    @PostMapping("/ediciones/{id}/publicar")
    public String publicar(@PathVariable String id, RedirectAttributes ra) {
        String destino = "/organizador/ediciones";
        Edicion e = edicionRepository.findById(id).orElse(null);
        if (e == null) {
            return resultado(ra, false, "Edicion no encontrada", destino);
        }
        if (!"CERRADA".equals(e.getEstado())) {
            return resultado(ra, false, "Solo se pueden publicar resultados de una edicion CERRADA", destino);
        }
        e.setResultadosPublicados(!e.isResultadosPublicados());
        edicionRepository.save(e);
        return resultado(ra, true, e.isResultadosPublicados() ? "Resultados publicados" : "Resultados ocultados", destino);
    }

    @PostMapping("/ediciones/{id}/eliminar")
    public String eliminarEdicion(@PathVariable String id, RedirectAttributes ra) {
        String destino = "/organizador/ediciones";
        if (!edicionRepository.existsById(id)) {
            return resultado(ra, false, "Edicion no encontrada", destino);
        }
        votoRepository.deleteByEdicionId(id);
        nominacionRepository.deleteByEdicionId(id);
        categoriaRepository.deleteByEdicionId(id);
        edicionRepository.deleteById(id);
        return resultado(ra, true, "Edicion eliminada junto con sus categorias, nominaciones y votos", destino);
    }

    // ===================== CATEGORIAS =====================

    @GetMapping("/ediciones/{id}/categorias")
    public String categorias(@PathVariable String id, Model model, RedirectAttributes ra) {
        Edicion e = edicionRepository.findById(id).orElse(null);
        if (e == null) {
            return resultado(ra, false, "Edicion no encontrada", "/organizador/ediciones");
        }
        model.addAttribute("edicion", e);
        model.addAttribute("categorias", categoriaRepository.findByEdicionId(id));
        return "organizador/categorias";
    }

    @PostMapping("/categorias/guardar")
    public String guardarCategoria(@RequestParam(required = false) String id,
                                   @RequestParam String edicionId,
                                   @RequestParam String nombre,
                                   @RequestParam(required = false) String descripcion,
                                   RedirectAttributes ra) {
        Edicion e = edicionRepository.findById(edicionId).orElse(null);
        if (e == null) {
            return resultado(ra, false, "Edicion no encontrada", "/organizador/ediciones");
        }
        String destino = "/organizador/ediciones/" + edicionId + "/categorias";
        boolean esNuevo = id == null || id.isBlank();
        Categoria c;
        if (esNuevo) {
            if (!"BORRADOR".equals(e.getEstado()) && !"NOMINACION".equals(e.getEstado())) {
                return resultado(ra, false, "Solo se pueden crear categorias en BORRADOR o NOMINACION", destino);
            }
            c = new Categoria();
            c.setEdicionId(edicionId);
        } else {
            c = categoriaRepository.findById(id).orElse(null);
            if (c == null) {
                return resultado(ra, false, "Categoria no encontrada", destino);
            }
        }
        c.setNombre(nombre.trim());
        c.setDescripcion(descripcion);
        categoriaRepository.save(c);
        return resultado(ra, true, esNuevo ? "Categoria creada" : "Categoria actualizada", destino);
    }

    @PostMapping("/categorias/{id}/eliminar")
    public String eliminarCategoria(@PathVariable String id, RedirectAttributes ra) {
        Categoria c = categoriaRepository.findById(id).orElse(null);
        if (c == null) {
            return resultado(ra, false, "Categoria no encontrada", "/organizador/ediciones");
        }
        String destino = "/organizador/ediciones/" + c.getEdicionId() + "/categorias";
        Edicion e = edicionRepository.findById(c.getEdicionId()).orElse(null);
        if (e != null && !"BORRADOR".equals(e.getEstado()) && !"NOMINACION".equals(e.getEstado())) {
            return resultado(ra, false, "Solo se pueden eliminar categorias en BORRADOR o NOMINACION", destino);
        }
        votoRepository.deleteByCategoriaId(id);
        nominacionRepository.deleteByCategoriaId(id);
        categoriaRepository.deleteById(id);
        return resultado(ra, true, "Categoria eliminada", destino);
    }

    // ===================== NOMINACIONES (aprobar / rechazar) =====================

    @GetMapping("/nominaciones")
    public String nominaciones(@RequestParam(required = false) String edicionId, Model model) {
        List<Edicion> ediciones = edicionesOrdenadas();
        Edicion seleccionada = null;
        if (edicionId != null) {
            seleccionada = ediciones.stream().filter(e -> e.getId().equals(edicionId)).findFirst().orElse(null);
        }
        if (seleccionada == null && !ediciones.isEmpty()) {
            seleccionada = ediciones.get(0);
        }
        List<Nominacion> lista = List.of();
        if (seleccionada != null) {
            lista = nominacionRepository.findByEdicionId(seleccionada.getId()).stream()
                    .sorted(Comparator.comparingInt(n -> "PENDIENTE".equals(n.getEstado()) ? 0 : 1))
                    .collect(Collectors.toList());
        }
        model.addAttribute("ediciones", ediciones);
        model.addAttribute("edicionSel", seleccionada);
        model.addAttribute("nominaciones", lista);
        model.addAttribute("categoriasPorId", categoriaRepository.findAll().stream()
                .collect(Collectors.toMap(Categoria::getId, c -> c)));
        model.addAttribute("jugadoresPorId", jugadorRepository.findAll().stream()
                .collect(Collectors.toMap(Jugador::getId, j -> j)));
        model.addAttribute("usuariosPorId", usuarioRepository.findAll().stream()
                .collect(Collectors.toMap(Usuario::getId, u -> u)));
        return "organizador/nominaciones";
    }

    @PostMapping("/nominaciones/{id}/estado")
    public String estadoNominacion(@PathVariable String id, @RequestParam String estado, RedirectAttributes ra) {
        Nominacion n = nominacionRepository.findById(id).orElse(null);
        if (n == null) {
            return resultado(ra, false, "Nominacion no encontrada", "/organizador/nominaciones");
        }
        String destino = "/organizador/nominaciones?edicionId=" + n.getEdicionId();
        if (!"APROBADA".equals(estado) && !"RECHAZADA".equals(estado)) {
            return resultado(ra, false, "Estado no valido", destino);
        }
        Edicion e = edicionRepository.findById(n.getEdicionId()).orElse(null);
        if (e == null || (!"NOMINACION".equals(e.getEstado()) && !"VOTACION".equals(e.getEstado()))) {
            return resultado(ra, false, "Solo se puede aprobar o rechazar mientras la edicion esta en NOMINACION o VOTACION", destino);
        }
        if ("RECHAZADA".equals(estado) && votoRepository.existsByNominacionId(id)) {
            return resultado(ra, false, "La nominacion ya recibio votos y no puede rechazarse", destino);
        }
        n.setEstado(estado);
        nominacionRepository.save(n);
        return resultado(ra, true, "Nominacion " + estado.toLowerCase(), destino);
    }
}
