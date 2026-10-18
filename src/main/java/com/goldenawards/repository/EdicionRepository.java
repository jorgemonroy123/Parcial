package com.goldenawards.repository;

import com.goldenawards.model.Edicion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EdicionRepository extends MongoRepository<Edicion, String> {
    List<Edicion> findByEstadoIn(List<String> estados);
}
