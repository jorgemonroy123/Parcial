package com.goldenawards.repository;

import com.goldenawards.model.Feedback;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FeedbackRepository extends MongoRepository<Feedback, String> {
    List<Feedback> findByEdicionIdOrderByFechaDesc(String edicionId);
    List<Feedback> findAllByOrderByFechaDesc();
}
