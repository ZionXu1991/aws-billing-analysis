package com.devops.billing.service;

import com.devops.billing.dto.BillingDTO.*;
import com.devops.billing.entity.AccountMetadata;
import com.devops.billing.entity.MonthlyCostSummary;
import com.devops.billing.enums.Environment;
import com.devops.billing.repository.AccountMetadataRepository;
import com.devops.billing.repository.MonthlyCostSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CostAnalysisService {

    private final MonthlyCostSummaryRepository monthlyCostSummaryRepository;
    private final AccountMetadataRepository accountMetadataRepository;

    private static final DateTimeFormatter YEAR_MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    public CostAnalysisResponse analyze(CostAnalysisRequest request) {
        log.info("Running cost analysis: groupBy={}", request.groupBy());

        String startYearMonth = request.startYearMonth();
        String endYearMonth = request.endYearMonth();
        if (startYearMonth == null || endYearMonth == null) {
            String current = YearMonth.now().format(YEAR_MONTH_FMT);
            startYearMonth = current;
            endYearMonth = current;
        }

        // Fetch all summaries in the date range
        List<MonthlyCostSummary> allSummaries = new ArrayList<>();
        YearMonth start = YearMonth.parse(startYearMonth, YEAR_MONTH_FMT);
        YearMonth end = YearMonth.parse(endYearMonth, YEAR_MONTH_FMT);
        YearMonth ym = start;
        while (!ym.isAfter(end)) {
            allSummaries.addAll(monthlyCostSummaryRepository.findByYearMonth(ym.format(YEAR_MONTH_FMT)));
            ym = ym.plusMonths(1);
        }

        // Apply filters
        if (request.markets() != null && !request.markets().isEmpty()) {
            allSummaries = allSummaries.stream()
                    .filter(s -> request.markets().contains(s.getMarket()))
                    .collect(Collectors.toList());
        }
        if (request.environments() != null && !request.environments().isEmpty()) {
            allSummaries = allSummaries.stream()
                    .filter(s -> s.getEnvironment() != null && request.environments().contains(s.getEnvironment().name()))
                    .collect(Collectors.toList());
        }
        if (request.services() != null && !request.services().isEmpty()) {
            allSummaries = allSummaries.stream()
                    .filter(s -> request.services().contains(s.getServiceName()))
                    .collect(Collectors.toList());
        }
        if (request.accounts() != null && !request.accounts().isEmpty()) {
            allSummaries = allSummaries.stream()
                    .filter(s -> request.accounts().contains(s.getAccountId()))
                    .collect(Collectors.toList());
        }

        // Group by the requested dimension
        String groupBy = request.groupBy() != null ? request.groupBy() : "market";
        Function<MonthlyCostSummary, String> grouper = switch (groupBy) {
            case "service" -> MonthlyCostSummary::getServiceName;
            case "account" -> MonthlyCostSummary::getAccountId;
            case "environment" -> s -> s.getEnvironment() != null ? s.getEnvironment().name() : "UNKNOWN";
            case "yearMonth" -> MonthlyCostSummary::getYearMonth;
            default -> s -> s.getMarket() != null ? s.getMarket() : "UNKNOWN";
        };

        Map<String, BigDecimal> grouped = allSummaries.stream()
                .collect(Collectors.groupingBy(
                        grouper,
                        Collectors.reducing(BigDecimal.ZERO, MonthlyCostSummary::getTotalCost, BigDecimal::add)
                ));

        BigDecimal totalCost = grouped.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate MoM for each group using previous period
        YearMonth prevStart = start.minusMonths(1);
        YearMonth prevEnd = end.minusMonths(1);
        List<MonthlyCostSummary> prevSummaries = new ArrayList<>();
        ym = prevStart;
        while (!ym.isAfter(prevEnd)) {
            prevSummaries.addAll(monthlyCostSummaryRepository.findByYearMonth(ym.format(YEAR_MONTH_FMT)));
            ym = ym.plusMonths(1);
        }
        Map<String, BigDecimal> prevGrouped = prevSummaries.stream()
                .collect(Collectors.groupingBy(
                        grouper,
                        Collectors.reducing(BigDecimal.ZERO, MonthlyCostSummary::getTotalCost, BigDecimal::add)
                ));

        List<CostAnalysisItem> items = grouped.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(entry -> {
                    BigDecimal cost = entry.getValue().setScale(2, RoundingMode.HALF_UP);
                    BigDecimal percentage = BigDecimal.ZERO;
                    if (totalCost.compareTo(BigDecimal.ZERO) != 0) {
                        percentage = cost.divide(totalCost, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(2, RoundingMode.HALF_UP);
                    }
                    BigDecimal prevCost = prevGrouped.getOrDefault(entry.getKey(), BigDecimal.ZERO);
                    BigDecimal momChange = BigDecimal.ZERO;
                    if (prevCost.compareTo(BigDecimal.ZERO) != 0) {
                        momChange = cost.subtract(prevCost)
                                .divide(prevCost, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(2, RoundingMode.HALF_UP);
                    }
                    return new CostAnalysisItem(entry.getKey(), cost, percentage, momChange);
                })
                .toList();

        return new CostAnalysisResponse(items, totalCost.setScale(2, RoundingMode.HALF_UP), groupBy);
    }

    public FilterOptionsResponse getFilterOptions() {
        List<String> markets = accountMetadataRepository.findDistinctMarkets();
        List<String> environments = Arrays.stream(Environment.values())
                .map(Environment::name)
                .toList();
        List<String> services = monthlyCostSummaryRepository.findByYearMonth(YearMonth.now().format(YEAR_MONTH_FMT))
                .stream()
                .map(MonthlyCostSummary::getServiceName)
                .distinct()
                .sorted()
                .toList();
        List<String> accounts = accountMetadataRepository.findByActiveTrue()
                .stream()
                .map(AccountMetadata::getAccountId)
                .sorted()
                .toList();

        return new FilterOptionsResponse(markets, environments, services, accounts);
    }
}
