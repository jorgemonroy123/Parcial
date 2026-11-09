package com.goldenawards.controller;

import com.goldenawards.model.Jugador;
import com.goldenawards.model.Parametro;
import com.goldenawards.model.Usuario;
import com.goldenawards.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class AdminControladorRest {

    private static final List<String> ROLES = List.of("ADMIN", "ORGANIZADOR", "VOTANTE", "NOMINADO", "JURADO",
            "AUDITOR", "MODERADOR", "PERIODISTA", "REPRESENTANTE", "SOPORTE", "PUBLICO");

    private final UsuarioRepository usuarioRepository;
    private final JugadorRepository jugadorRepository;
    private final ParametroRepository parametroRepository;
    private final NominacionRepository nominacionRepository;
    private final VotoRepository votoRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminControladorRest(UsuarioRepository usuarioRepository, JugadorRepository jugadorRepository,
                                ParametroRepository parametroRepository, NominacionRepository nominacionRepository,
                                VotoRepository votoRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.jugadorRepository = jugadorRepository;
        this.parametroRepository = parametroRepository;
        this.nominacionRepository = nominacionRepository;
        this.votoRepository = votoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private ResponseEntity<Object> error(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("error", mensaje));
    }

    // ===================== USUARIOS =====================

    @GetMapping("/usuarios")
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    @GetMapping("/usuarios/{id}")
    public ResponseEntity<Object> obtenerUsuario(@PathVariable String id) {
        Optional<Usuario> u = usuarioRepository.findById(id);
        return u.<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> error(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    @PostMapping("/usuarios")
    public ResponseEntity<Object> crearUsuario(@RequestBody Usuario datos) {
        String problema = validarUsuario(datos, null, true);
        if (problema != null) {
            return error(HttpStatus.BAD_REQUEST, problema);
        }
        datos.setId(null);
        datos.setUsername(datos.getUsername().trim());
        datos.setPassword(passwordEncoder.encode(datos.getPassword()));
        if (!"NOMINADO".equals(datos.getRol())) {
            datos.setJugadorId(null);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioRepository.save(datos));
    }

    @PutMapping("/usuarios/{id}")
    public ResponseEntity<Object> actualizarUsuario(@PathVariable String id, @RequestBody Usuario datos,
                                                    Principal principal) {
        Usuario u = usuarioRepository.findById(id).orElse(null);
        if (u == null) {
            return error(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }
        String problema = validarUsuario(datos, id, false);
        if (problema != null) {
            return error(HttpStatus.BAD_REQUEST, problema);
        }
        if (u.getUsername().equals(principal.getName()) && (!datos.isActivo() || !"ADMIN".equals(datos.getRol()))) {
            return error(HttpStatus.BAD_REQUEST, "No puedes desactivarte ni quitarte el rol de administrador");
        }
        u.setNombre(datos.getNombre());
        u.setUsername(datos.getUsername().trim());
        u.setRol(datos.getRol());
        u.setJugadorId("NOMINADO".equals(datos.getRol()) ? datos.getJugadorId() : null);
        u.setActivo(datos.isActivo());
        if (datos.getPassword() != null && !datos.getPassword().isBlank()) {
            u.setPassword(passwordEncoder.encode(datos.getPassword()));
        }
        return ResponseEntity.ok(usuarioRepository.save(u));
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Object> eliminarUsuario(@PathVariable String id, Principal principal) {
        Usuario u = usuarioRepository.findById(id).orElse(null);
        if (u == null) {
            return error(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }
        if (u.getUsername().equals(principal.getName())) {
            return error(HttpStatus.BAD_REQUEST, "No puedes eliminar tu propia cuenta");
        }
        if (!votoRepository.findByVotanteId(id).isEmpty() || !nominacionRepository.findByPropuestoPorId(id).isEmpty()) {
            return error(HttpStatus.CONFLICT, "El usuario tiene votos o nominaciones registradas; desactivalo en lugar de eliminarlo");
        }
        usuarioRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("mensaje", "Usuario eliminado"));
    }

    private String validarUsuario(Usuario d, String idActual, boolean esNuevo) {
        if (d.getNombre() == null || d.getNombre().isBlank()) return "El nombre es obligatorio";
        if (d.getUsername() == null || d.getUsername().isBlank()) return "El nombre de usuario es obligatorio";
        if (d.getRol() == null || !ROLES.contains(d.getRol())) return "Rol no valido. Usa: " + ROLES;
        if (esNuevo && (d.getPassword() == null || d.getPassword().isBlank())) return "La contrasena es obligatoria";
        if ("NOMINADO".equals(d.getRol())) {
            if (d.getJugadorId() == null || !jugadorRepository.existsById(d.getJugadorId())) {
                return "Un usuario NOMINADO requiere un jugadorId valido";
            }
        }
        Optional<Usuario> existente = usuarioRepository.findByUsername(d.getUsername().trim());
        if (existente.isPresent() && (idActual == null || !existente.get().getId().equals(idActual))) {
            return "El nombre de usuario ya existe";
        }
        return null;
    }

    // ===================== JUGADORES =====================

    @GetMapping("/jugadores")
    public List<Jugador> listarJugadores() {
        return jugadorRepository.findAll();
    }

    @GetMapping("/jugadores/{id}")
    public ResponseEntity<Object> obtenerJugador(@PathVariable String id) {
        Optional<Jugador> j = jugadorRepository.findById(id);
        return j.<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> error(HttpStatus.NOT_FOUND, "Jugador no encontrado"));
    }

    @PostMapping("/jugadores")
    public ResponseEntity<Object> crearJugador(@RequestBody Jugador datos) {
        if (datos.getNombre() == null || datos.getNombre().isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
        datos.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(jugadorRepository.save(datos));
    }

    @PutMapping("/jugadores/{id}")
    public ResponseEntity<Object> actualizarJugador(@PathVariable String id, @RequestBody Jugador datos) {
        Jugador j = jugadorRepository.findById(id).orElse(null);
        if (j == null) {
            return error(HttpStatus.NOT_FOUND, "Jugador no encontrado");
        }
        if (datos.getNombre() == null || datos.getNombre().isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
        j.setNombre(datos.getNombre());
        j.setEquipo(datos.getEquipo());
        j.setPosicion(datos.getPosicion());
        j.setPais(datos.getPais());
        return ResponseEntity.ok(jugadorRepository.save(j));
    }

    @DeleteMapping("/jugadores/{id}")
    public ResponseEntity<Object> eliminarJugador(@PathVariable String id) {
        if (!jugadorRepository.existsById(id)) {
            return error(HttpStatus.NOT_FOUND, "Jugador no encontrado");
        }
        boolean nominado = !nominacionRepository.findByJugadorId(id).isEmpty();
        boolean asociado = usuarioRepository.findByRol("NOMINADO").stream().anyMatch(u -> id.equals(u.getJugadorId()));
        if (nominado || asociado) {
            return error(HttpStatus.CONFLICT, "El jugador tiene nominaciones o un usuario asociado; no se puede eliminar");
        }
        jugadorRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("mensaje", "Jugador eliminado"));
    }

    // ===================== PARAMETROS =====================

    @GetMapping("/parametros")
    public List<Parametro> listarParametros() {
        return parametroRepository.findAll();
    }

    @GetMapping("/parametros/{id}")
    public ResponseEntity<Object> obtenerParametro(@PathVariable String id) {
        Optional<Parametro> p = parametroRepository.findById(id);
        return p.<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> error(HttpStatus.NOT_FOUND, "Parametro no encontrado"));
    }

    @PostMapping("/parametros")
    public ResponseEntity<Object> crearParametro(@RequestBody Parametro datos) {
        if (datos.getClave() == null || datos.getClave().isBlank() || datos.getValor() == null) {
            return error(HttpStatus.BAD_REQUEST, "La clave y el valor son obligatorios");
        }
        if (parametroRepository.findByClave(datos.getClave().trim()).isPresent()) {
            return error(HttpStatus.BAD_REQUEST, "Ya existe un parametro con esa clave");
        }
        datos.setId(null);
        datos.setClave(datos.getClave().trim());
        return ResponseEntity.status(HttpStatus.CREATED).body(parametroRepository.save(datos));
    }

    @PutMapping("/parametros/{id}")
    public ResponseEntity<Object> actualizarParametro(@PathVariable String id, @RequestBody Parametro datos) {
        Parametro p = parametroRepository.findById(id).orElse(null);
        if (p == null) {
            return error(HttpStatus.NOT_FOUND, "Parametro no encontrado");
        }
        if (datos.getClave() == null || datos.getClave().isBlank() || datos.getValor() == null) {
            return error(HttpStatus.BAD_REQUEST, "La clave y el valor son obligatorios");
        }
        Optional<Parametro> otro = parametroRepository.findByClave(datos.getClave().trim());
        if (otro.isPresent() && !otro.get().getId().equals(id)) {
            return error(HttpStatus.BAD_REQUEST, "Ya existe un parametro con esa clave");
        }
        p.setClave(datos.getClave().trim());
        p.setValor(datos.getValor());
        p.setDescripcion(datos.getDescripcion());
        return ResponseEntity.ok(parametroRepository.save(p));
    }

    @DeleteMapping("/parametros/{id}")
    public ResponseEntity<Object> eliminarParametro(@PathVariable String id) {
        if (!parametroRepository.existsById(id)) {
            return error(HttpStatus.NOT_FOUND, "Parametro no encontrado");
        }
        parametroRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("mensaje", "Parametro eliminado"));
    }
}
