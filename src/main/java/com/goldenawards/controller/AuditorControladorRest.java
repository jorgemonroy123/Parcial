package com.goldenawards.controller;

import com.goldenawards.config.Auditoria;
import com.goldenawards.model.Categoria;
import com.goldenawards.model.Edicion;
import com.goldenawards.model.RegistroAuditoria;
import com.goldenawards.repository.CategoriaRepository;
import com.goldenawards.repository.EdicionRepository;
import com.goldenawards.repository.RegistroAuditoriaRepository;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Rol AUDITOR (API, solo lectura): registro de actividad de votacion, anomalias y exportacion.
 * Trazabilidad: REQ-AUD-002, REQ-AUD-003, REQ-AUD-004, NFR-REN-001. El controlador web reutiliza estos metodos.
 * No hay ningun endpoint de escritura: el Auditor no puede crear, editar ni borrar nada.
 */
@RestController
@RequestMapping("/api/auditor")
public class AuditorControladorRest {

    public static final int LIMITE_POR_DEFECTO = 200;   // NFR-REN-001
    public static final int LIMITE_MAXIMO = 1000;

    private final RegistroAuditoriaRepository auditoriaRepository;
    private final EdicionRepository edicionRepository;
    private final CategoriaRepository categoriaRepository;

    public AuditorControladorRest(RegistroAuditoriaRepository auditoriaRepository,
                                  EdicionRepository edicionRepository, CategoriaRepository categoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
        this.edicionRepository = edicionRepository;
        this.categoriaRepository = categoriaRepository;
    }

    private boolean hay(String s) {
        return s != null && !s.isBlank();
    }

    private String fmt(LocalDateTime f) {
        if (f == null) {
            return "";
        }
        String s = f.toString().replace('T', ' ');
        return s.length() > 16 ? s.substring(0, 16) : s;
    }

    // ===================== FILTROS (REQ-AUD-002) =====================

    // Registros filtrados por edicion, categoria y tipo; los mas recientes primero
    public List<RegistroAuditoria> filtrar(String edicionId, String categoriaId, String tipo) {
        List<RegistroAuditoria> base = hay(edicionId)
                ? auditoriaRepository.findByEdicionId(edicionId)
                : auditoriaRepository.findAll();
        return base.stream()
                .filter(r -> !hay(categoriaId) || categoriaId.equals(r.getCategoriaId()))
                .filter(r -> !hay(tipo) || tipo.equals(r.getTipo()))
                .sorted(Comparator.comparing(RegistroAuditoria::getFecha,
                        Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder())).reversed())
                .collect(Collectors.toList());
    }

    // Convierte los registros en filas legibles (con nombres de edicion y categoria)
    public List<Map<String, Object>> filas(List<RegistroAuditoria> registros) {
        Map<String, String> ediciones = edicionRepository.findAll().stream()
                .collect(Collectors.toMap(Edicion::getId, Edicion::getNombre, (a, b) -> a));
        Map<String, String> categorias = categoriaRepository.findAll().stream()
                .collect(Collectors.toMap(Categoria::getId, Categoria::getNombre, (a, b) -> a));
        List<Map<String, Object>> filas = new ArrayList<>();
        for (RegistroAuditoria r : registros) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("fecha", fmt(r.getFecha()));
            f.put("tipo", r.getTipo());
            f.put("usuario", r.getUsername());
            f.put("rol", r.getRol());
            f.put("edicion", ediciones.getOrDefault(r.getEdicionId(), "(edicion eliminada)"));
            f.put("categoria", categorias.getOrDefault(r.getCategoriaId(), "(categoria eliminada)"));
            f.put("canal", r.getCanal());
            filas.add(f);
        }
        return filas;
    }

    // Resumen de lo que se esta viendo (conteos por tipo y usuarios distintos)
    public Map<String, Object> resumen(List<RegistroAuditoria> registros) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("votos", registros.stream().filter(r -> Auditoria.VOTO.equals(r.getTipo())).count());
        m.put("calificaciones", registros.stream().filter(r -> Auditoria.CALIFICACION.equals(r.getTipo())).count());
        m.put("intentosDuplicados", registros.stream().filter(r -> Auditoria.INTENTO_DUPLICADO.equals(r.getTipo())).count());
        m.put("usuariosDistintos", registros.stream().map(RegistroAuditoria::getUsuarioId).distinct().count());
        return m;
    }

    public int normalizarLimite(Integer limite) {
        if (limite == null || limite < 1) {
            return LIMITE_POR_DEFECTO;
        }
        return Math.min(limite, LIMITE_MAXIMO);
    }

    // Ejemplo: GET /api/auditor/registros?edicionId=...&categoriaId=...&tipo=VOTO&limite=100
    @GetMapping("/registros")
    public Map<String, Object> registros(@RequestParam(required = false) String edicionId,
                                         @RequestParam(required = false) String categoriaId,
                                         @RequestParam(required = false) String tipo,
                                         @RequestParam(required = false) Integer limite) {
        List<RegistroAuditoria> todos = filtrar(edicionId, categoriaId, tipo);
        int tope = normalizarLimite(limite);
        List<RegistroAuditoria> visibles = todos.stream().limit(tope).collect(Collectors.toList());
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("total", todos.size());
        respuesta.put("mostrados", visibles.size());
        respuesta.put("limite", tope);
        respuesta.put("resumen", resumen(todos));
        respuesta.put("registros", filas(visibles));
        return respuesta;
    }

    // ===================== ANOMALIAS (REQ-AUD-003) =====================

    /**
     * Anomalias de voto duplicado:
     *  - VOTO_DUPLICADO: mas de un voto registrado del mismo usuario en la misma categoria
     *    (no deberia ocurrir: la base de datos lo impide; si aparece, hay que investigarlo).
     *  - INTENTO_DUPLICADO: intentos de un segundo voto que el sistema rechazo.
     */
    public List<Map<String, Object>> detectarAnomalias(String edicionId) {
        List<RegistroAuditoria> base = filtrar(edicionId, null, null);
        Map<String, String> ediciones = edicionRepository.findAll().stream()
                .collect(Collectors.toMap(Edicion::getId, Edicion::getNombre, (a, b) -> a));
        Map<String, String> categorias = categoriaRepository.findAll().stream()
                .collect(Collectors.toMap(Categoria::getId, Categoria::getNombre, (a, b) -> a));

        List<Map<String, Object>> anomalias = new ArrayList<>();
        agregarAnomalias(anomalias, base, Auditoria.VOTO, "VOTO_DUPLICADO",
                "Mas de un voto del mismo usuario en la categoria", 2, ediciones, categorias);
        agregarAnomalias(anomalias, base, Auditoria.INTENTO_DUPLICADO, "INTENTO_DUPLICADO",
                "Intento de segundo voto rechazado por el sistema", 1, ediciones, categorias);
        anomalias.sort((a, b) -> Long.compare((Long) b.get("cantidad"), (Long) a.get("cantidad")));
        return anomalias;
    }

    private void agregarAnomalias(List<Map<String, Object>> destino, List<RegistroAuditoria> base,
                                  String tipoOrigen, String tipoAnomalia, String descripcion, int minimo,
                                  Map<String, String> ediciones, Map<String, String> categorias) {
        Map<String, List<RegistroAuditoria>> grupos = base.stream()
                .filter(r -> tipoOrigen.equals(r.getTipo()))
                .collect(Collectors.groupingBy(r -> r.getUsuarioId() + "|" + r.getEdicionId() + "|" + r.getCategoriaId()));
        for (List<RegistroAuditoria> grupo : grupos.values()) {
            if (grupo.size() < minimo) {
                continue;
            }
            RegistroAuditoria ejemplo = grupo.get(0);   // los registros vienen ordenados: el primero es el mas reciente
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("anomalia", tipoAnomalia);
            a.put("descripcion", descripcion);
            a.put("usuario", ejemplo.getUsername());
            a.put("edicion", ediciones.getOrDefault(ejemplo.getEdicionId(), "(edicion eliminada)"));
            a.put("categoria", categorias.getOrDefault(ejemplo.getCategoriaId(), "(categoria eliminada)"));
            a.put("cantidad", (long) grupo.size());
            a.put("ultimaFecha", fmt(ejemplo.getFecha()));
            destino.add(a);
        }
    }

    // Ejemplo: GET /api/auditor/anomalias?edicionId=...
    @GetMapping("/anomalias")
    public Map<String, Object> anomalias(@RequestParam(required = false) String edicionId) {
        List<Map<String, Object>> lista = detectarAnomalias(edicionId);
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("total", lista.size());
        respuesta.put("anomalias", lista);
        return respuesta;
    }

    // ===================== EXPORTACION (REQ-AUD-004) =====================

    private String celda(Object valor) {
        String s = valor == null ? "" : valor.toString();
        if (s.contains(";") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    // CSV con separador ";" (el que abre bien Excel en espanol) y los mismos filtros de la consulta
    public String generarCsv(List<RegistroAuditoria> registros) {
        StringBuilder sb = new StringBuilder("Fecha;Tipo;Usuario;Rol;Edicion;Categoria;Canal\n");
        for (Map<String, Object> f : filas(registros)) {
            sb.append(celda(f.get("fecha"))).append(';')
              .append(celda(f.get("tipo"))).append(';')
              .append(celda(f.get("usuario"))).append(';')
              .append(celda(f.get("rol"))).append(';')
              .append(celda(f.get("edicion"))).append(';')
              .append(celda(f.get("categoria"))).append(';')
              .append(celda(f.get("canal"))).append('\n');
        }
        return sb.toString();
    }

    // Ejemplo: GET /api/auditor/exportar?edicionId=...  (descarga el CSV con TODOS los registros filtrados)
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(@RequestParam(required = false) String edicionId,
                                           @RequestParam(required = false) String categoriaId,
                                           @RequestParam(required = false) String tipo) {
        String csv = "\uFEFF" + generarCsv(filtrar(edicionId, categoriaId, tipo));   // BOM para que Excel respete las tildes
        String sufijo = "todas";
        if (hay(edicionId)) {
            sufijo = edicionRepository.findById(edicionId).map(Edicion::getNombre).orElse("edicion")
                    .toLowerCase().replaceAll("[^a-z0-9]+", "-");
        }
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        cabeceras.setContentDisposition(ContentDisposition.attachment()
                .filename("auditoria-" + sufijo + ".csv").build());
        return ResponseEntity.ok().headers(cabeceras).body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
