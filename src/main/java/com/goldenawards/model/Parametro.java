package com.goldenawards.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "parametros")
public class Parametro {

    @Id
    private String id;
    @Indexed(unique = true)
    private String clave;
    private String valor;
    private String descripcion;

    public Parametro() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
