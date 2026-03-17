package com.devops.billing.entity;

import com.devops.billing.enums.Environment;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "monthly_cost_summary", schema = "billing",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_monthly_cost_account_yearmonth_service",
                        columnNames = {"accountId", "yearMonth", "serviceName"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyCostSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String yearMonth;

    @Column(nullable = false)
    private String serviceName;

    @Column(nullable = false, precision = 16, scale = 6)
    private BigDecimal totalCost;

    @Column(precision = 16, scale = 6)
    private BigDecimal previousMonthCost;

    @Column(precision = 16, scale = 6)
    private BigDecimal momChangePercent;

    @Enumerated(EnumType.STRING)
    private Environment environment;

    private String team;

    private String market;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
