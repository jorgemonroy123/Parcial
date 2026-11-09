# tasks.md — Grupo 2: Auditor y Periodista (T009–T016) · Puerta 3

| | |
|---|---|
| Tarjeta | D17 |
| Responsable | Juan Hernández |
| Revisa | Juan Ortega |
| Depende de | D16 (plan.md y ADR-003) |
| Trazabilidad | C-06 (ninguna tarea sin requisito) |
| Estado | Tareas implementadas · pendientes de revisión y de `mvn test` |

## Tareas
| ID | PBI | Tarea | Resp. | Revisa | Depende | Trazabilidad | Archivos |
|---|---|---|---|---|---|---|---|
| T009 | PBI-04 | Colección y servicio de auditoría | Juan Ortega | Jorge Monroy | D17 | REQ-AUD-001, ADR-003 | `model/RegistroAuditoria.java`, `repository/RegistroAuditoriaRepository.java`, `config/Auditoria.java` |
| T010 | PBI-04 | Registrar cada voto y calificación sin el nominado elegido | Juan Ortega | Jorge Monroy | T009 | REQ-AUD-001, NFR-AUD-001, C-01, C-02 | `VotanteControladorWeb/Rest`, `JuradoControladorWeb/Rest`, `DataInitializer` (copia de votos existentes) |
| T011 | PBI-04 | Controladores Web y Rest del Auditor (solo lectura) | Juan Hernández | Juan Ortega | T009, T007 | REQ-AUD-002, NFR-REN-001, C-08 | `AuditorControladorWeb/Rest`, `templates/auditor/registros.html` |
| T012 | PBI-04 | Anomalías de voto duplicado y exportación del reporte | Jorge Monroy | Juan Hernández | T011 | REQ-AUD-003, REQ-AUD-004 | `AuditorControladorRest` (`detectarAnomalias`, `exportar`), `templates/auditor/anomalias.html` |
| T013 | PBI-05 | Colección y servicio de noticias | Jorge Monroy | Juan Hernández | D17 | REQ-PER-001, REQ-PER-003 | `model/Noticia.java`, `repository/NoticiaRepository.java` |
| T014 | PBI-05 | Controladores Web y Rest del Periodista | Juan Hernández | Juan Ortega | T013, T007 | REQ-PER-001, REQ-PER-002, C-08 | `PeriodistaControladorWeb/Rest`, `templates/periodista/noticias.html` |
| T015 | PBI-05 | Página pública de noticias | Jorge Monroy | Juan Ortega | T013 | REQ-PER-002 | `NoticiaControladorWeb/Rest`, `templates/publico/noticias.html`, `templates/publico/noticia.html` |
| T016 | PBI-04 | Pruebas de aceptación de Auditor y Periodista | Juan Ortega | Juan Hernández | T012, T014, T015 | AC-AUD-001.1, AC-AUD-004.1, AC-PER-002.1 | `src/test/java/com/goldenawards/AuditoriaTest.java`, `NoticiasTest.java` |

> «Servicio de auditoría / de noticias» se implementó **sin clases `service`**, según la regla del proyecto: el registrador es una
> utilidad estática (`config/Auditoria.java`) y la lógica de noticias vive en métodos públicos de `PeriodistaControladorRest` que el
> controlador web reutiliza.

## Orden y paralelismo
```
T009 ─► T010
T009 ─► T011 ─► T012 ─┐
T013 ─► T014 ─────────┼─► T016
T013 ─► T015 ─────────┘
```
`T008` y `T013` son paralelizables con el resto (etiqueta `[P]` en el tablero).

## Verificación (Puerta 3)
- [ ] `mvn test` en verde: `AuditoriaTest`, `NoticiasTest`, `AccesoPorRolTest`.
- [ ] Prueba manual: votar como `votante` y ver el registro como `auditor` (sin nominado en ninguna columna).
- [ ] Prueba manual: publicar una noticia como `periodista` y abrir `/publico/noticias` sin sesión.
- [ ] Revisión del diff contra el patrón del Jurado (C-08) y anotación en la bitácora de IA (C-09).
