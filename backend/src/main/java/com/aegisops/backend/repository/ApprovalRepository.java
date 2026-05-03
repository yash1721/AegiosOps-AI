package com.aegisops.backend.repository;

import com.aegisops.backend.entity.Approval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApprovalRepository extends JpaRepository<Approval, UUID> {

    List<Approval> findByIncidentIdOrderByCreatedAtDesc(UUID incidentId);
}
