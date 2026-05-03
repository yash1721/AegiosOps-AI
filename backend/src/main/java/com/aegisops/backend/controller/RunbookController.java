package com.aegisops.backend.controller;

import com.aegisops.backend.dto.CreateRunbookRequest;
import com.aegisops.backend.dto.RunbookResponse;
import com.aegisops.backend.service.RunbookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/runbooks")
public class RunbookController {

    private final RunbookService runbookService;

    public RunbookController(RunbookService runbookService) {
        this.runbookService = runbookService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RunbookResponse createRunbook(@Valid @RequestBody CreateRunbookRequest request) {
        return runbookService.createRunbook(request);
    }

    @GetMapping
    public List<RunbookResponse> listRunbooks() {
        return runbookService.listRunbooks();
    }

    @GetMapping("/{runbookId}")
    public RunbookResponse getRunbook(@PathVariable UUID runbookId) {
        return runbookService.getRunbook(runbookId);
    }
}
