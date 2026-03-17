package com.devops.billing.scheduler;

import com.devops.billing.entity.DailyCost;
import com.devops.billing.entity.MonthlyCostSummary;
import com.devops.billing.repository.AccountMetadataRepository;
import com.devops.billing.repository.DailyCostRepository;
import com.devops.billing.repository.MonthlyCostSummaryRepository;
import com.devops.billing.service.AthenaQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "billing.ingestion.enabled", havingValue = "true", matchIfMissing = false)
public class CurIngestionScheduler {

    private final AthenaQueryService athenaQueryService;
    private final DailyCostRepository dailyCostRepository;
    private final MonthlyCostSummaryRepository monthlyCostSummaryRepository;
    private final AccountMetadataRepository accountMetadataRepository;

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void ingestDailyCosts() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        long startTime = System.currentTimeMillis();
        log.info("Starting daily cost ingestion for date: {}", yesterday);

        try {
            String query = buildDailyCostQuery(yesterday);
            List<Map<String, String>> results = athenaQueryService.executeQuery(query);

            List<DailyCost> dailyCosts = results.stream()
                    .map(this::parseDailyCostResult)
                    .collect(Collectors.toList());

            // Idempotent: delete existing data for this date before inserting
            dailyCostRepository.deleteByCostDate(yesterday);
            dailyCostRepository.saveAll(dailyCosts);

            updateMonthlySummaries(yesterday);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Daily cost ingestion completed for date: {}. Records: {}, Duration: {}ms",
                    yesterday, dailyCosts.size(), duration);
        } catch (Exception e) {
            log.error("Failed to ingest daily costs for date: {}", yesterday, e);
            throw e;
        }
    }

    void updateMonthlySummaries(LocalDate date) {
        String currentYearMonth = date.format(YEAR_MONTH_FORMATTER);
        YearMonth previousMonth = YearMonth.from(date).minusMonths(1);
        String previousYearMonth = previousMonth.format(YEAR_MONTH_FORMATTER);

        log.info("Updating monthly summaries for: {}", currentYearMonth);

        // Aggregate daily costs by account + service for the current month
        LocalDate monthStart = date.withDayOfMonth(1);
        LocalDate monthEnd = YearMonth.from(date).atEndOfMonth();
        List<DailyCost> monthlyCosts = dailyCostRepository.findByCostDateBetween(monthStart, monthEnd);

        Map<String, Map<String, BigDecimal>> aggregated = monthlyCosts.stream()
                .collect(Collectors.groupingBy(
                        DailyCost::getAccountId,
                        Collectors.groupingBy(
                                DailyCost::getServiceName,
                                Collectors.reducing(BigDecimal.ZERO, DailyCost::getUnblendedCost, BigDecimal::add)
                        )
                ));

        aggregated.forEach((accountId, serviceCosts) ->
                serviceCosts.forEach((serviceName, totalCost) -> {
                    // Look up previous month cost for MoM calculation
                    Optional<MonthlyCostSummary> previousSummary = monthlyCostSummaryRepository
                            .findByAccountIdAndYearMonthAndServiceName(accountId, previousYearMonth, serviceName);

                    BigDecimal previousMonthCost = previousSummary
                            .map(MonthlyCostSummary::getTotalCost)
                            .orElse(BigDecimal.ZERO);

                    BigDecimal momChangePercent = BigDecimal.ZERO;
                    if (previousMonthCost.compareTo(BigDecimal.ZERO) > 0) {
                        momChangePercent = totalCost.subtract(previousMonthCost)
                                .divide(previousMonthCost, 6, RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100"));
                    }

                    // Resolve environment and team from account metadata
                    var accountMetadata = accountMetadataRepository.findByAccountId(accountId);

                    // Upsert monthly cost summary
                    MonthlyCostSummary summary = monthlyCostSummaryRepository
                            .findByAccountIdAndYearMonthAndServiceName(accountId, currentYearMonth, serviceName)
                            .orElse(MonthlyCostSummary.builder()
                                    .accountId(accountId)
                                    .yearMonth(currentYearMonth)
                                    .serviceName(serviceName)
                                    .build());

                    summary.setTotalCost(totalCost);
                    summary.setPreviousMonthCost(previousMonthCost);
                    summary.setMomChangePercent(momChangePercent);
                    accountMetadata.ifPresent(meta -> {
                        summary.setEnvironment(meta.getEnvironment());
                        summary.setTeam(meta.getTeam());
                        summary.setMarket(meta.getMarket());
                    });

                    monthlyCostSummaryRepository.save(summary);
                })
        );

        log.info("Monthly summaries updated for: {}", currentYearMonth);
    }

    private String buildDailyCostQuery(LocalDate date) {
        return String.format(
                "SELECT line_item_usage_account_id, line_item_usage_start_date, " +
                "line_item_product_code, product_region_code, line_item_usage_type, " +
                "line_item_unblended_cost, line_item_blended_cost, line_item_usage_amount " +
                "FROM cur_table " +
                "WHERE line_item_usage_start_date = '%s'",
                date
        );
    }

    private DailyCost parseDailyCostResult(Map<String, String> row) {
        return DailyCost.builder()
                .accountId(row.get("line_item_usage_account_id"))
                .costDate(LocalDate.parse(row.get("line_item_usage_start_date")))
                .serviceName(row.get("line_item_product_code"))
                .region(row.getOrDefault("product_region_code", "global"))
                .usageType(row.getOrDefault("line_item_usage_type", "Unknown"))
                .unblendedCost(new BigDecimal(row.getOrDefault("line_item_unblended_cost", "0")))
                .blendedCost(new BigDecimal(row.getOrDefault("line_item_blended_cost", "0")))
                .usageQuantity(new BigDecimal(row.getOrDefault("line_item_usage_amount", "0")))
                .build();
    }
}
