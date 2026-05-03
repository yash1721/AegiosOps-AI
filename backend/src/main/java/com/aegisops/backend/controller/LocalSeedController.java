package com.aegisops.backend.controller;

import com.aegisops.backend.service.LocalSeedService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"local", "dev"})
@RequestMapping("/api/v1/dev")
public class LocalSeedController {

    private final LocalSeedService localSeedService;

    public LocalSeedController(LocalSeedService localSeedService) {
        this.localSeedService = localSeedService;
    }

    @PostMapping("/seed")
    public LocalSeedService.SeedResult seed() {
        return localSeedService.seed();
    }
}
