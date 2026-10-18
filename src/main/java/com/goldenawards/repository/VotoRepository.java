package com.goldenawards.repository;

import com.goldenawards.model.Voto;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface VotoRepository extends MongoRepository<Voto, String> {
    List<Voto> findByVotanteId(String votanteId);
    List<Voto> findByEdicionId(String edicionId);
    boolean existsByVotanteIdAndCategoriaId(String votanteId, String categoriaId);
    boolean existsByNominacionId(String nominacionId);
    void deleteByCategoriaId(String categoriaId);
    void deleteByEdicionId(String edicionId);
}
