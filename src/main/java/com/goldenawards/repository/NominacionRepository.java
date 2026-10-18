package com.goldenawards.repository;

import com.goldenawards.model.Nominacion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NominacionRepository extends MongoRepository<Nominacion, String> {
    List<Nominacion> findByEdicionId(String edicionId);
    List<Nominacion> findByEdicionIdAndEstado(String edicionId, String estado);
    List<Nominacion> findByCategoriaIdAndEstado(String categoriaId, String estado);
    List<Nominacion> findByEstado(String estado);
    List<Nominacion> findByJugadorId(String jugadorId);
    List<Nominacion> findByPropuestoPorId(String usuarioId);
    boolean existsByCategoriaIdAndJugadorId(String categoriaId, String jugadorId);
    void deleteByCategoriaId(String categoriaId);
    void deleteByEdicionId(String edicionId);
}
