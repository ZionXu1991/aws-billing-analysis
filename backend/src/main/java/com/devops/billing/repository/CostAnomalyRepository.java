package com.devops.billing.repository;

import com.devops.billing.entity.CostAnomaly;
import com.devops.billing.enums.AnomalySeverity;
import com.devops.billing.enums.AnomalyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CostAnomalyRepository extends JpaRepository<CostAnomaly, Long> {

    List<CostAnomaly> findByStatusOrderByCreatedAtDesc(AnomalyStatus status);

    List<CostAnomaly> findByDetectedDateBetween(LocalDate startDate, LocalDate endDate);

    long countByStatus(AnomalyStatus status);

    List<CostAnomaly> findBySeverityAndStatus(AnomalySeverity severity, AnomalyStatus status);

    Page<CostAnomaly> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
