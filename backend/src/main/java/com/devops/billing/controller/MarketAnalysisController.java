package com.devops.billing.controller;

import com.devops.billing.dto.BillingDTO.*;
import com.devops.billing.service.MarketAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/billing/markets")
@RequiredArgsConstructor
@Tag(name = "Market Analysis", description = "Cost analysis by market dimension")
public class MarketAnalysisController {

    private final MarketAnalysisService marketAnalysisService;

    @GetMapping("/costs")
    @Operation(summary = "Get cost breakdown by market")
    public ResponseEntity<MarketCostResponse> getByMarket(@RequestParam String yearMonth) {
        return ResponseEntity.ok(marketAnalysisService.getByMarket(yearMonth));
    }

    @GetMapping("/matrix")
    @Operation(summary = "Get market x environment heatmap data")
    public ResponseEntity<MarketEnvironmentCostResponse> getMarketEnvironmentMatrix(@RequestParam String yearMonth) {
        return ResponseEntity.ok(marketAnalysisService.getMarketEnvironmentMatrix(yearMonth));
    }

    @GetMapping("/trend")
    @Operation(summary = "Get market cost trend over time")
    public ResponseEntity<MarketTrendResponse> getMarketTrend(@RequestParam(defaultValue = "12") int months) {
        return ResponseEntity.ok(marketAnalysisService.getMarketTrend(months));
    }

    @GetMapping("/drivers")
    @Operation(summary = "Get top cost drivers by market and service")
    public ResponseEntity<CostDriverResponse> getTopCostDrivers(
            @RequestParam String yearMonth,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(marketAnalysisService.getTopCostDrivers(yearMonth, limit));
    }

    @GetMapping("/movers")
    @Operation(summary = "Get top cost movers (biggest MoM increases)")
    public ResponseEntity<CostMoverResponse> getTopCostMovers(
            @RequestParam String yearMonth,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(marketAnalysisService.getTopCostMovers(yearMonth, limit));
    }

    @GetMapping("/{market}/burn-rate")
    @Operation(summary = "Get daily burn rate for a market")
    public ResponseEntity<BurnRateResponse> getBurnRate(
            @PathVariable String market,
            @RequestParam String yearMonth) {
        return ResponseEntity.ok(marketAnalysisService.getBurnRate(yearMonth, market));
    }

    @GetMapping("/budget-vs-actual")
    @Operation(summary = "Get budget vs actual by market")
    public ResponseEntity<MarketBudgetResponse> getBudgetVsActual(@RequestParam String yearMonth) {
        return ResponseEntity.ok(marketAnalysisService.getBudgetVsActualByMarket(yearMonth));
    }

    @GetMapping("/list")
    @Operation(summary = "Get distinct market names")
    public ResponseEntity<List<String>> getMarketList() {
        return ResponseEntity.ok(marketAnalysisService.getDistinctMarkets());
    }
}
