package com.aegisops.backend.repository;

import com.aegisops.backend.entity.Runbook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RunbookRepository extends JpaRepository<Runbook, UUID> {

    List<Runbook> findAllByOrderByCreatedAtDesc();
}
