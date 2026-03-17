package com.devops.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_cost", schema = "billing",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_daily_cost_account_date_service_region_usage",
                        columnNames = {"accountId", "costDate", "serviceName", "region", "usageType"})
        },
        indexes = {
                @Index(name = "idx_daily_cost_account_id", columnList = "accountId"),
                @Index(name = "idx_daily_cost_cost_date", columnList = "costDate"),
                @Index(name = "idx_daily_cost_service_name", columnList = "serviceName"),
                @Index(name = "idx_daily_cost_account_id_cost_date", columnList = "accountId, costDate")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyCost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 12)
    private String accountId;

    @Column(nullable = false)
    private LocalDate costDate;

    @Column(nullable = false)
    private String serviceName;

    @Column(nullable = false)
    private String region;

    @Column(nullable = false)
    private String usageType;

    @Column(nullable = false, precision = 16, scale = 6)
    private BigDecimal unblendedCost;

    @Column(precision = 16, scale = 6)
    private BigDecimal blendedCost;

    @Column(precision = 16, scale = 6)
    private BigDecimal usageQuantity;

    @Column(nullable = false)
    @Builder.Default
    private String currency = "USD";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
