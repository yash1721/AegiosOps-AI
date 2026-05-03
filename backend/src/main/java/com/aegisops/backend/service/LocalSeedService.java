package com.aegisops.backend.service;

import com.aegisops.backend.dto.CreateRunbookRequest;
import com.aegisops.backend.entity.ServiceEntity;
import com.aegisops.backend.repository.RunbookRepository;
import com.aegisops.backend.repository.ServiceEntityRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile({"local", "dev"})
public class LocalSeedService {

    private final ServiceEntityRepository serviceEntityRepository;
    private final RunbookRepository runbookRepository;
    private final RunbookService runbookService;

    public LocalSeedService(
            ServiceEntityRepository serviceEntityRepository,
            RunbookRepository runbookRepository,
            RunbookService runbookService
    ) {
        this.serviceEntityRepository = serviceEntityRepository;
        this.runbookRepository = runbookRepository;
        this.runbookService = runbookService;
    }

    @Transactional
    public SeedResult seed() {
        int services = seedServices();
        int runbooks = seedRunbooks();
        return new SeedResult(services, runbooks);
    }

    private int seedServices() {
        int created = 0;
        created += createServiceIfMissing("payment-service", "payments", "tier-1");
        created += createServiceIfMissing("notification-service", "platform-messaging", "tier-2");
        created += createServiceIfMissing("order-service", "commerce", "tier-1");
        return created;
    }

    private int createServiceIfMissing(String name, String ownerTeam, String tier) {
        boolean exists = serviceEntityRepository.findAll()
                .stream()
                .anyMatch(service -> service.getName().equalsIgnoreCase(name));
        if (exists) {
            return 0;
        }
        ServiceEntity service = new ServiceEntity();
        service.setName(name);
        service.setOwnerTeam(ownerTeam);
        service.setTier(tier);
        serviceEntityRepository.save(service);
        return 1;
    }

    private int seedRunbooks() {
        int created = 0;
        created += createRunbookIfMissing(
                "payment-service",
                "Payment Service Latency Runbook",
                """
                        For p99_latency incidents on payment-service, first check recent deployments, gateway latency, database response time, and downstream processor health.
                        Compare current latency against the last known good window. If only payment-service is saturated, scale one replica at a time while watching error_rate.
                        If downstream processors are slow, stop retry storms and route traffic conservatively. Remediation requires human approval before rollback or traffic shifts.
                        """
        );
        created += createRunbookIfMissing(
                "payment-service",
                "Payment Error Rate Runbook",
                """
                        For payment-service error_rate incidents, inspect exception groups, dependency timeouts, payment gateway status, and configuration changes.
                        Roll back the latest risky deployment only after approval. If customer impact is high, disable noncritical payment features and preserve audit evidence.
                        """
        );
        created += createRunbookIfMissing(
                "notification-service",
                "Notification Queue Lag Runbook",
                """
                        For notification-service queue_lag incidents, check consumer throughput, queue depth, dead-letter volume, and downstream provider rate limits.
                        Scale consumers only when provider health is normal. Pause low-priority producers if lag continues to grow.
                        """
        );
        created += createRunbookIfMissing(
                "order-service",
                "Order DB Saturation Runbook",
                """
                        For order-service db_cpu incidents, inspect top queries, connection pool saturation, lock waits, and recent schema or traffic changes.
                        Consider disabling expensive background jobs and applying traffic shaping before database saturation causes cascading failures.
                        """
        );
        created += createRunbookIfMissing(
                "generic",
                "Generic Rollback Runbook",
                """
                        For incidents linked to recent deployments, identify the deployment window, compare metrics before and after release, and prepare rollback.
                        Rollback requires human approval. After rollback, monitor latency, error_rate, queue_lag, and customer impact until the incident is resolved.
                        """
        );
        return created;
    }

    private int createRunbookIfMissing(String serviceName, String title, String content) {
        boolean exists = runbookRepository.findAll()
                .stream()
                .anyMatch(runbook -> runbook.getTitle().equalsIgnoreCase(title));
        if (exists) {
            return 0;
        }
        runbookService.createRunbook(new CreateRunbookRequest(serviceName, title, content));
        return 1;
    }

    public record SeedResult(int servicesCreated, int runbooksCreated) {
    }
}
