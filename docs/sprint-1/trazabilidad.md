# Matriz de trazabilidad S1 y verificación de la DoD

| | |
|---|---|
| Tarjeta | D18 |
| Responsable | Juan Hernández |
| Participa | Equipo |
| Depende de | T016 |
| Formato | REQ → tarea → implementación → prueba |

## 1. Trazabilidad
| Requisito | Tarea | Implementación | Prueba |
|---|---|---|---|
| REQ-ROL-001 (reconocer 10 roles) | T005 | `SecurityConfig`, lista `ROLES` en `AdminControladorWeb/Rest` | Manual: crear un usuario con cada rol desde *Usuarios* |
| REQ-ROL-002 (403 a rutas de otro rol) | T005, T008 | `SecurityConfig` | `AccesoPorRolTest.unRolAjenoRecibe403` y `elRolPropietarioNoRecibe403` |
| REQ-ROL-003 (usuario de prueba por rol) | T006 | `DataInitializer.asegurarUsuariosDePrueba` | Manual: botones de acceso rápido en el login |
| REQ-ROL-004 (menú por rol) | T007 | `layout.html`, `InicioControladorWeb` (panel) | Manual: cada usuario ve solo su menú |
| REQ-ROL-005 (función exclusiva por rol) | D15 | `docs/matriz-permisos.md` | Revisión documental |
| REQ-AUD-001 | T009, T010 | `Auditoria`, `RegistroAuditoria`, controladores de voto y de jurado | `AuditoriaTest` (voto, calificación, corrección sin duplicar) |
| REQ-AUD-002 | T011 | `AuditorControladorWeb/Rest`, `auditor/registros.html` | `AccesoPorRolTest` (403) · manual de filtros |
| REQ-AUD-003 | T012 | `AuditorControladorRest.detectarAnomalias`, `auditor/anomalias.html` | `AuditoriaTest.seDetectanVotosDuplicadosEIntentosRechazados` · `unSegundoVotoSeRechazaYQuedaComoIntentoDuplicado` |
| REQ-AUD-004 | T012 | `AuditorControladorRest.exportar` | `AuditoriaTest.laExportacionPorEdicionTraeSoloLosRegistrosDeEsaEdicion` |
| REQ-PER-001 | T013, T014 | `Noticia`, `PeriodistaControladorWeb/Rest` | `NoticiasTest.unBorradorNoEsVisibleYAlPublicarloSi` |
| REQ-PER-002 | T014, T015 | `NoticiaControladorWeb/Rest` | `NoticiasTest` · `AccesoPorRolTest.unaNoticiaQueNoEstaPublicadaResponde404` |
| REQ-PER-003 | T013 | `Noticia`, `PeriodistaControladorRest.esAutor` | `NoticiasTest.laNoticiaGuardaAutorFechaYEdicion` y `soloElAutorPuedeEditarOPublicar` |
| NFR-AUD-001 | T010 | `RegistroAuditoria` (sin campos de nominado ni nota) | `AuditoriaTest.elRegistroNoTieneNingunCampo…` |
| NFR-REN-001 | T011 | `AuditorControladorRest.normalizarLimite` | `AuditoriaTest.elListadoTieneUnLimitePorConsulta` |
| NFR-SEG-001 | T008 | `SecurityConfig` | `AccesoPorRolTest` |
| NFR-DAT-001 | T009 | Registro con `usuarioId` y `username` solamente | Revisión del modelo |
| AC-AUD-001.1 | T016 | — | `AuditoriaTest.unVotoCreaUnRegistroConUsuarioFechaYCategoriaSinElNominado` |
| AC-AUD-004.1 | T016 | — | `AuditoriaTest.laExportacionPorEdicion…` |
| AC-PER-002.1 | T016 | — | `NoticiasTest.unBorradorNoEsVisibleYAlPublicarloSi` |

## 2. Verificación de la DoD
El documento de DoD del Sprint 0 no estaba entre los archivos revisados; este checklist usa los principios de la Constitución.
Marca cada punto cuando el equipo lo compruebe.

| Verificación | Principio | Estado |
|---|---|---|
| Ninguna tarea sin requisito (esta matriz) | C-06 | ☑ |
| El acceso por rol se aplica en el servidor y está probado | C-04, NFR-SEG-001 | ☐ ejecutar `mvn test` |
| El registro de auditoría no guarda el nominado | C-02, NFR-AUD-001 | ☐ ejecutar `mvn test` |
| Un voto por usuario por categoría sigue funcionando | C-01 | ☐ prueba de humo |
| Cada rol nuevo tiene par de controladores, menú, tarjeta y usuario de prueba | C-08 | ☑ Auditor y Periodista · ☐ los otros tres llegan en el Sprint 2 |
| Los 5 roles anteriores no se rompieron | C-10 | ☐ prueba de humo |
| Contraseñas cifradas y datos personales mínimos | C-07 | ☑ (BCrypt; registro con id y usuario) |
| Diff revisado por otra persona y anotado en la bitácora de IA | C-09 | ☐ ver `cierre.md` |
