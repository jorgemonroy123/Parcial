package com.goldenawards.controller;

import com.goldenawards.model.*;
import com.goldenawards.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

// Rankings y estadisticas: disponible para cualquier usuario autenticado
@RestController
@RequestMapping("/api/resultados")
public class ResultadoControladorRest {

    private final EdicionRepository edicionRepository;
    private final CategoriaRepository categoriaRepository;
    private final NominacionRepository nominacionRepository;
    private final VotoRepository votoRepository;
    private final JugadorRepository jugadorRepository;
    private final UsuarioRepository usuarioRepository;

    public ResultadoControladorRest(EdicionRepository edicionRepository, CategoriaRepository categoriaRepository,
                                    NominacionRepository nominacionRepository, VotoRepository votoRepository,
                                    JugadorRepository jugadorRepository, UsuarioRepository usuarioRepository) {
        this.edicionRepository = edicionRepository;
        this.categoriaRepository = categoriaRepository;
        this.nominacionRepository = nominacionRepository;
        this.votoRepository = votoRepository;
        this.jugadorRepository = jugadorRepository;
        this.usuarioRepository = usuarioRepository;
    }

    private boolean veTodo(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_ORGANIZADOR"));
    }

    // Ediciones cuyos resultados puede consultar el usuario
    @GetMapping
    public List<Edicion> ediciones(Authentication auth) {
        boolean staff = veTodo(auth);
        return edicionRepository.findAll().stream()
                .filter(e -> staff || e.isResultadosPublicados())
                .collect(Collectors.toList());
    }

    // Ranking por categoria y estadisticas de una edicion
    @GetMapping("/{id}")
    public ResponseEntity<Object> detalle(@PathVariable String id, Authentication auth) {
        Edicion e = edicionRepository.findById(id).orElse(null);
        if (e == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Edicion no encontrada"));
        }
        if (!veTodo(auth) && !e.isResultadosPublicados()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Los resultados de esta edicion aun no estan publicados"));
        }

        List<Voto> votos = votoRepository.findByEdicionId(id);
        Map<String, Jugador> jugadores = jugadorRepository.findAll().stream()
                .collect(Collectors.toMap(Jugador::getId, j -> j));

        List<Map<String, Object>> categorias = new ArrayList<>();
        for (Categoria c : categoriaRepository.findByEdicionId(id)) {
            List<Voto> votosCategoria = votos.stream()
                    .filter(v -> c.getId().equals(v.getCategoriaId()))
                    .collect(Collectors.toList());
            Map<String, Long> conteo = votosCategoria.stream()
                    .collect(Collectors.groupingBy(Voto::getNominacionId, Collectors.counting()));
            long total = votosCategoria.size();

            List<Nominacion> aprobadas = nominacionRepository.findByCategoriaIdAndEstado(c.getId(), "APROBADA").stream()
                    .sorted((a, b) -> Long.compare(conteo.getOrDefault(b.getId(), 0L), conteo.getOrDefault(a.getId(), 0L)))
                    .collect(Collectors.toList());

            List<Map<String, Object>> ranking = new ArrayList<>();
            int posicion = 0;
            int indice = 0;
            long anterior = -1;
            for (Nominacion n : aprobadas) {
                indice++;
                long cantidad = conteo.getOrDefault(n.getId(), 0L);
                if (cantidad != anterior) {
                    posicion = indice;
                    anterior = cantidad;
                }
                Jugador j = jugadores.get(n.getJugadorId());
                Map<String, Object> fila = new LinkedHashMap<>();
                fila.put("posicion", posicion);
                fila.put("nombre", j != null ? j.getNombre() : "(jugador eliminado)");
                fila.put("equipo", j != null ? j.getEquipo() : "");
                fila.put("pais", j != null ? j.getPais() : "");
                fila.put("votos", cantidad);
                fila.put("porcentaje", total == 0 ? 0L : Math.round(cantidad * 100.0 / total));
                ranking.add(fila);
            }
            Map<String, Object> categoria = new LinkedHashMap<>();
            categoria.put("nombre", c.getNombre());
            categoria.put("descripcion", c.getDescripcion());
            categoria.put("totalVotos", total);
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

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("edicion", e);
        respuesta.put("resumen", resumen);
        respuesta.put("categorias", categorias);
        return ResponseEntity.ok(respuesta);
    }
}
