package com.aegisops.backend.repository;

import com.aegisops.backend.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    List<Alert> findAllByOrderByCreatedAtDesc();

    List<Alert> findByIncidentIdOrderByCreatedAtDesc(UUID incidentId);
}
