package com.devops.billing.service;

import com.devops.billing.dto.BillingDTO.AnomalyResponse;
import com.devops.billing.entity.AccountMetadata;
import com.devops.billing.entity.CostAnomaly;
import com.devops.billing.entity.DailyCost;
import com.devops.billing.enums.AnomalySeverity;
import com.devops.billing.enums.AnomalyStatus;
import com.devops.billing.enums.AnomalyType;
import com.devops.billing.repository.AccountMetadataRepository;
import com.devops.billing.repository.CostAnomalyRepository;
import com.devops.billing.repository.DailyCostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnomalyDetectionService {

    private final DailyCostRepository dailyCostRepository;
    private final AccountMetadataRepository accountMetadataRepository;
    private final CostAnomalyRepository costAnomalyRepository;

    @Transactional
    public List<CostAnomaly> detectAnomalies(LocalDate date) {
        log.info("Running anomaly detection for date: {}", date);

        List<AccountMetadata> activeAccounts = accountMetadataRepository.findByActiveTrue();
        List<CostAnomaly> detectedAnomalies = new ArrayList<>();

        LocalDate rollingStart = date.minusDays(30);
        LocalDate rollingEnd = date.minusDays(1);

        for (AccountMetadata account : activeAccounts) {
            String accountId = account.getAccountId();

            // Get today's costs grouped by service
            List<DailyCost> todayCosts = dailyCostRepository.findByAccountIdAndCostDateBetween(accountId, date, date);
            Map<String, BigDecimal> todayServiceCosts = todayCosts.stream()
                    .collect(Collectors.groupingBy(
                            DailyCost::getServiceName,
                            Collectors.reducing(BigDecimal.ZERO, DailyCost::getUnblendedCost, BigDecimal::add)
                    ));

            // Get 30-day rolling costs by service
            List<DailyCost> historicalCosts = dailyCostRepository.findByAccountIdAndCostDateBetween(accountId, rollingStart, rollingEnd);
            Map<String, List<BigDecimal>> historicalServiceCosts = new HashMap<>();
            for (DailyCost dc : historicalCosts) {
                historicalServiceCosts
                        .computeIfAbsent(dc.getServiceName(), k -> new ArrayList<>())
                        .add(dc.getUnblendedCost());
            }

            // Collect all known services (historical + today)
            Set<String> allServices = new HashSet<>(todayServiceCosts.keySet());
            allServices.addAll(historicalServiceCosts.keySet());

            for (String serviceName : allServices) {
                BigDecimal todayCost = todayServiceCosts.getOrDefault(serviceName, BigDecimal.ZERO);
                List<BigDecimal> history = historicalServiceCosts.getOrDefault(serviceName, Collections.emptyList());

                // Detect NEW_SERVICE
                if (history.isEmpty() && todayCost.compareTo(BigDecimal.ZERO) > 0) {
                    CostAnomaly anomaly = CostAnomaly.builder()
                            .accountId(accountId)
                            .serviceName(serviceName)
                            .detectedDate(date)
                            .anomalyType(AnomalyType.NEW_SERVICE)
                            .severity(AnomalySeverity.MEDIUM)
                            .expectedCost(BigDecimal.ZERO)
                            .actualCost(todayCost.setScale(2, RoundingMode.HALF_UP))
                            .deviationPercent(BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP))
                            .description(String.format("New service '%s' detected for account %s with cost $%s",
                                    serviceName, accountId, todayCost.setScale(2, RoundingMode.HALF_UP)))
                            .build();
                    detectedAnomalies.add(anomaly);
                    continue;
                }

                // Detect ZERO_COST
                if (!history.isEmpty() && todayCost.compareTo(BigDecimal.ZERO) == 0) {
                    BigDecimal avgHistorical = calculateMean(history);
                    if (avgHistorical.compareTo(BigDecimal.valueOf(1)) > 0) {
                        CostAnomaly anomaly = CostAnomaly.builder()
                                .accountId(accountId)
                                .serviceName(serviceName)
                                .detectedDate(date)
                                .anomalyType(AnomalyType.ZERO_COST)
                                .severity(AnomalySeverity.MEDIUM)
                                .expectedCost(avgHistorical.setScale(2, RoundingMode.HALF_UP))
                                .actualCost(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                                .deviationPercent(BigDecimal.valueOf(-100).setScale(2, RoundingMode.HALF_UP))
                                .description(String.format("Service '%s' for account %s has zero cost, expected ~$%s",
                                        serviceName, accountId, avgHistorical.setScale(2, RoundingMode.HALF_UP)))
                                .build();
                        detectedAnomalies.add(anomaly);
                    }
                    continue;
                }

                // Standard deviation based detection (SPIKE)
                if (history.size() >= 7) {
                    BigDecimal mean = calculateMean(history);
                    BigDecimal stdDev = calculateStdDev(history, mean);

                    if (stdDev.compareTo(BigDecimal.ZERO) > 0 && mean.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal deviation = todayCost.subtract(mean).divide(stdDev, 6, RoundingMode.HALF_UP);
                        BigDecimal deviationPercent = todayCost.subtract(mean)
                                .divide(mean, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(2, RoundingMode.HALF_UP);

                        AnomalySeverity severity = null;
                        if (deviation.compareTo(BigDecimal.valueOf(3)) >= 0) {
                            severity = AnomalySeverity.CRITICAL;
                        } else if (deviation.compareTo(BigDecimal.valueOf(2)) >= 0) {
                            severity = AnomalySeverity.HIGH;
                        } else if (deviation.compareTo(BigDecimal.valueOf(1.5)) >= 0) {
                            severity = AnomalySeverity.MEDIUM;
                        }

                        if (severity != null) {
                            CostAnomaly anomaly = CostAnomaly.builder()
                                    .accountId(accountId)
                                    .serviceName(serviceName)
                                    .detectedDate(date)
                                    .anomalyType(AnomalyType.SPIKE)
                                    .severity(severity)
                                    .expectedCost(mean.setScale(2, RoundingMode.HALF_UP))
                                    .actualCost(todayCost.setScale(2, RoundingMode.HALF_UP))
                                    .deviationPercent(deviationPercent)
                                    .description(String.format("Cost spike detected for service '%s' in account %s: " +
                                                    "$%s vs expected $%s (%.1fσ deviation)",
                                            serviceName, accountId,
                                            todayCost.setScale(2, RoundingMode.HALF_UP),
                                            mean.setScale(2, RoundingMode.HALF_UP),
                                            deviation.doubleValue()))
                                    .build();
                            detectedAnomalies.add(anomaly);
                        }
                    }
                }
            }
        }

        if (!detectedAnomalies.isEmpty()) {
            costAnomalyRepository.saveAll(detectedAnomalies);
            log.info("Detected and saved {} anomalies for date {}", detectedAnomalies.size(), date);
        } else {
            log.info("No anomalies detected for date {}", date);
        }

        return detectedAnomalies;
    }

    public Page<AnomalyResponse> getAnomalies(Pageable pageable) {
        Map<String, String> accountNameMap = accountMetadataRepository.findByActiveTrue()
                .stream()
                .collect(Collectors.toMap(AccountMetadata::getAccountId, AccountMetadata::getAccountName, (a, b) -> a));

        return costAnomalyRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(anomaly -> toAnomalyResponse(anomaly, accountNameMap));
    }

    @Transactional
    public AnomalyResponse acknowledgeAnomaly(Long id, String acknowledgedBy) {
        CostAnomaly anomaly = costAnomalyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Anomaly not found with id: " + id));

        anomaly.setStatus(AnomalyStatus.ACKNOWLEDGED);
        anomaly.setAcknowledgedBy(acknowledgedBy);
        anomaly.setAcknowledgedAt(LocalDateTime.now());
        costAnomalyRepository.save(anomaly);

        Map<String, String> accountNameMap = accountMetadataRepository.findByActiveTrue()
                .stream()
                .collect(Collectors.toMap(AccountMetadata::getAccountId, AccountMetadata::getAccountName, (a, b) -> a));

        log.info("Anomaly {} acknowledged by {}", id, acknowledgedBy);
        return toAnomalyResponse(anomaly, accountNameMap);
    }

    private AnomalyResponse toAnomalyResponse(CostAnomaly anomaly, Map<String, String> accountNameMap) {
        return new AnomalyResponse(
                anomaly.getId(),
                anomaly.getAccountId(),
                accountNameMap.getOrDefault(anomaly.getAccountId(), anomaly.getAccountId()),
                anomaly.getServiceName(),
                anomaly.getDetectedDate(),
                anomaly.getAnomalyType().name(),
                anomaly.getSeverity().name(),
                anomaly.getStatus().name(),
                anomaly.getExpectedCost() != null ? anomaly.getExpectedCost().setScale(2, RoundingMode.HALF_UP) : null,
                anomaly.getActualCost() != null ? anomaly.getActualCost().setScale(2, RoundingMode.HALF_UP) : null,
                anomaly.getDeviationPercent() != null ? anomaly.getDeviationPercent().setScale(2, RoundingMode.HALF_UP) : null,
                anomaly.getDescription(),
                anomaly.getAcknowledgedBy(),
                anomaly.getAcknowledgedAt(),
                anomaly.getCreatedAt()
        );
    }

    private BigDecimal calculateMean(List<BigDecimal> values) {
        if (values.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateStdDev(List<BigDecimal> values, BigDecimal mean) {
        if (values.size() < 2) return BigDecimal.ZERO;
        BigDecimal sumSquaredDiffs = values.stream()
                .map(v -> v.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal variance = sumSquaredDiffs.divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue())).setScale(6, RoundingMode.HALF_UP);
    }
}
