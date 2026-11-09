package com.goldenawards.controller;

import com.goldenawards.config.Auditoria;
import com.goldenawards.model.*;
import com.goldenawards.repository.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/votante")
public class VotanteControladorWeb {

    private final UsuarioRepository usuarioRepository;
    private final EdicionRepository edicionRepository;
    private final CategoriaRepository categoriaRepository;
    private final NominacionRepository nominacionRepository;
    private final VotoRepository votoRepository;
    private final JugadorRepository jugadorRepository;
    private final RegistroAuditoriaRepository auditoriaRepository;

    public VotanteControladorWeb(UsuarioRepository usuarioRepository, EdicionRepository edicionRepository,
                                 CategoriaRepository categoriaRepository, NominacionRepository nominacionRepository,
                                 VotoRepository votoRepository, JugadorRepository jugadorRepository,
                                 RegistroAuditoriaRepository auditoriaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.edicionRepository = edicionRepository;
        this.categoriaRepository = categoriaRepository;
        this.nominacionRepository = nominacionRepository;
        this.votoRepository = votoRepository;
        this.jugadorRepository = jugadorRepository;
        this.auditoriaRepository = auditoriaRepository;
    }

    private String resultado(RedirectAttributes ra, boolean exito, String mensaje, String destino) {
        ra.addFlashAttribute("mensaje", mensaje);
        ra.addFlashAttribute("tipo", exito ? "success" : "danger");
        return "redirect:" + destino;
    }

    private Usuario actual(Principal principal) {
        return usuarioRepository.findByUsername(principal.getName()).orElseThrow();
    }

    // ===================== VOTAR / PROPONER =====================

    @GetMapping("/votar")
    public String votar(Principal principal, Model model) {
        Usuario u = actual(principal);
        List<Edicion> abiertas = edicionRepository.findByEstadoIn(List.of("NOMINACION", "VOTACION")).stream()
                .sorted(Comparator.comparingInt(Edicion::getAnio).reversed())
                .collect(Collectors.toList());

        Map<String, List<Categoria>> categoriasPorEdicion = new HashMap<>();
        Map<String, List<Nominacion>> nominacionesPorCategoria = new HashMap<>();
        for (Edicion e : abiertas) {
            List<Categoria> categorias = categoriaRepository.findByEdicionId(e.getId());
            categoriasPorEdicion.put(e.getId(), categorias);
            for (Categoria c : categorias) {
                nominacionesPorCategoria.put(c.getId(),
                        nominacionRepository.findByCategoriaIdAndEstado(c.getId(), "APROBADA"));
            }
        }
        // categoriaId -> nominacionId por la que ya voto este usuario
        Map<String, String> misVotos = votoRepository.findByVotanteId(u.getId()).stream()
                .collect(Collectors.toMap(Voto::getCategoriaId, Voto::getNominacionId, (a, b) -> a));

        model.addAttribute("ediciones", abiertas);
        model.addAttribute("categoriasPorEdicion", categoriasPorEdicion);
        model.addAttribute("nominacionesPorCategoria", nominacionesPorCategoria);
        model.addAttribute("misVotos", misVotos);
        model.addAttribute("jugadores", jugadorRepository.findAll());
        model.addAttribute("jugadoresPorId", jugadorRepository.findAll().stream()
                .collect(Collectors.toMap(Jugador::getId, j -> j)));
        return "votante/votar";
    }

    @PostMapping("/votar")
    public String emitirVoto(@RequestParam String nominacionId, Principal principal, RedirectAttributes ra) {
        String destino = "/votante/votar";
        Nominacion n = nominacionRepository.findById(nominacionId).orElse(null);
        if (n == null || !"APROBADA".equals(n.getEstado())) {
            return resultado(ra, false, "Solo se puede votar por nominados aprobados", destino);
        }
        Edicion e = edicionRepository.findById(n.getEdicionId()).orElse(null);
        if (e == null || !"VOTACION".equals(e.getEstado())) {
            return resultado(ra, false, "El periodo de votacion de esta edicion no esta abierto", destino);
        }
        Usuario u = actual(principal);
        if (votoRepository.existsByVotanteIdAndCategoriaId(u.getId(), n.getCategoriaId())) {
            Auditoria.registrar(auditoriaRepository, Auditoria.INTENTO_DUPLICADO, u, n.getEdicionId(), n.getCategoriaId(), Auditoria.CANAL_WEB);
            return resultado(ra, false, "Ya emitiste tu voto en esta categoria", destino);
        }
        Voto v = new Voto();
        v.setEdicionId(n.getEdicionId());
        v.setCategoriaId(n.getCategoriaId());
        v.setNominacionId(n.getId());
        v.setVotanteId(u.getId());
        v.setFecha(LocalDateTime.now());
        try {
            votoRepository.save(v);
        } catch (DuplicateKeyException ex) {
            Auditoria.registrar(auditoriaRepository, Auditoria.INTENTO_DUPLICADO, u, n.getEdicionId(), n.getCategoriaId(), Auditoria.CANAL_WEB);
            return resultado(ra, false, "Ya emitiste tu voto en esta categoria", destino);
        }
        // Auditoria: quien voto, cuando y en que categoria (nunca por quien)
        Auditoria.registrar(auditoriaRepository, Auditoria.VOTO, u, n.getEdicionId(), n.getCategoriaId(), Auditoria.CANAL_WEB);
        return resultado(ra, true, "Tu voto fue registrado", destino);
    }

    @PostMapping("/proponer")
    public String proponer(@RequestParam String categoriaId, @RequestParam String jugadorId,
                           Principal principal, RedirectAttributes ra) {
        String destino = "/votante/votar";
        Categoria c = categoriaRepository.findById(categoriaId).orElse(null);
        if (c == null) {
            return resultado(ra, false, "Categoria no encontrada", destino);
        }
        Edicion e = edicionRepository.findById(c.getEdicionId()).orElse(null);
        if (e == null || !"NOMINACION".equals(e.getEstado())) {
            return resultado(ra, false, "El periodo de nominacion de esta edicion no esta abierto", destino);
        }
        if (!jugadorRepository.existsById(jugadorId)) {
            return resultado(ra, false, "Jugador no encontrado", destino);
        }
        if (nominacionRepository.existsByCategoriaIdAndJugadorId(categoriaId, jugadorId)) {
            return resultado(ra, false, "Ese jugador ya fue propuesto en la categoria", destino);
        }
        Nominacion n = new Nominacion();
        n.setEdicionId(c.getEdicionId());
        n.setCategoriaId(categoriaId);
        n.setJugadorId(jugadorId);
        n.setPropuestoPorId(actual(principal).getId());
        n.setEstado("PENDIENTE");
        n.setFecha(LocalDateTime.now());
        try {
            nominacionRepository.save(n);
        } catch (DuplicateKeyException ex) {
            return resultado(ra, false, "Ese jugador ya fue propuesto en la categoria", destino);
        }
        return resultado(ra, true, "Propuesta enviada; el organizador debe aprobarla", destino);
    }

    // ===================== HISTORIAL =====================

    @GetMapping("/historial")
    public String historial(Principal principal, Model model) {
        Usuario u = actual(principal);
        List<Voto> votos = votoRepository.findByVotanteId(u.getId()).stream()
                .sorted(Comparator.comparing(Voto::getFecha).reversed())
                .collect(Collectors.toList());
        List<Nominacion> propuestas = nominacionRepository.findByPropuestoPorId(u.getId());

        model.addAttribute("votos", votos);
        model.addAttribute("propuestas", propuestas);
        model.addAttribute("edicionesPorId", edicionRepository.findAll().stream()
                .collect(Collectors.toMap(Edicion::getId, e -> e)));
        model.addAttribute("categoriasPorId", categoriaRepository.findAll().stream()
                .collect(Collectors.toMap(Categoria::getId, c -> c)));
        model.addAttribute("jugadoresPorId", jugadorRepository.findAll().stream()
                .collect(Collectors.toMap(Jugador::getId, j -> j)));
        model.addAttribute("nominacionesPorId", nominacionRepository.findAll().stream()
                .collect(Collectors.toMap(Nominacion::getId, n -> n)));
        return "votante/historial";
    }
}
