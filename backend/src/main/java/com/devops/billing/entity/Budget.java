package com.devops.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "budget", schema = "billing",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_budget_account_service_yearmonth",
                        columnNames = {"accountId", "serviceName", "yearMonth"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountId;

    private String serviceName;

    private String market;

    @Column(nullable = false)
    private String yearMonth;

    @Column(nullable = false, precision = 16, scale = 6)
    private BigDecimal budgetAmount;

    @Column(precision = 16, scale = 6)
    private BigDecimal actualAmount;

    @Column(precision = 16, scale = 6)
    @Builder.Default
    private BigDecimal alertThresholdPercent = new BigDecimal("80");

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
