package com.aegisops.backend.repository;

import com.aegisops.backend.entity.Incident;
import com.aegisops.backend.enums.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    Optional<Incident> findFirstByDedupKeyAndStatusInOrderByStartedAtAsc(
            String dedupKey,
            Collection<IncidentStatus> statuses
    );

    List<Incident> findAllByOrderByStartedAtDesc();
}
