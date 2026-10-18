package com.goldenawards.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

// Calificacion (1 a 10) que un miembro del jurado le da a un nominado
@Document(collection = "puntajes")
@CompoundIndex(def = "{'juradoId': 1, 'nominacionId': 1}", unique = true)
public class Puntaje {

    @Id
    private String id;
    private String edicionId;
    private String categoriaId;
    private String nominacionId;
    private String juradoId;
    private int puntaje;
    private String comentario;
    private LocalDateTime fecha;

    public Puntaje() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEdicionId() { return edicionId; }
    public void setEdicionId(String edicionId) { this.edicionId = edicionId; }
    public String getCategoriaId() { return categoriaId; }
    public void setCategoriaId(String categoriaId) { this.categoriaId = categoriaId; }
    public String getNominacionId() { return nominacionId; }
    public void setNominacionId(String nominacionId) { this.nominacionId = nominacionId; }
    public String getJuradoId() { return juradoId; }
    public void setJuradoId(String juradoId) { this.juradoId = juradoId; }
    public int getPuntaje() { return puntaje; }
    public void setPuntaje(int puntaje) { this.puntaje = puntaje; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
