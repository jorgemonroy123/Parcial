package com.goldenawards.controller;

import com.goldenawards.model.Jugador;
import com.goldenawards.model.Parametro;
import com.goldenawards.model.Usuario;
import com.goldenawards.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminControladorWeb {

    private static final List<String> ROLES = List.of("ADMIN", "ORGANIZADOR", "VOTANTE", "NOMINADO", "JURADO",
            "AUDITOR", "MODERADOR", "PERIODISTA", "REPRESENTANTE", "SOPORTE", "PUBLICO");

    private final UsuarioRepository usuarioRepository;
    private final JugadorRepository jugadorRepository;
    private final ParametroRepository parametroRepository;
    private final NominacionRepository nominacionRepository;
    private final VotoRepository votoRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminControladorWeb(UsuarioRepository usuarioRepository, JugadorRepository jugadorRepository,
                               ParametroRepository parametroRepository, NominacionRepository nominacionRepository,
                               VotoRepository votoRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.jugadorRepository = jugadorRepository;
        this.parametroRepository = parametroRepository;
        this.nominacionRepository = nominacionRepository;
        this.votoRepository = votoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private String resultado(RedirectAttributes ra, boolean exito, String mensaje, String destino) {
        ra.addFlashAttribute("mensaje", mensaje);
        ra.addFlashAttribute("tipo", exito ? "success" : "danger");
        return "redirect:" + destino;
    }

    // ===================== USUARIOS =====================

    @GetMapping("/usuarios")
    public String usuarios(Model model) {
        model.addAttribute("usuarios", usuarioRepository.findAll());
        model.addAttribute("jugadores", jugadorRepository.findAll());
        model.addAttribute("jugadoresPorId", jugadorRepository.findAll().stream()
                .collect(Collectors.toMap(Jugador::getId, j -> j)));
        model.addAttribute("roles", ROLES);
        return "admin/usuarios";
    }

    @PostMapping("/usuarios/guardar")
    public String guardarUsuario(@RequestParam(required = false) String id,
                                 @RequestParam String nombre,
                                 @RequestParam String username,
                                 @RequestParam(required = false) String password,
                                 @RequestParam String rol,
                                 @RequestParam(required = false) String jugadorId,
                                 @RequestParam(defaultValue = "false") boolean activo,
                                 Principal principal, RedirectAttributes ra) {
        String destino = "/admin/usuarios";
        boolean esNuevo = id == null || id.isBlank();
        username = username.trim();

        if (!ROLES.contains(rol)) {
            return resultado(ra, false, "Rol no valido", destino);
        }
        if ("NOMINADO".equals(rol) && (jugadorId == null || jugadorId.isBlank())) {
            return resultado(ra, false, "Un usuario NOMINADO debe estar asociado a un jugador", destino);
        }
        Optional<Usuario> existente = usuarioRepository.findByUsername(username);
        if (existente.isPresent() && (esNuevo || !existente.get().getId().equals(id))) {
            return resultado(ra, false, "El nombre de usuario ya existe", destino);
        }

        Usuario u;
        if (esNuevo) {
            if (password == null || password.isBlank()) {
                return resultado(ra, false, "La contrasena es obligatoria para un usuario nuevo", destino);
            }
            u = new Usuario();
        } else {
            u = usuarioRepository.findById(id).orElse(null);
            if (u == null) {
                return resultado(ra, false, "Usuario no encontrado", destino);
            }
            if (u.getUsername().equals(principal.getName()) && (!activo || !"ADMIN".equals(rol))) {
                return resultado(ra, false, "No puedes desactivarte ni quitarte el rol de administrador", destino);
            }
        }

        u.setNombre(nombre.trim());
        u.setUsername(username);
        u.setRol(rol);
        u.setJugadorId("NOMINADO".equals(rol) ? jugadorId : null);
        u.setActivo(activo);
        if (password != null && !password.isBlank()) {
            u.setPassword(passwordEncoder.encode(password));
        }
        usuarioRepository.save(u);
        return resultado(ra, true, esNuevo ? "Usuario creado" : "Usuario actualizado", destino);
    }

    @PostMapping("/usuarios/{id}/eliminar")
    public String eliminarUsuario(@PathVariable String id, Principal principal, RedirectAttributes ra) {
        String destino = "/admin/usuarios";
        Usuario u = usuarioRepository.findById(id).orElse(null);
        if (u == null) {
            return resultado(ra, false, "Usuario no encontrado", destino);
        }
        if (u.getUsername().equals(principal.getName())) {
            return resultado(ra, false, "No puedes eliminar tu propia cuenta", destino);
        }
        if (!votoRepository.findByVotanteId(id).isEmpty() || !nominacionRepository.findByPropuestoPorId(id).isEmpty()) {
            return resultado(ra, false, "El usuario tiene votos o nominaciones registradas; desactivalo en lugar de eliminarlo", destino);
        }
        usuarioRepository.deleteById(id);
        return resultado(ra, true, "Usuario eliminado", destino);
    }

    // ===================== JUGADORES (catalogo) =====================

    @GetMapping("/jugadores")
    public String jugadores(Model model) {
        model.addAttribute("jugadores", jugadorRepository.findAll());
        return "admin/jugadores";
    }

    @PostMapping("/jugadores/guardar")
    public String guardarJugador(@RequestParam(required = false) String id,
                                 @RequestParam String nombre,
                                 @RequestParam String equipo,
                                 @RequestParam String posicion,
                                 @RequestParam String pais,
                                 RedirectAttributes ra) {
        String destino = "/admin/jugadores";
        boolean esNuevo = id == null || id.isBlank();
        Jugador j;
        if (esNuevo) {
            j = new Jugador();
        } else {
            j = jugadorRepository.findById(id).orElse(null);
            if (j == null) {
                return resultado(ra, false, "Jugador no encontrado", destino);
            }
        }
        j.setNombre(nombre.trim());
        j.setEquipo(equipo.trim());
        j.setPosicion(posicion.trim());
        j.setPais(pais.trim());
        jugadorRepository.save(j);
        return resultado(ra, true, esNuevo ? "Jugador creado" : "Jugador actualizado", destino);
    }

    @PostMapping("/jugadores/{id}/eliminar")
    public String eliminarJugador(@PathVariable String id, RedirectAttributes ra) {
        String destino = "/admin/jugadores";
        if (!jugadorRepository.existsById(id)) {
            return resultado(ra, false, "Jugador no encontrado", destino);
        }
        boolean nominado = !nominacionRepository.findByJugadorId(id).isEmpty();
        boolean asociado = usuarioRepository.findByRol("NOMINADO").stream().anyMatch(u -> id.equals(u.getJugadorId()));
        if (nominado || asociado) {
            return resultado(ra, false, "El jugador tiene nominaciones o un usuario asociado; no se puede eliminar", destino);
        }
        jugadorRepository.deleteById(id);
        return resultado(ra, true, "Jugador eliminado", destino);
    }

    // ===================== PARAMETROS GLOBALES =====================

    @GetMapping("/parametros")
    public String parametros(Model model) {
        model.addAttribute("parametros", parametroRepository.findAll());
        return "admin/parametros";
    }

    @PostMapping("/parametros/guardar")
    public String guardarParametro(@RequestParam(required = false) String id,
                                   @RequestParam String clave,
                                   @RequestParam String valor,
                                   @RequestParam(required = false) String descripcion,
                                   RedirectAttributes ra) {
        String destino = "/admin/parametros";
        boolean esNuevo = id == null || id.isBlank();
        clave = clave.trim();
        Optional<Parametro> existente = parametroRepository.findByClave(clave);
        if (existente.isPresent() && (esNuevo || !existente.get().getId().equals(id))) {
            return resultado(ra, false, "Ya existe un parametro con esa clave", destino);
        }
        Parametro p;
        if (esNuevo) {
            p = new Parametro();
        } else {
            p = parametroRepository.findById(id).orElse(null);
            if (p == null) {
                return resultado(ra, false, "Parametro no encontrado", destino);
            }
        }
        p.setClave(clave);
        p.setValor(valor.trim());
        p.setDescripcion(descripcion);
        parametroRepository.save(p);
        return resultado(ra, true, esNuevo ? "Parametro creado" : "Parametro actualizado", destino);
    }

    @PostMapping("/parametros/{id}/eliminar")
    public String eliminarParametro(@PathVariable String id, RedirectAttributes ra) {
        if (!parametroRepository.existsById(id)) {
            return resultado(ra, false, "Parametro no encontrado", "/admin/parametros");
        }
        parametroRepository.deleteById(id);
        return resultado(ra, true, "Parametro eliminado", "/admin/parametros");
    }
}
