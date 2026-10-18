package com.goldenawards.controller;

import com.goldenawards.model.Jugador;
import com.goldenawards.model.Nominacion;
import com.goldenawards.model.Usuario;
import com.goldenawards.repository.JugadorRepository;
import com.goldenawards.repository.NominacionRepository;
import com.goldenawards.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nominado")
public class NominadoControladorRest {

    private final UsuarioRepository usuarioRepository;
    private final JugadorRepository jugadorRepository;
    private final NominacionRepository nominacionRepository;

    public NominadoControladorRest(UsuarioRepository usuarioRepository, JugadorRepository jugadorRepository,
                                   NominacionRepository nominacionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.jugadorRepository = jugadorRepository;
        this.nominacionRepository = nominacionRepository;
    }

    // Perfil del jugador asociado al usuario NOMINADO autenticado
    @GetMapping("/perfil")
    public ResponseEntity<Object> perfil(Principal principal) {
        Usuario u = usuarioRepository.findByUsername(principal.getName()).orElseThrow();
        Jugador j = u.getJugadorId() == null ? null : jugadorRepository.findById(u.getJugadorId()).orElse(null);
        if (j == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Tu usuario no tiene un jugador asociado"));
        }
        return ResponseEntity.ok(j);
    }

    // Nominaciones en las que participa el jugador (los resultados oficiales estan en /api/resultados)
    @GetMapping("/nominaciones")
    public List<Nominacion> nominaciones(Principal principal) {
        Usuario u = usuarioRepository.findByUsername(principal.getName()).orElseThrow();
        if (u.getJugadorId() == null) {
            return List.of();
        }
        return nominacionRepository.findByJugadorId(u.getJugadorId());
    }
}
