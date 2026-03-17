package com.devops.billing.controller;

import com.devops.billing.dto.BillingDTO.*;
import com.devops.billing.service.CostQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1/billing/costs")
@RequiredArgsConstructor
@Tag(name = "Billing Costs", description = "Detailed cost queries and comparisons")
public class BillingCostController {

    private final CostQueryService costQueryService;

    @GetMapping("/daily")
    @Operation(summary = "Get daily cost details")
    public ResponseEntity<DailyCostResponse> getDailyCosts(
            @RequestParam(required = false) String accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(costQueryService.getDailyCosts(accountId, startDate, endDate));
    }

    @GetMapping("/comparison")
    @Operation(summary = "Get cost comparison (MoM or YoY)")
    public ResponseEntity<CostComparisonResponse> getComparison(
            @RequestParam(required = false) String accountId,
            @RequestParam String currentMonth,
            @RequestParam(defaultValue = "mom") String comparisonType) {
        return ResponseEntity.ok(costQueryService.getComparison(accountId, currentMonth, comparisonType));
    }
}
