package com.aegisops.backend.repository;

import com.aegisops.backend.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ServiceEntityRepository extends JpaRepository<ServiceEntity, UUID> {
}
