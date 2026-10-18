package com.goldenawards.model;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "votos")
@CompoundIndex(def = "{'votanteId': 1, 'categoriaId': 1}", unique = true)
public class Voto {

    @Id
    private String id;
    private String edicionId;
    private String categoriaId;
    private String nominacionId;
    private String votanteId;
    private LocalDateTime fecha;

    public Voto() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEdicionId() { return edicionId; }
    public void setEdicionId(String edicionId) { this.edicionId = edicionId; }
    public String getCategoriaId() { return categoriaId; }
    public void setCategoriaId(String categoriaId) { this.categoriaId = categoriaId; }
    public String getNominacionId() { return nominacionId; }
    public void setNominacionId(String nominacionId) { this.nominacionId = nominacionId; }
    public String getVotanteId() { return votanteId; }
    public void setVotanteId(String votanteId) { this.votanteId = votanteId; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
