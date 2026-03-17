package com.devops.billing.repository;

import com.devops.billing.entity.MonthlyCostSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlyCostSummaryRepository extends JpaRepository<MonthlyCostSummary, Long> {

    List<MonthlyCostSummary> findByYearMonth(String yearMonth);

    List<MonthlyCostSummary> findByAccountIdAndYearMonth(String accountId, String yearMonth);

    List<MonthlyCostSummary> findByAccountId(String accountId);

    @Query("SELECT m.environment, SUM(m.totalCost) FROM MonthlyCostSummary m " +
            "WHERE m.yearMonth = :yearMonth " +
            "GROUP BY m.environment")
    List<Object[]> findMonthlyCostByEnvironment(@Param("yearMonth") String yearMonth);

    @Query("SELECT m.yearMonth, SUM(m.totalCost) FROM MonthlyCostSummary m " +
            "WHERE m.yearMonth BETWEEN :startYearMonth AND :endYearMonth " +
            "GROUP BY m.yearMonth ORDER BY m.yearMonth ASC")
    List<Object[]> findMonthlyTrend(@Param("startYearMonth") String startYearMonth,
                                     @Param("endYearMonth") String endYearMonth);

    @Query("SELECT m.market, SUM(m.totalCost) FROM MonthlyCostSummary m " +
            "WHERE m.yearMonth = :yearMonth AND m.market IS NOT NULL " +
            "GROUP BY m.market")
    List<Object[]> findMonthlyCostByMarket(@Param("yearMonth") String yearMonth);

    @Query("SELECT m.market, m.environment, SUM(m.totalCost) FROM MonthlyCostSummary m " +
            "WHERE m.yearMonth = :yearMonth AND m.market IS NOT NULL " +
            "GROUP BY m.market, m.environment")
    List<Object[]> findMonthlyCostByMarketAndEnvironment(@Param("yearMonth") String yearMonth);

    @Query("SELECT m.yearMonth, m.market, SUM(m.totalCost) FROM MonthlyCostSummary m " +
            "WHERE m.yearMonth BETWEEN :startYearMonth AND :endYearMonth AND m.market IS NOT NULL " +
            "GROUP BY m.yearMonth, m.market ORDER BY m.yearMonth ASC")
    List<Object[]> findMarketTrend(@Param("startYearMonth") String startYearMonth,
                                    @Param("endYearMonth") String endYearMonth);

    @Query("SELECT m.market, m.serviceName, SUM(m.totalCost) FROM MonthlyCostSummary m " +
            "WHERE m.yearMonth = :yearMonth AND m.market IS NOT NULL " +
            "GROUP BY m.market, m.serviceName ORDER BY SUM(m.totalCost) DESC")
    List<Object[]> findCostByMarketAndService(@Param("yearMonth") String yearMonth);

    Optional<MonthlyCostSummary> findByAccountIdAndYearMonthAndServiceName(
            String accountId, String yearMonth, String serviceName);
}
