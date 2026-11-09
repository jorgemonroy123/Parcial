# tasks.md — Grupo 1: Roles base (T005–T008) · Puerta 3

| | |
|---|---|
| Tarjeta | D12 · PBI-03 |
| Responsable | Juan Hernández |
| Revisa | Juan Ortega |
| Depende de | D09 (plan marco y ADR-002) |
| Trazabilidad | ADR-002 · REQ-ROL-001 · REQ-ROL-002 |
| Estado | Redactado · listo para revisión |

## Objetivo
Pasar de 5 a **10 roles que cuentan** (Administrador, Organizador, Votante, Nominado, Jurado, Auditor, Moderador,
Periodista, Representante del nominado y Soporte técnico), con acceso controlado **en el servidor** (403), menú por rol
y usuario de prueba por rol. El rol *Público* no cuenta; sigue existiendo como acceso abierto.

## Requisitos que cubre (EARS)
| ID | Requisito |
|---|---|
| REQ-ROL-001 | **Ubicuo:** el sistema deberá reconocer 10 roles y permitir su asignación. |
| REQ-ROL-002 | **Evento:** cuando un usuario accede a una ruta de otro rol, el sistema deberá devolver 403. |
| REQ-ROL-003 | **Ubicuo:** el sistema deberá tener un usuario de prueba por rol. |
| REQ-ROL-004 | **Estado:** mientras un usuario tenga un rol, el sistema deberá mostrar solo su menú. |

## Tareas
| ID | Tarea | Resp. | Revisa | Depende | Trazabilidad | Archivos |
|---|---|---|---|---|---|---|
| T005 | Cinco roles nuevos en la lista de roles y la seguridad | Juan Ortega | Juan Hernández | D12 | REQ-ROL-001, REQ-ROL-002, ADR-002, C-04 | `config/SecurityConfig.java`, `controller/AdminControladorWeb.java`, `controller/AdminControladorRest.java` |
| T006 | Usuarios de prueba: auditor, moderador, periodista, representante y soporte | Jorge Monroy | Juan Ortega | T005 | REQ-ROL-003, C-08 | `config/DataInitializer.java` |
| T007 | Menú por rol y tarjeta en el panel de cada rol | Juan Hernández | Jorge Monroy | T005 | REQ-ROL-004 | `templates/fragments/layout.html`, `controller/InicioControladorWeb.java`, `templates/login.html`, `templates/index.html` |
| T008 | Pruebas de acceso denegado (403) por rol | Jorge Monroy | Juan Hernández | T005 | REQ-ROL-002, NFR-SEG-001 | `src/test/java/com/goldenawards/AccesoPorRolTest.java` |

## Decisiones de implementación
1. **Una regla por rol en `SecurityConfig`:** `/jurado/**`, `/auditor/**`, `/periodista/**`, `/moderador/**`,
   `/representante/**` y `/soporte/**` (y su equivalente `/api/...`) exigen su rol con `hasRole(...)`. Un rol ajeno recibe 403 (C-04).
2. **Línea base del Jurado:** el proyecto traía el Jurado a medias (controladores y plantillas, pero sin regla de seguridad,
   sin estar en la lista de roles, sin menú, sin tarjeta y sin usuario de prueba). Se completó dentro de T005–T007 porque
   sin eso no hay 10 roles reales (C-10).
3. **Moderador, Representante y Soporte** tienen en este sprint solo un punto de entrada protegido
   (`RolProximamenteControladorWeb`: `/moderador/inicio`, `/representante/inicio`, `/soporte/inicio`) para que el menú, la tarjeta y la
   prueba de 403 tengan destino. Su módulo completo (par de controladores Web y Rest) llega en el Sprint 2.
4. **Datos de prueba aditivos:** `DataInitializer` agrega los usuarios que falten aunque la base ya tenga datos.

## Verificación (Puerta 3)
- [ ] `mvn test` en verde (incluye `AccesoPorRolTest`: cada ruta × cada rol → 403 si no es el propietario).
- [ ] Iniciar sesión con cada usuario de prueba y comprobar que solo ve su menú (REQ-ROL-004).
- [ ] Los 5 roles anteriores siguen funcionando igual (C-10).
- [ ] Revisión del diff por la persona indicada en «Revisa» y anotación en la bitácora de IA (C-09).
