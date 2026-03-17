package com.devops.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class BillingDTO {

    // ── Overview ──────────────────────────────────────────────────────

    public record OverviewResponse(
            BigDecimal mtdTotalCost,
            BigDecimal previousMtdCost,
            BigDecimal momChangePercent,
            BigDecimal projectedMonthEndCost,
            long activeAnomalyCount,
            BigDecimal budgetUtilizationPercent
    ) {}

    // ── Trend ─────────────────────────────────────────────────────────

    public record TrendPoint(
            String period,
            BigDecimal devCost,
            BigDecimal preprodCost,
            BigDecimal prodCost,
            BigDecimal totalCost
    ) {}

    public record TrendResponse(
            List<TrendPoint> data,
            String granularity
    ) {}

    // ── Service Cost ──────────────────────────────────────────────────

    public record ServiceCostItem(
            String serviceName,
            BigDecimal cost,
            BigDecimal percentage
    ) {}

    public record ServiceCostResponse(
            List<ServiceCostItem> items,
            BigDecimal totalCost
    ) {}

    // ── Account Cost ──────────────────────────────────────────────────

    public record AccountCostItem(
            String accountId,
            String accountName,
            String team,
            String environment,
            String market,
            BigDecimal mtdCost,
            BigDecimal momChangePercent
    ) {}

    public record AccountCostResponse(
            List<AccountCostItem> items
    ) {}

    // ── Environment Cost ──────────────────────────────────────────────

    public record EnvironmentCostItem(
            String environment,
            BigDecimal cost,
            BigDecimal percentage,
            long accountCount
    ) {}

    public record EnvironmentCostResponse(
            List<EnvironmentCostItem> items
    ) {}

    // ── Daily Cost ────────────────────────────────────────────────────

    public record DailyCostItem(
            LocalDate date,
            String accountId,
            String serviceName,
            String region,
            BigDecimal cost
    ) {}

    public record DailyCostResponse(
            List<DailyCostItem> items,
            BigDecimal totalCost
    ) {}

    // ── Cost Comparison ───────────────────────────────────────────────

    public record CostComparisonItem(
            String period,
            BigDecimal currentCost,
            BigDecimal previousCost,
            BigDecimal changePercent
    ) {}

    public record CostComparisonResponse(
            List<CostComparisonItem> items,
            String comparisonType
    ) {}

    // ── Anomaly ───────────────────────────────────────────────────────

    public record AnomalyResponse(
            Long id,
            String accountId,
            String accountName,
            String serviceName,
            LocalDate detectedDate,
            String anomalyType,
            String severity,
            String status,
            BigDecimal expectedCost,
            BigDecimal actualCost,
            BigDecimal deviationPercent,
            String description,
            String acknowledgedBy,
            LocalDateTime acknowledgedAt,
            LocalDateTime createdAt
    ) {}

    public record AnomalyAcknowledgeRequest(
            String acknowledgedBy
    ) {}

    // ── Account ───────────────────────────────────────────────────────

    public record AccountRequest(
            String accountId,
            String accountName,
            String team,
            String environment,
            String market,
            String region
    ) {}

    public record AccountResponse(
            Long id,
            String accountId,
            String accountName,
            String team,
            String environment,
            String market,
            String region,
            Boolean active,
            LocalDateTime createdAt
    ) {}

    // ── Budget ────────────────────────────────────────────────────────

    public record BudgetRequest(
            String accountId,
            String serviceName,
            String market,
            String yearMonth,
            BigDecimal budgetAmount,
            BigDecimal alertThresholdPercent
    ) {}

    public record BudgetResponse(
            Long id,
            String accountId,
            String serviceName,
            String market,
            String yearMonth,
            BigDecimal budgetAmount,
            BigDecimal actualAmount,
            BigDecimal alertThresholdPercent,
            BigDecimal utilizationPercent,
            Boolean active
    ) {}

    // ── Market Cost ──────────────────────────────────────────────────

    public record MarketCostItem(
            String market,
            BigDecimal cost,
            BigDecimal percentage,
            BigDecimal momChangePercent,
            long accountCount
    ) {}

    public record MarketCostResponse(
            List<MarketCostItem> items,
            BigDecimal totalCost
    ) {}

    // ── Market x Environment Matrix ──────────────────────────────────

    public record MarketEnvironmentCostItem(
            String market,
            String environment,
            BigDecimal cost
    ) {}

    public record MarketEnvironmentCostResponse(
            List<MarketEnvironmentCostItem> items,
            List<String> markets,
            List<String> environments
    ) {}

    // ── Market Trend ─────────────────────────────────────────────────

    public record MarketTrendPoint(
            String yearMonth,
            String market,
            BigDecimal cost
    ) {}

    public record MarketTrendResponse(
            List<MarketTrendPoint> data,
            List<String> markets
    ) {}

    // ── Cost Drivers ─────────────────────────────────────────────────

    public record CostDriverItem(
            String market,
            String serviceName,
            BigDecimal cost,
            BigDecimal percentage,
            BigDecimal momChangePercent
    ) {}

    public record CostDriverResponse(
            List<CostDriverItem> items
    ) {}

    // ── Cost Movers ──────────────────────────────────────────────────

    public record CostMoverItem(
            String market,
            String serviceName,
            BigDecimal currentCost,
            BigDecimal previousCost,
            BigDecimal changeAmount,
            BigDecimal changePercent
    ) {}

    public record CostMoverResponse(
            List<CostMoverItem> items
    ) {}

    // ── Burn Rate ────────────────────────────────────────────────────

    public record BurnRateItem(
            LocalDate date,
            BigDecimal dailyCost,
            BigDecimal cumulativeCost,
            BigDecimal projectedMonthEnd
    ) {}

    public record BurnRateResponse(
            String market,
            String yearMonth,
            List<BurnRateItem> items,
            BigDecimal averageDailyBurn,
            BigDecimal projectedMonthEnd
    ) {}

    // ── Market Budget ────────────────────────────────────────────────

    public record MarketBudgetItem(
            String market,
            BigDecimal budgetAmount,
            BigDecimal actualAmount,
            BigDecimal utilizationPercent,
            BigDecimal remainingAmount
    ) {}

    public record MarketBudgetResponse(
            List<MarketBudgetItem> items
    ) {}

    // ── Cost Analysis ────────────────────────────────────────────────

    public record CostAnalysisRequest(
            List<String> markets,
            List<String> environments,
            List<String> services,
            List<String> accounts,
            String startYearMonth,
            String endYearMonth,
            String groupBy
    ) {}

    public record CostAnalysisItem(
            String group,
            BigDecimal cost,
            BigDecimal percentage,
            BigDecimal momChangePercent
    ) {}

    public record CostAnalysisResponse(
            List<CostAnalysisItem> items,
            BigDecimal totalCost,
            String groupBy
    ) {}

    // ── Filter Options ───────────────────────────────────────────────

    public record FilterOptionsResponse(
            List<String> markets,
            List<String> environments,
            List<String> services,
            List<String> accounts
    ) {}

    private BillingDTO() {
        // Utility class – prevent instantiation
    }
}
