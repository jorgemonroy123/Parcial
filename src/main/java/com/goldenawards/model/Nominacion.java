package com.goldenawards.model;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "nominaciones")
@CompoundIndex(def = "{'categoriaId': 1, 'jugadorId': 1}", unique = true)
public class Nominacion {

    @Id
    private String id;
    private String edicionId;
    private String categoriaId;
    private String jugadorId;
    private String propuestoPorId;
    private String estado;
    private LocalDateTime fecha;

    public Nominacion() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEdicionId() { return edicionId; }
    public void setEdicionId(String edicionId) { this.edicionId = edicionId; }
    public String getCategoriaId() { return categoriaId; }
    public void setCategoriaId(String categoriaId) { this.categoriaId = categoriaId; }
    public String getJugadorId() { return jugadorId; }
    public void setJugadorId(String jugadorId) { this.jugadorId = jugadorId; }
    public String getPropuestoPorId() { return propuestoPorId; }
    public void setPropuestoPorId(String propuestoPorId) { this.propuestoPorId = propuestoPorId; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
