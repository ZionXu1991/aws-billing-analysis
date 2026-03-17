package com.devops.billing.controller;

import com.devops.billing.dto.BillingDTO.*;
import com.devops.billing.service.AnomalyDetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/billing/anomalies")
@RequiredArgsConstructor
@Tag(name = "Billing Anomalies", description = "Cost anomaly detection and management")
public class BillingAnomalyController {

    private final AnomalyDetectionService anomalyDetectionService;

    @GetMapping
    @Operation(summary = "List anomalies with pagination")
    public ResponseEntity<Page<AnomalyResponse>> getAnomalies(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(anomalyDetectionService.getAnomalies(pageable));
    }

    @PostMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge an anomaly")
    public ResponseEntity<AnomalyResponse> acknowledgeAnomaly(
            @PathVariable Long id,
            @RequestBody AnomalyAcknowledgeRequest request) {
        return ResponseEntity.ok(anomalyDetectionService.acknowledgeAnomaly(id, request.acknowledgedBy()));
    }
}
