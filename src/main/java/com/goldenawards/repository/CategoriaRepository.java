package com.goldenawards.repository;

import com.goldenawards.model.Categoria;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CategoriaRepository extends MongoRepository<Categoria, String> {
    List<Categoria> findByEdicionId(String edicionId);
    void deleteByEdicionId(String edicionId);
}
