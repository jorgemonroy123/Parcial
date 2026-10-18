package com.goldenawards.controller;

import com.goldenawards.config.Ponderacion;
import com.goldenawards.model.*;
import com.goldenawards.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

// Rankings y estadisticas: disponible para cualquier usuario autenticado
@Controller
@RequestMapping("/resultados")
public class ResultadoControladorWeb {

    private final EdicionRepository edicionRepository;
    private final CategoriaRepository categoriaRepository;
    private final NominacionRepository nominacionRepository;
    private final VotoRepository votoRepository;
    private final JugadorRepository jugadorRepository;
    private final UsuarioRepository usuarioRepository;
    private final PuntajeRepository puntajeRepository;
    private final ParametroRepository parametroRepository;

    public ResultadoControladorWeb(EdicionRepository edicionRepository, CategoriaRepository categoriaRepository,
                                   NominacionRepository nominacionRepository, VotoRepository votoRepository,
                                   JugadorRepository jugadorRepository, UsuarioRepository usuarioRepository,
                                   PuntajeRepository puntajeRepository, ParametroRepository parametroRepository) {
        this.edicionRepository = edicionRepository;
        this.categoriaRepository = categoriaRepository;
        this.nominacionRepository = nominacionRepository;
        this.votoRepository = votoRepository;
        this.jugadorRepository = jugadorRepository;
        this.usuarioRepository = usuarioRepository;
        this.puntajeRepository = puntajeRepository;
        this.parametroRepository = parametroRepository;
    }

    // Administrador y Organizador ven todo (conteo en vivo); los demas solo resultados publicados
    private boolean veTodo(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_ORGANIZADOR"));
    }

    @GetMapping
    public String lista(Authentication auth, Model model) {
        boolean staff = veTodo(auth);
        List<Edicion> visibles = edicionRepository.findAll().stream()
                .filter(e -> staff || e.isResultadosPublicados())
                .sorted(Comparator.comparingInt(Edicion::getAnio).reversed())
                .collect(Collectors.toList());
        model.addAttribute("ediciones", visibles);
        model.addAttribute("staff", staff);
        return "resultados/index";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable String id, Authentication auth, Model model, RedirectAttributes ra) {
        Edicion e = edicionRepository.findById(id).orElse(null);
        if (e == null || (!veTodo(auth) && !e.isResultadosPublicados())) {
            ra.addFlashAttribute("mensaje", "Los resultados de esa edicion no estan disponibles");
            ra.addFlashAttribute("tipo", "warning");
            return "redirect:/resultados";
        }

        List<Voto> votos = votoRepository.findByEdicionId(id);
        Map<String, Jugador> jugadores = jugadorRepository.findAll().stream()
                .collect(Collectors.toMap(Jugador::getId, j -> j));

        int peso = Ponderacion.pesoJurado(parametroRepository);
        List<Puntaje> puntajes = puntajeRepository.findByEdicionId(id);

        List<Map<String, Object>> categorias = new ArrayList<>();
        for (Categoria c : categoriaRepository.findByEdicionId(id)) {
            List<Voto> votosCategoria = votos.stream()
                    .filter(v -> c.getId().equals(v.getCategoriaId()))
                    .collect(Collectors.toList());
            Map<String, Long> conteo = votosCategoria.stream()
                    .collect(Collectors.groupingBy(Voto::getNominacionId, Collectors.counting()));
            long total = votosCategoria.size();

            // Calificaciones del jurado en esta categoria (promedio por nominado)
            List<Puntaje> puntajesCategoria = puntajes.stream()
                    .filter(p -> c.getId().equals(p.getCategoriaId()))
                    .collect(Collectors.toList());
            boolean hayJurado = !puntajesCategoria.isEmpty();
            Map<String, Double> promedios = puntajesCategoria.stream()
                    .collect(Collectors.groupingBy(Puntaje::getNominacionId, Collectors.averagingInt(Puntaje::getPuntaje)));
            Map<String, Long> juradosPorNominado = puntajesCategoria.stream()
                    .collect(Collectors.groupingBy(Puntaje::getNominacionId, Collectors.counting()));

            // Puntaje final = votos del publico + calificacion del jurado (ver config/Ponderacion)
            Map<String, Double> finales = new HashMap<>();
            List<Nominacion> aprobadas = nominacionRepository.findByCategoriaIdAndEstado(c.getId(), "APROBADA");
            for (Nominacion n : aprobadas) {
                double pct = total == 0 ? 0 : conteo.getOrDefault(n.getId(), 0L) * 100.0 / total;
                finales.put(n.getId(), Ponderacion.puntajeFinal(pct, promedios.getOrDefault(n.getId(), 0.0), peso, hayJurado));
            }
            aprobadas = aprobadas.stream()
                    .sorted((a, b) -> Double.compare(finales.get(b.getId()), finales.get(a.getId())))
                    .collect(Collectors.toList());

            List<Map<String, Object>> ranking = new ArrayList<>();
            int posicion = 0;
            int indice = 0;
            long anterior = -1;
            for (Nominacion n : aprobadas) {
                indice++;
                long cantidad = conteo.getOrDefault(n.getId(), 0L);
                long finalRedondeado = Math.round(finales.get(n.getId()) * 100);
                if (finalRedondeado != anterior) {   // los empates comparten posicion
                    posicion = indice;
                    anterior = finalRedondeado;
                }
                Jugador j = jugadores.get(n.getJugadorId());
                Map<String, Object> fila = new LinkedHashMap<>();
                fila.put("posicion", posicion);
                fila.put("nombre", j != null ? j.getNombre() : "(jugador eliminado)");
                fila.put("equipo", j != null ? j.getEquipo() : "");
                fila.put("pais", j != null ? j.getPais() : "");
                fila.put("votos", cantidad);
                fila.put("porcentaje", total == 0 ? 0L : Math.round(cantidad * 100.0 / total));
                fila.put("promedioJurado", promedios.containsKey(n.getId())
                        ? Double.valueOf(Math.round(promedios.get(n.getId()) * 10) / 10.0) : null);
                fila.put("juradosQueCalificaron", juradosPorNominado.getOrDefault(n.getId(), 0L));
                fila.put("puntajeFinal", Math.round(finales.get(n.getId()) * 10) / 10.0);
                ranking.add(fila);
            }
            Map<String, Object> categoria = new LinkedHashMap<>();
            categoria.put("nombre", c.getNombre());
            categoria.put("descripcion", c.getDescripcion());
            categoria.put("totalVotos", total);
            categoria.put("hayJurado", hayJurado);
            categoria.put("pesoJurado", peso);
            categoria.put("ranking", ranking);
            categorias.add(categoria);
        }

        long votantes = votos.stream().map(Voto::getVotanteId).distinct().count();
        long registrados = usuarioRepository.findByRol("VOTANTE").size();
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("totalVotos", votos.size());
        resumen.put("votantes", votantes);
        resumen.put("participacion", registrados == 0 ? 0L : Math.round(votantes * 100.0 / registrados));
        resumen.put("categorias", categorias.size());
        resumen.put("nominados", nominacionRepository.findByEdicionId(id).stream()
                .filter(n -> "APROBADA".equals(n.getEstado())).count());

        model.addAttribute("edicion", e);
        model.addAttribute("resumen", resumen);
        model.addAttribute("categorias", categorias);
        return "resultados/detalle";
    }
}
