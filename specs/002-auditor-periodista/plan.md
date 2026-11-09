# plan.md — Auditor y Periodista · Puerta 2

| | |
|---|---|
| Tarjeta | D16 (junto con ADR-003) |
| Responsable | Juan Ortega |
| Revisa | Juan Hernández |
| Depende de | D14 (spec v1.0) |
| Trazabilidad | ADR-003 · NFR-AUD-001 · C-02 |

## 1. Enfoque
Se sigue el patrón del Jurado (C-08): por cada rol, un par `…ControladorWeb` / `…ControladorRest` en la misma carpeta
`controller/`, plantillas Thymeleaf propias, menú, tarjeta del panel y usuario de prueba. Sin clases `service`: los
controladores usan los repositorios; la lógica compartida vive en métodos públicos del controlador Rest que el Web reutiliza.

## 2. Modelo de datos
| Colección | Clase | Campos |
|---|---|---|
| `auditoria` | `model/RegistroAuditoria` | `id, tipo, usuarioId, username, rol, edicionId, categoriaId, canal, fecha` — **sin** nominado ni nota |
| `noticias` | `model/Noticia` | `id, titulo, resumen, contenido, edicionId, autorId, autorNombre, estado, fechaCreacion, fechaPublicacion` |

`tipo` ∈ `VOTO`, `CALIFICACION`, `INTENTO_DUPLICADO`. `canal` ∈ `WEB`, `API`, `SEMILLA`. `estado` ∈ `BORRADOR`, `PUBLICADA`.

## 3. Componentes
| Pieza | Archivo | Responsabilidad |
|---|---|---|
| Registrador | `config/Auditoria.java` | Utilidad estática (no es un service) que escribe el registro. Recibe usuario, edición y categoría; **no tiene parámetros para el nominado ni la nota**. Si falla, deja una advertencia y no interrumpe el voto. |
| Puntos de captura | `VotanteControladorWeb/Rest`, `JuradoControladorWeb/Rest` | Llaman al registrador tras guardar un voto o una calificación nueva, y ante un intento de segundo voto. |
| Auditor | `AuditorControladorWeb/Rest` | Filtros, resumen, anomalías, exportación CSV. Solo `GET`. |
| Periodista | `PeriodistaControladorWeb/Rest` | Crear borrador, editar, publicar (solo las propias). |
| Noticias públicas | `NoticiaControladorWeb/Rest` | Lista y detalle de noticias `PUBLICADA`; un borrador responde 404. |
| Roles pendientes | `RolProximamenteControladorWeb` | Punto de entrada protegido de Moderador, Representante y Soporte (Sprint 2). |

## 4. Rutas y seguridad
| Rol | Web | API | Regla |
|---|---|---|---|
| Auditor | `/auditor/registros`, `/auditor/anomalias`, `/auditor/exportar` | `/api/auditor/registros`, `/anomalias`, `/exportar` | `hasRole("AUDITOR")` |
| Periodista | `/periodista/noticias`, `…/guardar`, `…/{id}/publicar` | `/api/periodista/noticias` (GET, POST), `…/{id}` (PUT), `…/{id}/publicar` (PUT) | `hasRole("PERIODISTA")` |
| Público | `/publico/noticias`, `/publico/noticias/{id}` | `/api/publico/noticias`, `/{id}` | abierto (ya cubierto por `/publico/**` y `/api/publico/**`) |

El Administrador **no** accede a `/auditor/**` ni a `/periodista/**`: el 403 aplica a todos los demás roles (función exclusiva, C-03).

## 4. Flujos
**Voto →** validar (nominación aprobada, edición en votación, sin voto previo) → guardar `Voto` → `Auditoria.registrar(VOTO, …)`.
Si ya había voto: rechazar y registrar `INTENTO_DUPLICADO`.

**Noticia →** Periodista crea `BORRADOR` → edita → publica (`PUBLICADA` + fecha) → aparece en `/publico/noticias`.

## 5. Rendimiento y datos
- Consulta del Auditor limitada a 200 registros por defecto, máximo 1.000 (NFR-REN-001). La exportación trae todos los filtrados.
- Se filtra en memoria sobre las colecciones (volumen de un premio); si crece, añadir índices por `edicionId` y `fecha`.
- Datos personales mínimos: `usuarioId` y `username` (NFR-DAT-001).

## 6. Riesgos
| Riesgo | Mitigación |
|---|---|
| Que una futura edición del registro añada el nominado | Prueba `elRegistroNoTieneNingunCampoQueIdentifiqueAlNominadoNiLaNota` (reflexión sobre los campos) |
| Que se pueda leer el voto cruzando fechas del registro con la colección `votos` | Fuera del alcance de este sprint; se registra en ADR-003 como consecuencia conocida |
| Fallo del registro de auditoría durante un voto | El voto no se cancela; queda advertencia en el log |
