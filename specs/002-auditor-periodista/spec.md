# Spec — Auditor y Periodista · v1.0 (Puerta 1)

| | |
|---|---|
| Tarjetas | D13 (v0.1 y clarificación) · D14 (v1.0) |
| PBI | PBI-04 (Auditor) · PBI-05 (Periodista) |
| Responsable | Juan Hernández |
| Aprueba | Jorge Monroy |
| Estado | v1.0 · pendiente de aprobación |

## 1. Contexto
El premio necesita (a) un rol que verifique la limpieza de la votación **sin romper el secreto del voto** y (b) un rol que
informe al público sobre cada edición. Ambos son roles nuevos con función exclusiva (C-03).

## 2. Alcance
**Dentro:** registro de auditoría de votos y calificaciones; consulta, anomalías y exportación para el Auditor;
noticias del Periodista y su página pública.
**Fuera (otros sprints):** tickets (Soporte), moderación de comentarios, propuestas del Representante.

## 3. Actores
| Rol | Qué puede hacer | Qué no puede |
|---|---|---|
| Auditor | Leer el registro, ver anomalías y exportar CSV | Crear, editar o borrar cualquier dato; ver por quién se votó |
| Periodista | Crear, editar y publicar **sus** noticias | Borrar noticias, tocar las de otro autor |
| Público (sin sesión) | Leer noticias **publicadas** | Ver borradores |

## 4. Requisitos (EARS)
### Auditor
| ID | Requisito |
|---|---|
| REQ-AUD-001 | **Evento:** cuando un votante emite un voto o un jurado registra una calificación, el sistema deberá guardar usuario, rol, fecha, edición, categoría y canal, **sin** el nominado elegido ni la nota. |
| REQ-AUD-002 | **Estado:** mientras el usuario tenga el rol Auditor, el sistema deberá permitirle consultar el registro filtrado por edición, categoría y tipo, en solo lectura. |
| REQ-AUD-003 | **Evento:** cuando el Auditor abre las anomalías, el sistema deberá mostrar los votos duplicados del mismo usuario en una categoría y los intentos de segundo voto rechazados. |
| REQ-AUD-004 | **Evento:** cuando el Auditor exporta, el sistema deberá generar un archivo CSV con los filtros aplicados. |

### Periodista
| ID | Requisito |
|---|---|
| REQ-PER-001 | **Evento:** cuando el Periodista publica una noticia, el sistema deberá mostrarla en la página pública de noticias. |
| REQ-PER-002 | **No deseado:** si una noticia está en borrador, entonces el sistema no deberá mostrarla en la página pública ni en la API pública. |
| REQ-PER-003 | **Ubicuo:** el sistema deberá guardar de cada noticia su autor, fecha de creación, fecha de publicación, estado y edición asociada, y solo el autor podrá editarla o publicarla. |

### No funcionales
| ID | Requisito |
|---|---|
| NFR-AUD-001 | El registro de auditoría deberá tener **0 campos** que identifiquen al nominado elegido (ni su nota). Verificable por prueba automática. |
| NFR-REN-001 | La consulta del Auditor deberá mostrar como máximo 200 registros por defecto (tope 1.000); el reporte completo se obtiene exportando. |
| NFR-SEG-001 | Toda ruta nueva deberá devolver 403 a cualquier rol que no sea su propietario (C-04). |
| NFR-DAT-001 | El registro guarda solo identificador y nombre de usuario (sin contraseñas ni datos personales adicionales) (C-07). |

## 5. Criterios de aceptación (Given / When / Then)
| ID | Criterio |
|---|---|
| AC-AUD-001.1 | **Dado** un votante, **cuando** emite un voto, **entonces** se crea un registro con su usuario, fecha y categoría y **sin** campo de nominado. |
| AC-AUD-002.1 | **Dado** un Votante, **cuando** intenta abrir `/auditor/registros`, **entonces** recibe 403. |
| AC-AUD-003.1 | **Dado** que un votante intenta votar dos veces en la misma categoría, **cuando** el auditor abre las anomalías, **entonces** ve un «intento duplicado» de ese usuario. |
| AC-AUD-004.1 | **Dado** un filtro por edición, **cuando** exporto, **entonces** descargo solo los registros de esa edición. |
| AC-PER-001.1 | **Dado** una noticia en borrador, **cuando** el periodista la publica, **entonces** es visible sin iniciar sesión. |
| AC-PER-002.1 | **Dado** una noticia en borrador, **cuando** un visitante abre su dirección pública, **entonces** recibe 404. |
| AC-PER-003.1 | **Dado** una noticia de otro autor, **cuando** un periodista intenta editarla, **entonces** el sistema lo rechaza. |

## 6. Clarificaciones (decisiones tomadas en la spec)
| # | Pregunta | Decisión |
|---|---|---|
| CL-01 | ¿Se registran las calificaciones del jurado? | Sí, **solo al crearlas**; corregir una nota existente no genera otro registro. |
| CL-02 | ¿Se guarda la nota o el nominado calificado? | No: igual que en el voto (C-02). |
| CL-03 | ¿Qué es una anomalía? | (a) más de un voto del mismo usuario en la misma categoría (`VOTO_DUPLICADO`, no debería ocurrir porque la base lo impide) y (b) un intento de segundo voto rechazado (`INTENTO_DUPLICADO`). |
| CL-04 | ¿Se registran los intentos rechazados? | Sí, con el tipo `INTENTO_DUPLICADO`. |
| CL-05 | ¿Qué formato tiene el reporte? | CSV con separador `;` y BOM UTF-8 para que Excel (configuración regional en español) lo abra bien. |
| CL-06 | ¿Pueden editarse noticias ya publicadas? | Sí, por su autor. No se pueden borrar ni despublicar en este sprint. |
| CL-07 | ¿Qué datos de votos antiguos aparecen? | Al iniciar, los votos y calificaciones que ya existían se copian al registro (canal `SEMILLA`), siempre sin nominado ni nota. |

## 7. Trazabilidad
Cada requisito se implementa en las tareas T009–T016 (ver `tasks.md`) y se verifica en `docs/sprint-1/trazabilidad.md`.
