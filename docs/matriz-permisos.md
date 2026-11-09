# Matriz de permisos v1.0 (10 roles × módulos)

| | |
|---|---|
| Tarjeta | D15 · PBI-03 |
| Responsable | Jorge Monroy |
| Revisa | Juan Hernández |
| Trazabilidad | REQ-ROL-005 · C-03 |
| Estado | v1.0 · pendiente de aprobación |

**Leyenda:** L leer · C crear · E editar · B borrar · A aprobar/ocultar · V votar o calificar · P publicar · X exportar ·
`—` sin acceso (el servidor responde 403).

## 1. Matriz
| Rol | Usuarios y roles | Catálogos | Ediciones y categorías | Nominaciones | Votación | Calificación del jurado | Resultados y rankings | Perfil del nominado | Comentarios de la encuesta | Noticias | Registro de auditoría | Tickets de soporte | Propuestas de cambio de perfil |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Administrador | CLEB | CLEB | L | L | — | — | L | CLEB | L | L | — | L | L |
| Organizador | — | L | CLEBA | CLEBA | — | — | L·P | L | L | L | — | C | LA |
| Votante | — | — | L | L | V | — | L | L | L | L | — | C | — |
| Nominado | — | — | L | L (las suyas) | — | — | L (los suyos) | L (el propio) | — | L | — | C | — |
| Jurado | — | — | L | L | — | V | L | L | — | L | — | C | — |
| **Auditor** | — | — | L | — | — | — | — | — | — | L (pública) | **L·X** | C | — |
| Moderador | — | — | L | L | — | — | L | L | LA | L | — | C | — |
| **Periodista** | — | — | L | L | — | — | L | L | — | **CLEP (las suyas)** | — | C | — |
| Representante | — | — | L | L | — | — | L | L (el suyo) | — | L | — | C | CL (solo su nominado) |
| Soporte técnico | — | — | — | — | — | — | — | — | — | L | — | LE (atiende) | — |

Público (no cuenta como rol): lee ediciones, nominaciones, resultados publicados y noticias; puede comentar la encuesta.

## 2. Cambios respecto a la v0.1
| Celda | v0.1 | v1.0 | Motivo |
|---|---|---|---|
| Auditor · Noticias | — | L (pública) | La página `/publico/noticias` es abierta a cualquiera, con o sin sesión; no tiene sentido bloquearla a un rol. |
| Auditor · Registro de auditoría | L·X | L·X | Confirmado: solo lectura y exportación. No existe ningún `POST/PUT/DELETE`. |
| Periodista · Noticias | CLEP | CLEP (sin B) | Confirmado: no hay endpoint para borrar noticias; solo el autor edita o publica las suyas. |

Los permisos de los 5 roles anteriores siguen siendo los inferidos en la v0.1; deben confirmarse contra el comportamiento real.

## 3. Función exclusiva por rol (C-03) y cómo se verifica
| Rol | Función que ningún otro rol hace | Evidencia (ruta) | Estado |
|---|---|---|---|
| Administrador | Asignar roles y gestionar usuarios, jugadores y parámetros | `/admin/**` | Sprint 0 |
| Organizador | Abrir/cerrar periodos, aprobar nominaciones, publicar resultados | `/organizador/**` | Sprint 0 |
| Votante | Emitir el voto único por categoría | `/votante/votar` | Sprint 0 |
| Nominado | Ver su propio perfil y resultados | `/nominado/perfil` | Sprint 0 |
| Jurado | Calificar de 1 a 10 a los nominados | `/jurado/evaluar` | Completado en Sprint 1 (seguridad, menú, usuario) |
| Auditor | Leer el registro de actividad sin ver por quién se votó, ver anomalías y exportar | `/auditor/**` | **Sprint 1** |
| Periodista | Redactar y publicar noticias | `/periodista/**` | **Sprint 1** |
| Moderador | Aprobar u ocultar comentarios de la encuesta | `/moderador/**` | Entrada protegida en Sprint 1; módulo en Sprint 2 |
| Representante | Proponer cambios al perfil de su nominado | `/representante/**` | Entrada protegida en Sprint 1; módulo en Sprint 2 |
| Soporte técnico | Atender tickets y cambiar su estado | `/soporte/**` | Entrada protegida en Sprint 1; módulo en Sprint 2 |

Verificación automática de los 403: `AccesoPorRolTest` (cada ruta × cada rol que no es el propietario).
