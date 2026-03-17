package com.devops.billing.scheduler;

import com.devops.billing.service.AnomalyDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "billing.anomaly-detection.enabled", havingValue = "true", matchIfMissing = false)
public class AnomalyDetectionScheduler {

    private final AnomalyDetectionService anomalyDetectionService;

    @Scheduled(cron = "0 30 6 * * *")
    public void runAnomalyDetection() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Starting anomaly detection for date: {}", yesterday);

        try {
            var anomaliesFound = anomalyDetectionService.detectAnomalies(yesterday);
            log.info("Anomaly detection completed for date: {}. Anomalies found: {}", yesterday, anomaliesFound.size());
        } catch (Exception e) {
            log.error("Failed to run anomaly detection for date: {}", yesterday, e);
            throw e;
        }
    }
}
