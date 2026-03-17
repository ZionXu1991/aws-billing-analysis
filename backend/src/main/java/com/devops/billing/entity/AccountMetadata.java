package com.devops.billing.entity;

import com.devops.billing.enums.Environment;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "account_metadata", schema = "billing",
        indexes = {
                @Index(name = "idx_account_metadata_account_id", columnList = "accountId", unique = true),
                @Index(name = "idx_account_metadata_team", columnList = "team"),
                @Index(name = "idx_account_metadata_environment", columnList = "environment"),
                @Index(name = "idx_account_metadata_market", columnList = "market"),
                @Index(name = "idx_account_metadata_market_env", columnList = "market,environment")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String accountId;

    @Column(nullable = false)
    private String accountName;

    @Column(nullable = false)
    private String team;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Environment environment;

    @Column(nullable = false)
    @Builder.Default
    private String market = "UNKNOWN";

    @Column(nullable = false)
    @Builder.Default
    private String region = "ap-southeast-1";

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
