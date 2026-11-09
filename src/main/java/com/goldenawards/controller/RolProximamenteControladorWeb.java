package com.goldenawards.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Puntos de entrada protegidos de los roles cuyo modulo se construye en el Sprint 2
 * (Moderador, Representante del nominado y Soporte tecnico). Sirven para que el menu y la tarjeta del panel
 * tengan destino y para verificar el acceso por rol (REQ-ROL-002); cada uno se reemplaza por su
 * par de controladores Web y Rest en el Sprint 2.
 */
@Controller
public class RolProximamenteControladorWeb {

    @GetMapping("/moderador/inicio")
    public String moderador(Model model) {
        return pagina(model, "Moderador", "bi-shield-check",
                "Aprobar u ocultar los comentarios de la encuesta del público y revisar propuestas de nominación inapropiadas.");
    }

    @GetMapping("/representante/inicio")
    public String representante(Model model) {
        return pagina(model, "Representante del nominado", "bi-person-lines-fill",
                "Proponer cambios a la biografía, méritos y estadísticas de su nominado; el Organizador los aprueba antes de publicarlos.");
    }

    @GetMapping("/soporte/inicio")
    public String soporte(Model model) {
        return pagina(model, "Soporte técnico", "bi-headset",
                "Atender los tickets de los usuarios y cambiar su estado.");
    }

    private String pagina(Model model, String rol, String icono, String funcion) {
        model.addAttribute("rolNombre", rol);
        model.addAttribute("icono", icono);
        model.addAttribute("funcion", funcion);
        return "rol/proximamente";
    }
}
