package com.devops.billing.service;

import com.devops.billing.dto.BillingDTO.*;
import com.devops.billing.entity.DailyCost;
import com.devops.billing.repository.DailyCostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CostQueryService {

    private final DailyCostRepository dailyCostRepository;

    private static final DateTimeFormatter YEAR_MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    public DailyCostResponse getDailyCosts(String accountId, LocalDate startDate, LocalDate endDate) {
        log.info("Querying daily costs: accountId={}, start={}, end={}", accountId, startDate, endDate);

        List<DailyCost> costs = dailyCostRepository.findByAccountIdAndCostDateBetween(accountId, startDate, endDate);

        List<DailyCostItem> items = costs.stream()
                .map(dc -> new DailyCostItem(
                        dc.getCostDate(),
                        dc.getAccountId(),
                        dc.getServiceName(),
                        dc.getRegion(),
                        dc.getUnblendedCost().setScale(2, RoundingMode.HALF_UP)
                ))
                .toList();

        BigDecimal totalCost = items.stream()
                .map(DailyCostItem::cost)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new DailyCostResponse(items, totalCost);
    }

    public CostComparisonResponse getComparison(String accountId, String currentMonth, String comparisonType) {
        log.info("Getting cost comparison: accountId={}, currentMonth={}, type={}", accountId, currentMonth, comparisonType);

        YearMonth current = YearMonth.parse(currentMonth, YEAR_MONTH_FMT);
        YearMonth previous;

        if ("yoy".equalsIgnoreCase(comparisonType)) {
            previous = current.minusYears(1);
        } else {
            // default to "mom"
            previous = current.minusMonths(1);
        }

        List<CostComparisonItem> items = new ArrayList<>();

        // Compare daily costs grouped by date offset within the month
        LocalDate currentStart = current.atDay(1);
        LocalDate currentEnd = current.atEndOfMonth();
        LocalDate previousStart = previous.atDay(1);
        LocalDate previousEnd = previous.atEndOfMonth();

        List<Object[]> currentDailyTotals = dailyCostRepository.findDailyCostGroupedByDate(accountId, currentStart, currentEnd);
        List<Object[]> previousDailyTotals = dailyCostRepository.findDailyCostGroupedByDate(accountId, previousStart, previousEnd);

        // Build maps by day-of-month
        java.util.Map<Integer, BigDecimal> currentMap = new java.util.LinkedHashMap<>();
        for (Object[] row : currentDailyTotals) {
            LocalDate date = (LocalDate) row[0];
            BigDecimal cost = (BigDecimal) row[1];
            currentMap.put(date.getDayOfMonth(), cost);
        }

        java.util.Map<Integer, BigDecimal> previousMap = new java.util.LinkedHashMap<>();
        for (Object[] row : previousDailyTotals) {
            LocalDate date = (LocalDate) row[0];
            BigDecimal cost = (BigDecimal) row[1];
            previousMap.put(date.getDayOfMonth(), cost);
        }

        int maxDay = Math.max(
                currentMap.keySet().stream().mapToInt(Integer::intValue).max().orElse(0),
                previousMap.keySet().stream().mapToInt(Integer::intValue).max().orElse(0)
        );

        for (int day = 1; day <= maxDay; day++) {
            BigDecimal currentCost = Optional.ofNullable(currentMap.get(day))
                    .orElse(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal previousCost = Optional.ofNullable(previousMap.get(day))
                    .orElse(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

            BigDecimal changePercent = BigDecimal.ZERO;
            if (previousCost.compareTo(BigDecimal.ZERO) != 0) {
                changePercent = currentCost.subtract(previousCost)
                        .divide(previousCost, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            String period = String.format("Day %d", day);
            items.add(new CostComparisonItem(period, currentCost, previousCost, changePercent));
        }

        return new CostComparisonResponse(items, comparisonType);
    }
}
