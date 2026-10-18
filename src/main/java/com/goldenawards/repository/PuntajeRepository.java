package com.goldenawards.repository;

import com.goldenawards.model.Puntaje;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PuntajeRepository extends MongoRepository<Puntaje, String> {
    List<Puntaje> findByJuradoId(String juradoId);
    List<Puntaje> findByEdicionId(String edicionId);
    Optional<Puntaje> findByJuradoIdAndNominacionId(String juradoId, String nominacionId);
    boolean existsByNominacionId(String nominacionId);
    void deleteByCategoriaId(String categoriaId);
    void deleteByEdicionId(String edicionId);
}
