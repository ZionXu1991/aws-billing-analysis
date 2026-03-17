package com.devops.billing.controller;

import com.devops.billing.dto.BillingDTO.*;
import com.devops.billing.service.BillingDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/billing/dashboard")
@RequiredArgsConstructor
@Tag(name = "Billing Dashboard", description = "AWS billing dashboard overview and analytics")
public class BillingDashboardController {

    private final BillingDashboardService billingDashboardService;

    @GetMapping("/overview")
    @Operation(summary = "Get billing overview")
    public ResponseEntity<OverviewResponse> getOverview() {
        return ResponseEntity.ok(billingDashboardService.getOverview());
    }

    @GetMapping("/trend")
    @Operation(summary = "Get cost trend over time")
    public ResponseEntity<TrendResponse> getTrend(
            @RequestParam(defaultValue = "monthly") String granularity,
            @RequestParam(defaultValue = "12") int months) {
        return ResponseEntity.ok(billingDashboardService.getTrend(granularity, months));
    }

    @GetMapping("/by-service")
    @Operation(summary = "Get costs grouped by AWS service")
    public ResponseEntity<ServiceCostResponse> getByService(
            @RequestParam String yearMonth,
            @RequestParam(defaultValue = "10") int topN) {
        return ResponseEntity.ok(billingDashboardService.getByService(yearMonth, topN));
    }

    @GetMapping("/by-account")
    @Operation(summary = "Get costs grouped by AWS account")
    public ResponseEntity<AccountCostResponse> getByAccount(
            @RequestParam String yearMonth,
            @RequestParam(required = false) String market) {
        return ResponseEntity.ok(billingDashboardService.getByAccount(yearMonth));
    }

    @GetMapping("/by-environment")
    @Operation(summary = "Get costs grouped by environment")
    public ResponseEntity<EnvironmentCostResponse> getByEnvironment(
            @RequestParam String yearMonth) {
        return ResponseEntity.ok(billingDashboardService.getByEnvironment(yearMonth));
    }
}
