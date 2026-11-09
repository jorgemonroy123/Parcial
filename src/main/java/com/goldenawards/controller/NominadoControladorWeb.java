package com.goldenawards.controller;

import com.goldenawards.model.*;
import com.goldenawards.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/nominado")
public class NominadoControladorWeb {

    private final UsuarioRepository usuarioRepository;
    private final JugadorRepository jugadorRepository;
    private final EdicionRepository edicionRepository;
    private final CategoriaRepository categoriaRepository;
    private final NominacionRepository nominacionRepository;
    private final VotoRepository votoRepository;

    public NominadoControladorWeb(UsuarioRepository usuarioRepository, JugadorRepository jugadorRepository,
                                  EdicionRepository edicionRepository, CategoriaRepository categoriaRepository,
                                  NominacionRepository nominacionRepository, VotoRepository votoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.jugadorRepository = jugadorRepository;
        this.edicionRepository = edicionRepository;
        this.categoriaRepository = categoriaRepository;
        this.nominacionRepository = nominacionRepository;
        this.votoRepository = votoRepository;
    }

    @GetMapping("/perfil")
    public String perfil(Principal principal, Model model) {
        Usuario u = usuarioRepository.findByUsername(principal.getName()).orElseThrow();
        Jugador jugador = u.getJugadorId() == null ? null : jugadorRepository.findById(u.getJugadorId()).orElse(null);
        List<Nominacion> mias = jugador == null ? List.of() : nominacionRepository.findByJugadorId(jugador.getId());

        Map<String, Edicion> edicionesPorId = edicionRepository.findAll().stream()
                .collect(Collectors.toMap(Edicion::getId, e -> e));

        // Resultados oficiales: solo de ediciones con resultados publicados
        Map<String, Long> votosRecibidos = new HashMap<>();
        Set<String> ganadoras = new HashSet<>();
        for (Nominacion n : mias) {
            Edicion e = edicionesPorId.get(n.getEdicionId());
            if (e == null || !e.isResultadosPublicados() || !"APROBADA".equals(n.getEstado())) {
                continue;
            }
            Map<String, Long> conteo = votoRepository.findByEdicionId(e.getId()).stream()
                    .filter(v -> n.getCategoriaId().equals(v.getCategoriaId()))
                    .collect(Collectors.groupingBy(Voto::getNominacionId, Collectors.counting()));
            long propios = conteo.getOrDefault(n.getId(), 0L);
            long maximo = conteo.values().stream().mapToLong(Long::longValue).max().orElse(0L);
            votosRecibidos.put(n.getId(), propios);
            if (propios > 0 && propios == maximo) {
                ganadoras.add(n.getId());
            }
        }

        model.addAttribute("usuario", u);
        model.addAttribute("jugador", jugador);
        model.addAttribute("nominaciones", mias);
        model.addAttribute("edicionesPorId", edicionesPorId);
        model.addAttribute("categoriasPorId", categoriaRepository.findAll().stream()
                .collect(Collectors.toMap(Categoria::getId, c -> c)));
        model.addAttribute("votosRecibidos", votosRecibidos);
        model.addAttribute("ganadoras", ganadoras);
        return "nominado/perfil";
    }
}
