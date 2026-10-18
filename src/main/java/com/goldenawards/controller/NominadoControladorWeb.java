package com.goldenawards.controller;

import com.goldenawards.config.Ponderacion;
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
    private final PuntajeRepository puntajeRepository;
    private final ParametroRepository parametroRepository;

    public NominadoControladorWeb(UsuarioRepository usuarioRepository, JugadorRepository jugadorRepository,
                                  EdicionRepository edicionRepository, CategoriaRepository categoriaRepository,
                                  NominacionRepository nominacionRepository, VotoRepository votoRepository,
                                  PuntajeRepository puntajeRepository, ParametroRepository parametroRepository) {
        this.usuarioRepository = usuarioRepository;
        this.jugadorRepository = jugadorRepository;
        this.edicionRepository = edicionRepository;
        this.categoriaRepository = categoriaRepository;
        this.nominacionRepository = nominacionRepository;
        this.votoRepository = votoRepository;
        this.puntajeRepository = puntajeRepository;
        this.parametroRepository = parametroRepository;
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
            List<Voto> votosCategoria = votoRepository.findByEdicionId(e.getId()).stream()
                    .filter(v -> n.getCategoriaId().equals(v.getCategoriaId()))
                    .collect(Collectors.toList());
            Map<String, Long> conteo = votosCategoria.stream()
                    .collect(Collectors.groupingBy(Voto::getNominacionId, Collectors.counting()));
            long total = votosCategoria.size();

            // Mismo calculo del ranking oficial: votos del publico + calificacion del jurado
            List<Puntaje> puntajesCategoria = puntajeRepository.findByEdicionId(e.getId()).stream()
                    .filter(p -> n.getCategoriaId().equals(p.getCategoriaId()))
                    .collect(Collectors.toList());
            boolean hayJurado = !puntajesCategoria.isEmpty();
            Map<String, Double> promedios = puntajesCategoria.stream()
                    .collect(Collectors.groupingBy(Puntaje::getNominacionId, Collectors.averagingInt(Puntaje::getPuntaje)));
            int peso = Ponderacion.pesoJurado(parametroRepository);
            double mejor = 0;
            double mio = 0;
            for (Nominacion otra : nominacionRepository.findByCategoriaIdAndEstado(n.getCategoriaId(), "APROBADA")) {
                double pct = total == 0 ? 0 : conteo.getOrDefault(otra.getId(), 0L) * 100.0 / total;
                double fin = Ponderacion.puntajeFinal(pct, promedios.getOrDefault(otra.getId(), 0.0), peso, hayJurado);
                mejor = Math.max(mejor, fin);
                if (otra.getId().equals(n.getId())) {
                    mio = fin;
                }
            }
            votosRecibidos.put(n.getId(), conteo.getOrDefault(n.getId(), 0L));
            if (mio > 0 && Math.round(mio * 100) == Math.round(mejor * 100)) {
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
