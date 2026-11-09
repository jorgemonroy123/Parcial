package com.goldenawards.config;

import com.goldenawards.model.RegistroAuditoria;
import com.goldenawards.model.Usuario;
import com.goldenawards.repository.RegistroAuditoriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Registra la actividad de votacion para el Auditor (T009 y T010).
 * Clase utilitaria (no es un service): los controladores la llaman justo despues de guardar un voto
 * o una calificacion. NUNCA recibe el nominado elegido ni el puntaje, asi que no hay forma de guardarlos
 * (constitucion C-02 / NFR-AUD-001). Si el registro falla no se interrumpe el voto: solo se deja una advertencia.
 */
public final class Auditoria {

    public static final String VOTO = "VOTO";
    public static final String CALIFICACION = "CALIFICACION";
    public static final String INTENTO_DUPLICADO = "INTENTO_DUPLICADO";

    public static final String CANAL_WEB = "WEB";
    public static final String CANAL_API = "API";

    private static final Logger log = LoggerFactory.getLogger(Auditoria.class);

    private Auditoria() {}

    public static void registrar(RegistroAuditoriaRepository repositorio, String tipo, Usuario usuario,
                                 String edicionId, String categoriaId, String canal) {
        try {
            RegistroAuditoria r = new RegistroAuditoria();
            r.setTipo(tipo);
            r.setUsuarioId(usuario.getId());
            r.setUsername(usuario.getUsername());
            r.setRol(usuario.getRol());
            r.setEdicionId(edicionId);
            r.setCategoriaId(categoriaId);
            r.setCanal(canal);
            r.setFecha(LocalDateTime.now());
            repositorio.save(r);
        } catch (RuntimeException ex) {
            log.warn("No se pudo registrar la auditoria ({}): {}", tipo, ex.getMessage());
        }
    }
}
