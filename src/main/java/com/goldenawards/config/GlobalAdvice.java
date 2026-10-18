package com.goldenawards.config;

import com.goldenawards.model.Parametro;
import com.goldenawards.model.Usuario;
import com.goldenawards.repository.ParametroRepository;
import com.goldenawards.repository.UsuarioRepository;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

// Comparte los parametros globales (configurados por el Administrador) con todas las vistas web
@ControllerAdvice(basePackages = "com.goldenawards.controller")
public class GlobalAdvice {

    private final ParametroRepository parametroRepository;
    private final UsuarioRepository usuarioRepository;

    public GlobalAdvice(ParametroRepository parametroRepository, UsuarioRepository usuarioRepository) {
        this.parametroRepository = parametroRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @ModelAttribute("nombreUsuario")
    public String nombreUsuario(Principal principal) {
        if (principal == null) {
            return null;
        }
        return usuarioRepository.findByUsername(principal.getName())
                .map(Usuario::getNombre).orElse(principal.getName());
    }

    @ModelAttribute("plataforma")
    public String plataforma() {
        return parametroRepository.findByClave("nombre_plataforma")
                .map(Parametro::getValor).orElse("Golden Awards");
    }

    @ModelAttribute("lema")
    public String lema() {
        return parametroRepository.findByClave("lema")
                .map(Parametro::getValor).orElse("");
    }
}
