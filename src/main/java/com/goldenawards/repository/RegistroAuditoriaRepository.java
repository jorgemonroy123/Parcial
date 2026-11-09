package com.goldenawards.repository;

import com.goldenawards.model.RegistroAuditoria;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RegistroAuditoriaRepository extends MongoRepository<RegistroAuditoria, String> {
    List<RegistroAuditoria> findByEdicionId(String edicionId);
}
