package com.devops.billing.repository;

import com.devops.billing.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByYearMonthAndActiveTrue(String yearMonth);

    Optional<Budget> findByAccountIdAndYearMonth(String accountId, String yearMonth);

    List<Budget> findByActiveTrue();

    List<Budget> findByMarketAndYearMonthAndActiveTrue(String market, String yearMonth);
}
