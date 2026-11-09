package com.goldenawards.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Registro de auditoria de la votacion (REQ-AUD-001, constitucion C-02).
 * Guarda QUIEN actuo, CUANDO y EN QUE categoria. Por diseno NO tiene ningun campo que identifique
 * al nominado elegido ni el puntaje: el voto sigue siendo secreto (NFR-AUD-001).
 */
@Document(collection = "auditoria")
public class RegistroAuditoria {

    @Id
    private String id;
    private String tipo;        // VOTO | CALIFICACION | INTENTO_DUPLICADO
    private String usuarioId;
    private String username;
    private String rol;
    private String edicionId;
    private String categoriaId;
    private String canal;       // WEB | API
    private LocalDateTime fecha;

    public RegistroAuditoria() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public String getEdicionId() { return edicionId; }
    public void setEdicionId(String edicionId) { this.edicionId = edicionId; }
    public String getCategoriaId() { return categoriaId; }
    public void setCategoriaId(String categoriaId) { this.categoriaId = categoriaId; }
    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
