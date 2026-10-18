package com.goldenawards.controller;

import com.goldenawards.model.Usuario;
import com.goldenawards.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class InicioControladorRest {

    private final UsuarioRepository usuarioRepository;

    public InicioControladorRest(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // Endpoint de salud (publico): comprueba que la API responde
    @GetMapping("/estado")
    public Map<String, Object> estado() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("estado", "UP");
        m.put("aplicacion", "Golden Awards");
        m.put("hora", LocalDateTime.now().toString());
        return m;
    }

    // Datos del usuario autenticado
    @GetMapping("/yo")
    public Map<String, Object> yo(Authentication auth) {
        Usuario u = usuarioRepository.findByUsername(auth.getName()).orElseThrow();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("nombre", u.getNombre());
        m.put("username", u.getUsername());
        m.put("rol", u.getRol());
        m.put("jugadorId", u.getJugadorId());
        return m;
    }
}
