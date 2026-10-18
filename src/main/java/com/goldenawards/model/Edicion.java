package com.goldenawards.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "ediciones")
public class Edicion {

    @Id
    private String id;
    private String nombre;
    private int anio;
    private String estado;
    private boolean resultadosPublicados;

    public Edicion() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public int getAnio() { return anio; }
    public void setAnio(int anio) { this.anio = anio; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public boolean isResultadosPublicados() { return resultadosPublicados; }
    public void setResultadosPublicados(boolean resultadosPublicados) { this.resultadosPublicados = resultadosPublicados; }
}
