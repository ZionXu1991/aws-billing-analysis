package com.devops.billing.service;

import com.devops.billing.dto.BillingDTO.*;
import com.devops.billing.entity.AccountMetadata;
import com.devops.billing.entity.MonthlyCostSummary;
import com.devops.billing.enums.Environment;
import com.devops.billing.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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
public class MarketAnalysisService {

    private final MonthlyCostSummaryRepository monthlyCostSummaryRepository;
    private final AccountMetadataRepository accountMetadataRepository;
    private final DailyCostRepository dailyCostRepository;
    private final BudgetRepository budgetRepository;

    private static final DateTimeFormatter YEAR_MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Cacheable("billing-market-costs")
    public MarketCostResponse getByMarket(String yearMonth) {
        log.info("Getting cost by market: yearMonth={}", yearMonth);

        List<Object[]> marketCosts = monthlyCostSummaryRepository.findMonthlyCostByMarket(yearMonth);

        // Previous month for MoM
        YearMonth ym = YearMonth.parse(yearMonth, YEAR_MONTH_FMT);
        String prevYearMonth = ym.minusMonths(1).format(YEAR_MONTH_FMT);
        Map<String, BigDecimal> prevCostMap = new HashMap<>();
        for (Object[] row : monthlyCostSummaryRepository.findMonthlyCostByMarket(prevYearMonth)) {
            prevCostMap.put((String) row[0], (BigDecimal) row[1]);
        }

        // Account counts per market
        Map<String, Long> marketAccountCounts = accountMetadataRepository.findByActiveTrue()
                .stream()
                .collect(Collectors.groupingBy(AccountMetadata::getMarket, Collectors.counting()));

        BigDecimal totalCost = marketCosts.stream()
                .map(row -> (BigDecimal) row[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<MarketCostItem> items = new ArrayList<>();
        for (Object[] row : marketCosts) {
            String market = (String) row[0];
            BigDecimal cost = ((BigDecimal) row[1]).setScale(2, RoundingMode.HALF_UP);
            BigDecimal percentage = BigDecimal.ZERO;
            if (totalCost.compareTo(BigDecimal.ZERO) != 0) {
                percentage = cost.divide(totalCost, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }
            BigDecimal prevCost = prevCostMap.getOrDefault(market, BigDecimal.ZERO);
            BigDecimal momChange = BigDecimal.ZERO;
            if (prevCost.compareTo(BigDecimal.ZERO) != 0) {
                momChange = cost.subtract(prevCost)
                        .divide(prevCost, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }
            long accountCount = marketAccountCounts.getOrDefault(market, 0L);
            items.add(new MarketCostItem(market, cost, percentage, momChange, accountCount));
        }

        return new MarketCostResponse(items, totalCost.setScale(2, RoundingMode.HALF_UP));
    }

    @Cacheable("billing-market-matrix")
    public MarketEnvironmentCostResponse getMarketEnvironmentMatrix(String yearMonth) {
        log.info("Getting market x environment matrix: yearMonth={}", yearMonth);

        List<Object[]> data = monthlyCostSummaryRepository.findMonthlyCostByMarketAndEnvironment(yearMonth);

        List<MarketEnvironmentCostItem> items = new ArrayList<>();
        Set<String> marketSet = new LinkedHashSet<>();
        Set<String> envSet = new LinkedHashSet<>();

        for (Object[] row : data) {
            String market = (String) row[0];
            Environment env = (Environment) row[1];
            BigDecimal cost = ((BigDecimal) row[2]).setScale(2, RoundingMode.HALF_UP);
            items.add(new MarketEnvironmentCostItem(market, env.name(), cost));
            marketSet.add(market);
            envSet.add(env.name());
        }

        return new MarketEnvironmentCostResponse(items, new ArrayList<>(marketSet), new ArrayList<>(envSet));
    }

    @Cacheable("billing-market-trend")
    public MarketTrendResponse getMarketTrend(int months) {
        log.info("Getting market trend: months={}", months);

        YearMonth currentMonth = YearMonth.now();
        YearMonth startMonth = currentMonth.minusMonths(months - 1);
        String startYearMonth = startMonth.format(YEAR_MONTH_FMT);
        String endYearMonth = currentMonth.format(YEAR_MONTH_FMT);

        List<Object[]> trendData = monthlyCostSummaryRepository.findMarketTrend(startYearMonth, endYearMonth);

        List<MarketTrendPoint> data = new ArrayList<>();
        Set<String> marketSet = new LinkedHashSet<>();

        for (Object[] row : trendData) {
            String yearMonth = (String) row[0];
            String market = (String) row[1];
            BigDecimal cost = ((BigDecimal) row[2]).setScale(2, RoundingMode.HALF_UP);
            data.add(new MarketTrendPoint(yearMonth, market, cost));
            marketSet.add(market);
        }

        return new MarketTrendResponse(data, new ArrayList<>(marketSet));
    }

    public CostDriverResponse getTopCostDrivers(String yearMonth, int limit) {
        log.info("Getting top cost drivers: yearMonth={}, limit={}", yearMonth, limit);

        List<Object[]> costByMarketService = monthlyCostSummaryRepository.findCostByMarketAndService(yearMonth);

        // Previous month for MoM
        YearMonth ym = YearMonth.parse(yearMonth, YEAR_MONTH_FMT);
        String prevYearMonth = ym.minusMonths(1).format(YEAR_MONTH_FMT);
        Map<String, BigDecimal> prevCostMap = new HashMap<>();
        for (Object[] row : monthlyCostSummaryRepository.findCostByMarketAndService(prevYearMonth)) {
            String key = row[0] + "|" + row[1];
            prevCostMap.put(key, (BigDecimal) row[2]);
        }

        BigDecimal totalCost = costByMarketService.stream()
                .map(row -> (BigDecimal) row[2])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CostDriverItem> items = new ArrayList<>();
        for (Object[] row : costByMarketService) {
            String market = (String) row[0];
            String serviceName = (String) row[1];
            BigDecimal cost = ((BigDecimal) row[2]).setScale(2, RoundingMode.HALF_UP);

            BigDecimal percentage = BigDecimal.ZERO;
            if (totalCost.compareTo(BigDecimal.ZERO) != 0) {
                percentage = cost.divide(totalCost, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            String key = market + "|" + serviceName;
            BigDecimal prevCost = prevCostMap.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal momChange = BigDecimal.ZERO;
            if (prevCost.compareTo(BigDecimal.ZERO) != 0) {
                momChange = cost.subtract(prevCost)
                        .divide(prevCost, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            items.add(new CostDriverItem(market, serviceName, cost, percentage, momChange));
            if (items.size() >= limit) break;
        }

        return new CostDriverResponse(items);
    }

    public CostMoverResponse getTopCostMovers(String yearMonth, int limit) {
        log.info("Getting top cost movers: yearMonth={}, limit={}", yearMonth, limit);

        YearMonth ym = YearMonth.parse(yearMonth, YEAR_MONTH_FMT);
        String prevYearMonth = ym.minusMonths(1).format(YEAR_MONTH_FMT);

        List<Object[]> currentCosts = monthlyCostSummaryRepository.findCostByMarketAndService(yearMonth);
        Map<String, BigDecimal> prevCostMap = new HashMap<>();
        for (Object[] row : monthlyCostSummaryRepository.findCostByMarketAndService(prevYearMonth)) {
            String key = row[0] + "|" + row[1];
            prevCostMap.put(key, (BigDecimal) row[2]);
        }

        List<CostMoverItem> movers = new ArrayList<>();
        for (Object[] row : currentCosts) {
            String market = (String) row[0];
            String serviceName = (String) row[1];
            BigDecimal currentCost = ((BigDecimal) row[2]).setScale(2, RoundingMode.HALF_UP);
            String key = market + "|" + serviceName;
            BigDecimal previousCost = prevCostMap.getOrDefault(key, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal changeAmount = currentCost.subtract(previousCost).setScale(2, RoundingMode.HALF_UP);
            BigDecimal changePercent = BigDecimal.ZERO;
            if (previousCost.compareTo(BigDecimal.ZERO) != 0) {
                changePercent = changeAmount.divide(previousCost, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }
            movers.add(new CostMoverItem(market, serviceName, currentCost, previousCost, changeAmount, changePercent));
        }

        movers.sort((a, b) -> b.changeAmount().compareTo(a.changeAmount()));
        return new CostMoverResponse(movers.subList(0, Math.min(limit, movers.size())));
    }

    public BurnRateResponse getBurnRate(String yearMonth, String market) {
        log.info("Getting burn rate: yearMonth={}, market={}", yearMonth, market);

        YearMonth ym = YearMonth.parse(yearMonth, YEAR_MONTH_FMT);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();
        LocalDate today = LocalDate.now();
        if (endDate.isAfter(today)) {
            endDate = today;
        }

        List<String> accountIds = accountMetadataRepository.findByMarket(market)
                .stream()
                .map(AccountMetadata::getAccountId)
                .toList();

        if (accountIds.isEmpty()) {
            return new BurnRateResponse(market, yearMonth, List.of(), BigDecimal.ZERO, BigDecimal.ZERO);
        }

        List<Object[]> dailyCosts = dailyCostRepository.findDailyCostForAccounts(accountIds, startDate, endDate);

        List<BurnRateItem> items = new ArrayList<>();
        BigDecimal cumulative = BigDecimal.ZERO;
        int dayCount = 0;

        for (Object[] row : dailyCosts) {
            LocalDate date = (LocalDate) row[0];
            BigDecimal dailyCost = ((BigDecimal) row[1]).setScale(2, RoundingMode.HALF_UP);
            cumulative = cumulative.add(dailyCost);
            dayCount++;

            BigDecimal avgDaily = cumulative.divide(BigDecimal.valueOf(dayCount), 6, RoundingMode.HALF_UP);
            BigDecimal projected = avgDaily.multiply(BigDecimal.valueOf(ym.lengthOfMonth())).setScale(2, RoundingMode.HALF_UP);

            items.add(new BurnRateItem(date, dailyCost, cumulative.setScale(2, RoundingMode.HALF_UP), projected));
        }

        BigDecimal avgDailyBurn = dayCount > 0
                ? cumulative.divide(BigDecimal.valueOf(dayCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal projectedMonthEnd = avgDailyBurn.multiply(BigDecimal.valueOf(ym.lengthOfMonth())).setScale(2, RoundingMode.HALF_UP);

        return new BurnRateResponse(market, yearMonth, items, avgDailyBurn, projectedMonthEnd);
    }

    public MarketBudgetResponse getBudgetVsActualByMarket(String yearMonth) {
        log.info("Getting budget vs actual by market: yearMonth={}", yearMonth);

        List<String> markets = accountMetadataRepository.findDistinctMarkets();
        List<Object[]> marketCosts = monthlyCostSummaryRepository.findMonthlyCostByMarket(yearMonth);

        Map<String, BigDecimal> actualCostMap = new HashMap<>();
        for (Object[] row : marketCosts) {
            actualCostMap.put((String) row[0], (BigDecimal) row[1]);
        }

        List<MarketBudgetItem> items = new ArrayList<>();
        for (String market : markets) {
            var budgets = budgetRepository.findByMarketAndYearMonthAndActiveTrue(market, yearMonth);
            BigDecimal budgetAmount = budgets.stream()
                    .map(b -> b.getBudgetAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal actualAmount = actualCostMap.getOrDefault(market, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal utilization = BigDecimal.ZERO;
            if (budgetAmount.compareTo(BigDecimal.ZERO) != 0) {
                utilization = actualAmount.divide(budgetAmount, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }
            BigDecimal remaining = budgetAmount.subtract(actualAmount).setScale(2, RoundingMode.HALF_UP);

            items.add(new MarketBudgetItem(market, budgetAmount, actualAmount, utilization, remaining));
        }

        return new MarketBudgetResponse(items);
    }

    public List<String> getDistinctMarkets() {
        return accountMetadataRepository.findDistinctMarkets();
    }
}
