package com.goldenawards.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "jugadores")
public class Jugador {

    @Id
    private String id;
    private String nombre;
    private String equipo;
    private String posicion;
    private String pais;
    private String biografia;
    private Integer goles;
    private Integer asistencias;
    private Integer partidos;
    private Integer trofeos;

    public Jugador() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEquipo() { return equipo; }
    public void setEquipo(String equipo) { this.equipo = equipo; }
    public String getPosicion() { return posicion; }
    public void setPosicion(String posicion) { this.posicion = posicion; }
    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }
    public String getBiografia() { return biografia; }
    public void setBiografia(String biografia) { this.biografia = biografia; }
    public Integer getGoles() { return goles; }
    public void setGoles(Integer goles) { this.goles = goles; }
    public Integer getAsistencias() { return asistencias; }
    public void setAsistencias(Integer asistencias) { this.asistencias = asistencias; }
    public Integer getPartidos() { return partidos; }
    public void setPartidos(Integer partidos) { this.partidos = partidos; }
    public Integer getTrofeos() { return trofeos; }
    public void setTrofeos(Integer trofeos) { this.trofeos = trofeos; }
}
