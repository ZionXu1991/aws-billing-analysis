package com.devops.billing.entity;

import com.devops.billing.enums.AnomalySeverity;
import com.devops.billing.enums.AnomalyStatus;
import com.devops.billing.enums.AnomalyType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cost_anomaly", schema = "billing",
        indexes = {
                @Index(name = "idx_cost_anomaly_account_id", columnList = "accountId"),
                @Index(name = "idx_cost_anomaly_severity", columnList = "severity"),
                @Index(name = "idx_cost_anomaly_status", columnList = "status"),
                @Index(name = "idx_cost_anomaly_detected_date", columnList = "detectedDate")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostAnomaly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String serviceName;

    @Column(nullable = false)
    private LocalDate detectedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnomalyType anomalyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnomalySeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AnomalyStatus status = AnomalyStatus.OPEN;

    @Column(precision = 16, scale = 6)
    private BigDecimal expectedCost;

    @Column(precision = 16, scale = 6)
    private BigDecimal actualCost;

    @Column(precision = 16, scale = 6)
    private BigDecimal deviationPercent;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String acknowledgedBy;

    private LocalDateTime acknowledgedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
