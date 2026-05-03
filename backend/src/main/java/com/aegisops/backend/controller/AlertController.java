package com.aegisops.backend.controller;

import com.aegisops.backend.dto.AlertResponse;
import com.aegisops.backend.dto.CreateAlertRequest;
import com.aegisops.backend.dto.CreateAlertResponse;
import com.aegisops.backend.service.AlertService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateAlertResponse createAlert(@Valid @RequestBody CreateAlertRequest request) {
        return alertService.createAlert(request);
    }

    @GetMapping
    public List<AlertResponse> listAlerts() {
        return alertService.listAlerts();
    }
}
