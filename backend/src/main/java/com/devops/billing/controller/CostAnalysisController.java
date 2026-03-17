package com.devops.billing.controller;

import com.devops.billing.dto.BillingDTO.*;
import com.devops.billing.service.CostAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/billing/analysis")
@RequiredArgsConstructor
@Tag(name = "Cost Analysis", description = "Multi-dimensional cost analysis")
public class CostAnalysisController {

    private final CostAnalysisService costAnalysisService;

    @PostMapping("/query")
    @Operation(summary = "Run multi-dimensional cost analysis")
    public ResponseEntity<CostAnalysisResponse> analyze(@RequestBody CostAnalysisRequest request) {
        return ResponseEntity.ok(costAnalysisService.analyze(request));
    }

    @GetMapping("/filters")
    @Operation(summary = "Get available filter options")
    public ResponseEntity<FilterOptionsResponse> getFilterOptions() {
        return ResponseEntity.ok(costAnalysisService.getFilterOptions());
    }
}
