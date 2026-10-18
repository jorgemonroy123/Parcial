package com.goldenawards.controller;

import com.goldenawards.model.Usuario;
import com.goldenawards.repository.UsuarioRepository;
import com.goldenawards.security.jwt.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Microservicio lógico de autenticación (REST).
 * Emite JWT para clientes API.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthControladorRest {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthControladorRest(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "username y password requeridos"));
        }
        Usuario u = usuarioRepository.findByUsername(username).orElse(null);
        if (u == null || !u.isActivo() || !passwordEncoder.matches(password, u.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Credenciales inválidas"));
        }
        String token = jwtUtil.generateToken(u.getUsername(), u.getRol());
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("token", token);
        resp.put("type", "Bearer");
        resp.put("username", u.getUsername());
        resp.put("rol", u.getRol());
        resp.put("nombre", u.getNombre());
        return ResponseEntity.ok(resp);
    }
}
