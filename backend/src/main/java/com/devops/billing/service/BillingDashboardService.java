package com.devops.billing.service;

import com.devops.billing.dto.BillingDTO.*;
import com.devops.billing.entity.AccountMetadata;
import com.devops.billing.entity.MonthlyCostSummary;
import com.devops.billing.enums.AnomalyStatus;
import com.devops.billing.enums.Environment;
import com.devops.billing.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BillingDashboardService {

    private final DailyCostRepository dailyCostRepository;
    private final MonthlyCostSummaryRepository monthlyCostSummaryRepository;
    private final AccountMetadataRepository accountMetadataRepository;
    private final CostAnomalyRepository costAnomalyRepository;
    private final BudgetRepository budgetRepository;

    private static final DateTimeFormatter YEAR_MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Cacheable("billing-overview")
    public OverviewResponse getOverview() {
        log.info("Calculating billing overview");

        LocalDate today = LocalDate.now();
        LocalDate firstOfMonth = today.withDayOfMonth(1);

        // MTD total cost
        BigDecimal mtdTotalCost = Optional.ofNullable(
                dailyCostRepository.sumCostByDateRange(firstOfMonth, today)
        ).orElse(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        // Previous month same period
        LocalDate prevMonthStart = firstOfMonth.minusMonths(1);
        LocalDate prevMonthSameDay = today.minusMonths(1);
        BigDecimal previousMtdCost = Optional.ofNullable(
                dailyCostRepository.sumCostByDateRange(prevMonthStart, prevMonthSameDay)
        ).orElse(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        // MoM change percent
        BigDecimal momChangePercent = BigDecimal.ZERO;
        if (previousMtdCost.compareTo(BigDecimal.ZERO) != 0) {
            momChangePercent = mtdTotalCost.subtract(previousMtdCost)
                    .divide(previousMtdCost, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Projected month-end cost
        int daysElapsed = today.getDayOfMonth();
        int daysInMonth = today.lengthOfMonth();
        BigDecimal projectedMonthEndCost = BigDecimal.ZERO;
        if (daysElapsed > 0) {
            projectedMonthEndCost = mtdTotalCost
                    .divide(BigDecimal.valueOf(daysElapsed), 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(daysInMonth))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Active anomaly count
        long activeAnomalyCount = costAnomalyRepository.countByStatus(AnomalyStatus.OPEN);

        // Budget utilization
        String currentYearMonth = today.format(YEAR_MONTH_FMT);
        BigDecimal budgetUtilizationPercent = calculateBudgetUtilization(currentYearMonth, mtdTotalCost);

        return new OverviewResponse(
                mtdTotalCost, previousMtdCost, momChangePercent,
                projectedMonthEndCost, activeAnomalyCount, budgetUtilizationPercent
        );
    }

    @Cacheable("billing-trend")
    public TrendResponse getTrend(String granularity, int months) {
        log.info("Calculating billing trend: granularity={}, months={}", granularity, months);

        YearMonth currentMonth = YearMonth.now();
        YearMonth startMonth = currentMonth.minusMonths(months - 1);

        String startYearMonth = startMonth.format(YEAR_MONTH_FMT);
        String endYearMonth = currentMonth.format(YEAR_MONTH_FMT);

        // Build account-to-environment mapping
        Map<String, Environment> accountEnvMap = accountMetadataRepository.findByActiveTrue()
                .stream()
                .collect(Collectors.toMap(AccountMetadata::getAccountId, AccountMetadata::getEnvironment, (a, b) -> a));

        // Fetch monthly summaries
        List<Object[]> monthlyTrend = monthlyCostSummaryRepository.findMonthlyTrend(startYearMonth, endYearMonth);

        // For environment breakdown, fetch all summaries in range
        List<MonthlyCostSummary> allSummaries = new ArrayList<>();
        YearMonth ym = startMonth;
        while (!ym.isAfter(currentMonth)) {
            allSummaries.addAll(monthlyCostSummaryRepository.findByYearMonth(ym.format(YEAR_MONTH_FMT)));
            ym = ym.plusMonths(1);
        }

        // Group by period and environment
        Map<String, Map<Environment, BigDecimal>> periodEnvCosts = new LinkedHashMap<>();
        for (MonthlyCostSummary summary : allSummaries) {
            Environment env = summary.getEnvironment();
            if (env == null) {
                env = accountEnvMap.getOrDefault(summary.getAccountId(), Environment.DEV);
            }
            periodEnvCosts
                    .computeIfAbsent(summary.getYearMonth(), k -> new EnumMap<>(Environment.class))
                    .merge(env, summary.getTotalCost(), BigDecimal::add);
        }

        List<TrendPoint> data = new ArrayList<>();
        for (Map.Entry<String, Map<Environment, BigDecimal>> entry : periodEnvCosts.entrySet()) {
            Map<Environment, BigDecimal> envCosts = entry.getValue();
            BigDecimal devCost = envCosts.getOrDefault(Environment.DEV, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal preprodCost = envCosts.getOrDefault(Environment.PREPROD, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal prodCost = envCosts.getOrDefault(Environment.PROD, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalCost = devCost.add(preprodCost).add(prodCost).setScale(2, RoundingMode.HALF_UP);
            data.add(new TrendPoint(entry.getKey(), devCost, preprodCost, prodCost, totalCost));
        }

        return new TrendResponse(data, granularity);
    }

    @Cacheable("billing-by-service")
    public ServiceCostResponse getByService(String yearMonth, int topN) {
        log.info("Getting cost by service: yearMonth={}, topN={}", yearMonth, topN);

        YearMonth ym = YearMonth.parse(yearMonth, YEAR_MONTH_FMT);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        List<Object[]> serviceCosts = dailyCostRepository.findTopServicesByCost(startDate, endDate, PageRequest.of(0, topN));
        BigDecimal totalCost = Optional.ofNullable(
                dailyCostRepository.sumCostByDateRange(startDate, endDate)
        ).orElse(BigDecimal.ZERO);

        List<ServiceCostItem> items = new ArrayList<>();
        for (Object[] row : serviceCosts) {
            String serviceName = (String) row[0];
            BigDecimal cost = ((BigDecimal) row[1]).setScale(2, RoundingMode.HALF_UP);
            BigDecimal percentage = BigDecimal.ZERO;
            if (totalCost.compareTo(BigDecimal.ZERO) != 0) {
                percentage = cost.divide(totalCost, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }
            items.add(new ServiceCostItem(serviceName, cost, percentage));
        }

        return new ServiceCostResponse(items, totalCost.setScale(2, RoundingMode.HALF_UP));
    }

    @Cacheable("billing-by-account")
    public AccountCostResponse getByAccount(String yearMonth) {
        log.info("Getting cost by account: yearMonth={}", yearMonth);

        YearMonth ym = YearMonth.parse(yearMonth, YEAR_MONTH_FMT);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        // Previous month for MoM
        YearMonth prevYm = ym.minusMonths(1);
        LocalDate prevStartDate = prevYm.atDay(1);
        LocalDate prevEndDate = prevYm.atEndOfMonth();

        List<Object[]> accountCosts = dailyCostRepository.findDailyCostGroupedByAccount(startDate, endDate);
        List<Object[]> prevAccountCosts = dailyCostRepository.findDailyCostGroupedByAccount(prevStartDate, prevEndDate);

        Map<String, BigDecimal> prevCostMap = new HashMap<>();
        for (Object[] row : prevAccountCosts) {
            prevCostMap.put((String) row[0], (BigDecimal) row[1]);
        }

        Map<String, AccountMetadata> metadataMap = accountMetadataRepository.findByActiveTrue()
                .stream()
                .collect(Collectors.toMap(AccountMetadata::getAccountId, a -> a, (a, b) -> a));

        List<AccountCostItem> items = new ArrayList<>();
        for (Object[] row : accountCosts) {
            String accountId = (String) row[0];
            BigDecimal mtdCost = ((BigDecimal) row[1]).setScale(2, RoundingMode.HALF_UP);
            BigDecimal prevCost = prevCostMap.getOrDefault(accountId, BigDecimal.ZERO);

            BigDecimal momChange = BigDecimal.ZERO;
            if (prevCost.compareTo(BigDecimal.ZERO) != 0) {
                momChange = mtdCost.subtract(prevCost)
                        .divide(prevCost, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            AccountMetadata metadata = metadataMap.get(accountId);
            String accountName = metadata != null ? metadata.getAccountName() : accountId;
            String team = metadata != null ? metadata.getTeam() : "Unknown";
            String environment = metadata != null ? metadata.getEnvironment().name() : "Unknown";
            String market = metadata != null ? metadata.getMarket() : "UNKNOWN";

            items.add(new AccountCostItem(accountId, accountName, team, environment, market, mtdCost, momChange));
        }

        return new AccountCostResponse(items);
    }

    @Cacheable("billing-by-env")
    public EnvironmentCostResponse getByEnvironment(String yearMonth) {
        log.info("Getting cost by environment: yearMonth={}", yearMonth);

        List<Object[]> envCosts = monthlyCostSummaryRepository.findMonthlyCostByEnvironment(yearMonth);

        BigDecimal totalCost = envCosts.stream()
                .map(row -> (BigDecimal) row[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Count accounts per environment
        Map<Environment, Long> envAccountCounts = accountMetadataRepository.findByActiveTrue()
                .stream()
                .collect(Collectors.groupingBy(AccountMetadata::getEnvironment, Collectors.counting()));

        List<EnvironmentCostItem> items = new ArrayList<>();
        for (Object[] row : envCosts) {
            Environment env = (Environment) row[0];
            BigDecimal cost = ((BigDecimal) row[1]).setScale(2, RoundingMode.HALF_UP);
            BigDecimal percentage = BigDecimal.ZERO;
            if (totalCost.compareTo(BigDecimal.ZERO) != 0) {
                percentage = cost.divide(totalCost, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }
            long accountCount = envAccountCounts.getOrDefault(env, 0L);
            items.add(new EnvironmentCostItem(env.name(), cost, percentage, accountCount));
        }

        return new EnvironmentCostResponse(items);
    }

    private BigDecimal calculateBudgetUtilization(String yearMonth, BigDecimal mtdTotalCost) {
        var budgets = budgetRepository.findByYearMonthAndActiveTrue(yearMonth);
        if (budgets.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalBudget = budgets.stream()
                .map(b -> b.getBudgetAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalBudget.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return mtdTotalCost.divide(totalBudget, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
