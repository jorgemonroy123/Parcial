# ADR-003 — Auditoría de votación que preserva el secreto del voto

- **Estado:** Propuesto (Sprint 1) · pendiente de aceptación del equipo
- **Fecha:** Sprint 1
- **Trazabilidad:** NFR-AUD-001 · C-02 · REQ-AUD-001

## Contexto
El rol Auditor debe poder verificar la limpieza de la votación: quién votó, cuándo y en qué categoría, detectar duplicados y
exportar un reporte. Pero el voto es secreto: nadie, ni el Auditor, debe poder saber por quién votó una persona (C-02).

## Decisión
Se crea una colección propia, `auditoria`, que se escribe en cada voto y en cada calificación del jurado **sin** ninguna
referencia al nominado elegido ni a la nota. El registrador (`config/Auditoria.java`) ni siquiera recibe esos datos, de modo que no
pueden guardarse por descuido. El Auditor solo lee esa colección; nunca accede a `votos` ni a `puntajes`.

## Alternativas consideradas
| Alternativa | Por qué no |
|---|---|
| Dar al Auditor lectura de la colección `votos` | Revelaría por quién votó cada persona (viola C-02). |
| Guardar el nominado cifrado en el registro | Complejidad y riesgo de fuga de la clave; no aporta valor al auditor. |
| Reconstruir quién votó consultando `votos` | Acopla la auditoría al dato secreto. |

## Consecuencias
- (+) El secreto se garantiza por diseño y se verifica con una prueba automática (0 campos que identifiquen al nominado).
- (+) El registro también captura los intentos de voto duplicado rechazados.
- (−) Hay doble escritura (voto y registro); si falla el registro, el voto sigue válido y solo queda una advertencia en el log.
- (−) Alguien con acceso directo a la base de datos podría cruzar marcas de tiempo entre `auditoria` y `votos`; la protección es de
  acceso por rol y de diseño de la aplicación, no de cifrado de base de datos.
- Los votos ya existentes se copian al registro al iniciar la aplicación (canal `SEMILLA`), también sin nominado.
