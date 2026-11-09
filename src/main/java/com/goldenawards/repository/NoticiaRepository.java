package com.goldenawards.repository;

import com.goldenawards.model.Noticia;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NoticiaRepository extends MongoRepository<Noticia, String> {
    List<Noticia> findByEstado(String estado);
    List<Noticia> findByAutorId(String autorId);
}
