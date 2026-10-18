package com.goldenawards.repository;

import com.goldenawards.model.Parametro;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ParametroRepository extends MongoRepository<Parametro, String> {
    Optional<Parametro> findByClave(String clave);
}
