package com.aegisops.backend.repository;

import com.aegisops.backend.entity.AIRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AIRecommendationRepository extends JpaRepository<AIRecommendation, UUID> {

    List<AIRecommendation> findByIncidentIdOrderByCreatedAtDesc(UUID incidentId);
}
