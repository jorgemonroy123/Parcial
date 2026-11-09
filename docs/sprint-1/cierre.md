# Cierre documental del Sprint 1: Review, Retro y acta

| | |
|---|---|
| Tarjeta | D19 |
| Responsable | Juan Hernández |
| Participa | Equipo |
| Depende de | D18 |

## 1. Meta del sprint
Dejar la base de 10 roles (seguridad por rol, menú, panel y usuarios de prueba) y entregar los dos primeros roles nuevos
completos: **Auditor** (PBI-04) y **Periodista** (PBI-05).

## 2. Resultado
| Entregable | Estado |
|---|---|
| T005–T008 · roles base, usuarios de prueba, menú/panel, pruebas de 403 | Implementado |
| T009–T012 · Auditor (registro sin nominado, solo lectura, anomalías, exportación) | Implementado |
| T013–T015 · Periodista y página pública de noticias | Implementado |
| T016 · pruebas de aceptación | Escritas; **pendiente ejecutarlas** con `mvn test` |
| D12–D19 · documentos | Redactados; pendientes de revisión/aprobación según cada tarjeta |

**Fuera del sprint (por diseño):** módulos completos de Moderador, Representante y Soporte (Sprint 2); en este sprint solo tienen
una entrada protegida con su menú y su tarjeta.

**Hallazgo:** el Jurado estaba incompleto en el código (sin regla de seguridad, sin menú, sin usuario de prueba). Se completó dentro de T005–T007.

## 3. Demostración (Review)
1. Iniciar sesión como cada uno de los 10 roles y comprobar su menú y su tarjeta de panel.
2. Como `votante`, votar; como `auditor`, ver el registro (sin nominado), filtrarlo y exportarlo en CSV.
3. Como `votante`, intentar un segundo voto en la misma categoría; como `auditor`, verlo en *Anomalías*.
4. Como `periodista`, crear un borrador, comprobar que `/publico/noticias` no lo muestra, publicarlo y verlo sin sesión.
5. Como `votante`, abrir `/auditor/registros` y comprobar el 403.

## 4. Retrospectiva (completar en la reunión)
| Qué salió bien | Qué mejorar | Acción para el Sprint 2 | Responsable |
|---|---|---|---|
| | | | |

## 5. Impedimentos y riesgos
| # | Descripción | Estado |
|---|---|---|
| 1 | Pruebas sin ejecutar todavía en el entorno del equipo | Abierto |
| 2 | Confirmar permisos reales de los 5 roles anteriores en la matriz | Abierto |

## 6. Bitácora de IA (C-09)
Todo el código y los documentos de este sprint fueron generados con ayuda de IA y deben ser revisados por una persona distinta de quien dirigió la IA.

| Tarjeta | Qué generó la IA | Revisor (según el tablero) | Revisado |
|---|---|---|---|
| T005 | Reglas de seguridad por rol y lista de roles | Juan Hernández | ☐ |
| T006 | Usuarios de prueba aditivos | Juan Ortega | ☐ |
| T007 | Menú, panel, login e inicio | Jorge Monroy | ☐ |
| T008 | `AccesoPorRolTest` | Juan Hernández | ☐ |
| T009 | Modelo, repositorio y registrador de auditoría | Jorge Monroy | ☐ |
| T010 | Captura en votos y calificaciones | Jorge Monroy | ☐ |
| T011 | Controladores y vista del Auditor | Juan Ortega | ☐ |
| T012 | Anomalías y exportación CSV | Juan Hernández | ☐ |
| T013 | Modelo y repositorio de noticias | Juan Hernández | ☐ |
| T014 | Controladores y vista del Periodista | Juan Ortega | ☐ |
| T015 | Página pública de noticias | Juan Ortega | ☐ |
| T016 | `AuditoriaTest`, `NoticiasTest` | Juan Hernández | ☐ |
| D12–D19 | Documentos SDD | según cada tarjeta | ☐ |

Quién dirigió a la IA en cada tarjeta: _completar_.

## 7. Acta
Fecha: ______ · Asistentes: ______________________ · Acuerdos: ______________________
