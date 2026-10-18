package com.goldenawards.repository;

import com.goldenawards.model.Jugador;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface JugadorRepository extends MongoRepository<Jugador, String> {
}
