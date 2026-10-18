# Golden Awards

Plataforma de gestión, nominación y votación de premios deportivos.

**Java 17 · Spring Boot 3.2 · MongoDB Atlas · Thymeleaf · Bootstrap 5 · JWT**

## Estructura importante

```
com.goldenawards
├── controller/          ← Web + Rest en la MISMA carpeta
│   ├── *ControladorWeb.java
│   ├── *ControladorRest.java
│   ├── AuthControladorRest.java      (login JWT)
│   ├── JuradoControladorWeb.java     (rol JURADO: evaluar nominados)
│   ├── JuradoControladorRest.java
│   ├── PublicoControladorWeb.java
│   └── PublicoControladorRest.java
├── security/jwt/
│   ├── JwtUtil.java                  ← generación/validación JWT
│   └── JwtAuthFilter.java            ← filtro Bearer para /api/**
├── gateway/
│   └── ApiGatewayController.java     ← mapa de rutas /api/gateway/**
├── config/              SecurityConfig, DataInitializer, Ponderacion (votos + jurado)
├── model/
└── repository/
```

## JWT, Gateway y “microservicios”

| Componente | Ubicación | Qué hace |
|------------|-----------|----------|
| **JWT** | `security/jwt/JwtUtil.java` + `JwtAuthFilter.java` | Login en `POST /api/auth/login` devuelve Bearer token. El filtro valida tokens en rutas `/api/**` protegidas. |
| **API Gateway** | `gateway/ApiGatewayController.java` | Punto de entrada documentado: `GET /api/gateway/routes` y `/api/gateway/health`. En producción sería Spring Cloud Gateway; aquí centraliza el mapa de servicios. |
| **Microservicios lógicos** | Misma app, paquetes por dominio | Auth, Admin, Organizador, Votante, Jurado, Nominado, Resultados, Público. Listos para extraer a jars independientes manteniendo los mismos paths `/api/...`. |

No son procesos separados en este prototipo (un solo deploy Spring Boot), pero la API y el JWT siguen el contrato de microservicios + gateway.

### Ejemplo JWT

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Usar el token:
curl http://localhost:8080/api/admin/... -H "Authorization: Bearer <token>"
```

## Área pública (sin ser votante)

| Ruta | Función |
|------|---------|
| `/publico/catalogo` | Filtros: edición, categoría, estado, búsqueda, orden A-Z |
| `/publico/comparar` | Comparar 2–3 nominados (bio + goles, asistencias, partidos, trofeos) |
| `/publico/feedback` | Encuesta estrellas + comentario |
| `/publico/estadisticas` | Gráficos Chart.js (barras / dona) |

API pública: `/api/publico/catalogo`, `/api/publico/feedback`, `/api/publico/estadisticas`

## Ejecutar

```bash
mvn spring-boot:run
```

http://localhost:8080

## Rol Jurado

Un miembro del jurado califica de **1 a 10** a cada nominado aprobado mientras la edición está en **votación**
(puede corregir su nota mientras siga en ese periodo). Su calificación se combina con los votos del público:

```
puntajeFinal = %votos × (100 − peso)/100  +  promedioJurado × 10 × peso/100
```

- `peso` es el parámetro global **`peso_jurado`** (por defecto 40; el Administrador lo cambia en *Parámetros*; 0 = solo cuenta el público).
- Si una categoría aún no tiene calificaciones del jurado, el ranking usa solo los votos.
- El ranking de `/resultados` y el resultado que ve el Nominado usan esta misma fórmula.

| Web | API |
|-----|-----|
| `/jurado/evaluar` · `/jurado/historial` | `GET /api/jurado/ediciones`, `GET /api/jurado/ediciones/{id}/nominados`, `POST /api/jurado/puntajes` (`{"nominacionId":"…","puntaje":8,"comentario":"…"}`), `GET /api/jurado/puntajes` |

Al arrancar, `DataInitializer` crea los usuarios `jurado` y `jurado2`, el parámetro `peso_jurado` y calificaciones de
ejemplo en la edición 2025 **aunque la base de datos ya tenga datos** (solo agrega lo que falta).

## Usuarios de prueba

| Rol | Usuario | Contraseña |
|-----|---------|------------|
| Administrador | `admin` | `admin123` |
| Organizador | `organizador` | `organizador123` |
| Votante | `votante` | `votante123` |
| Jurado | `jurado` | `jurado123` |
| Nominado | `nominado` | `nominado123` |
| Público | `publico` | `publico123` |

Extras: `votante2`, `votante3` (`votante123`) y `jurado2` (`jurado123`).
