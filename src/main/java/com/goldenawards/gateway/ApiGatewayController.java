package com.goldenawards.gateway;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * API Gateway (punto de entrada documentado de la API).
 * Ubicación: gateway/ApiGatewayController.java
 *
 * En producción este componente se despliega aparte (Spring Cloud Gateway).
 * Aquí centraliza el mapa de rutas hacia los "microservicios lógicos":
 *   - Auth      → /api/auth/**
 *   - Admin     → /api/admin/**
 *   - Organizador → /api/organizador/**
 *   - Votante   → /api/votante/**
 *   - Jurado    → /api/jurado/**
 *   - Nominado  → /api/nominado/**
 *   - Resultados → /api/resultados/**
 *   - Público   → /api/publico/**
 */
@RestController
@RequestMapping("/api/gateway")
public class ApiGatewayController {

    @GetMapping("/routes")
    public Map<String, Object> routes() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("gateway", "Golden Awards API Gateway");
        body.put("version", "1.0");
        body.put("servicios", List.of(
                Map.of("nombre", "auth-service", "basePath", "/api/auth", "descripcion", "Login JWT"),
                Map.of("nombre", "admin-service", "basePath", "/api/admin", "descripcion", "Usuarios y catálogos"),
                Map.of("nombre", "organizador-service", "basePath", "/api/organizador", "descripcion", "Ediciones y nominaciones"),
                Map.of("nombre", "votante-service", "basePath", "/api/votante", "descripcion", "Emisión de votos"),
                Map.of("nombre", "jurado-service", "basePath", "/api/jurado", "descripcion", "Calificacion de nominados"),
                Map.of("nombre", "nominado-service", "basePath", "/api/nominado", "descripcion", "Perfil nominado"),
                Map.of("nombre", "resultados-service", "basePath", "/api/resultados", "descripcion", "Rankings"),
                Map.of("nombre", "publico-service", "basePath", "/api/publico", "descripcion", "Catálogo, comparador, feedback, stats")
        ));
        return body;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "component", "api-gateway");
    }
}
