package com.goldenawards.config;

import com.goldenawards.model.Parametro;
import com.goldenawards.repository.ParametroRepository;

/**
 * Formula que combina el voto del publico con la calificacion del jurado.
 *
 *   puntajeFinal = %votos * (100 - peso)/100  +  promedioJurado * 10 * peso/100
 *
 * - %votos: porcentaje de los votos de la categoria que recibio el nominado (0 a 100).
 * - promedioJurado: promedio de las calificaciones del jurado (1 a 10) llevado a escala 0 a 100.
 * - peso: porcentaje del jurado, parametro global "peso_jurado" (por defecto 40; el Administrador lo cambia).
 * Si la categoria todavia no tiene calificaciones del jurado, el puntaje final es solo el % de votos.
 * (Clase utilitaria: no es un service.)
 */
public final class Ponderacion {

    public static final String CLAVE_PESO = "peso_jurado";
    public static final int PESO_POR_DEFECTO = 40;

    private Ponderacion() {}

    // Lee el peso del jurado (0 a 100) desde los parametros globales
    public static int pesoJurado(ParametroRepository parametroRepository) {
        int peso = parametroRepository.findByClave(CLAVE_PESO)
                .map(Parametro::getValor)
                .map(v -> {
                    try {
                        return Integer.parseInt(v.trim());
                    } catch (NumberFormatException ex) {
                        return PESO_POR_DEFECTO;
                    }
                })
                .orElse(PESO_POR_DEFECTO);
        return Math.max(0, Math.min(100, peso));
    }

    public static double puntajeFinal(double porcentajeVotos, double promedioJurado, int peso, boolean hayJurado) {
        if (!hayJurado) {
            return porcentajeVotos;
        }
        return porcentajeVotos * (100 - peso) / 100.0 + promedioJurado * 10 * peso / 100.0;
    }
}
