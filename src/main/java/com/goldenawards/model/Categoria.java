package com.goldenawards.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "categorias")
public class Categoria {

    @Id
    private String id;
    private String edicionId;
    private String nombre;
    private String descripcion;

    public Categoria() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEdicionId() { return edicionId; }
    public void setEdicionId(String edicionId) { this.edicionId = edicionId; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
