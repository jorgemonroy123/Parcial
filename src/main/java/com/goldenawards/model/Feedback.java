package com.goldenawards.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "feedbacks")
public class Feedback {

    @Id
    private String id;
    private String edicionId;
    private String edicionNombre;
    private int estrellas; // 1-5
    private String comentario;
    private String autorNombre; // opcional / anónimo
    private LocalDateTime fecha;

    public Feedback() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEdicionId() { return edicionId; }
    public void setEdicionId(String edicionId) { this.edicionId = edicionId; }
    public String getEdicionNombre() { return edicionNombre; }
    public void setEdicionNombre(String edicionNombre) { this.edicionNombre = edicionNombre; }
    public int getEstrellas() { return estrellas; }
    public void setEstrellas(int estrellas) { this.estrellas = estrellas; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
    public String getAutorNombre() { return autorNombre; }
    public void setAutorNombre(String autorNombre) { this.autorNombre = autorNombre; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
