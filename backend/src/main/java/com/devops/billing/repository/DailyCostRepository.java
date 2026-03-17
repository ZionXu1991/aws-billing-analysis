package com.devops.billing.repository;

import com.devops.billing.entity.DailyCost;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyCostRepository extends JpaRepository<DailyCost, Long> {

    List<DailyCost> findByAccountIdAndCostDateBetween(String accountId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT SUM(d.unblendedCost) FROM DailyCost d WHERE d.costDate BETWEEN :startDate AND :endDate")
    BigDecimal sumCostByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT d.serviceName, SUM(d.unblendedCost) FROM DailyCost d " +
            "WHERE d.costDate BETWEEN :startDate AND :endDate " +
            "GROUP BY d.serviceName ORDER BY SUM(d.unblendedCost) DESC")
    List<Object[]> findDailyCostGroupedByService(@Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    @Query("SELECT d.accountId, SUM(d.unblendedCost) FROM DailyCost d " +
            "WHERE d.costDate BETWEEN :startDate AND :endDate " +
            "GROUP BY d.accountId ORDER BY SUM(d.unblendedCost) DESC")
    List<Object[]> findDailyCostGroupedByAccount(@Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    @Query("SELECT d.costDate, SUM(d.unblendedCost) FROM DailyCost d " +
            "WHERE d.accountId = :accountId AND d.costDate BETWEEN :startDate AND :endDate " +
            "GROUP BY d.costDate ORDER BY d.costDate ASC")
    List<Object[]> findDailyCostGroupedByDate(@Param("accountId") String accountId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    @Query("SELECT d.serviceName, SUM(d.unblendedCost) FROM DailyCost d " +
            "WHERE d.costDate BETWEEN :startDate AND :endDate " +
            "GROUP BY d.serviceName ORDER BY SUM(d.unblendedCost) DESC")
    List<Object[]> findTopServicesByCost(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate,
                                          Pageable pageable);

    void deleteByAccountIdAndCostDate(String accountId, LocalDate costDate);

    void deleteByCostDate(LocalDate costDate);

    @Query("SELECT d.costDate, SUM(d.unblendedCost) FROM DailyCost d " +
            "WHERE d.accountId IN :accountIds AND d.costDate BETWEEN :startDate AND :endDate " +
            "GROUP BY d.costDate ORDER BY d.costDate ASC")
    List<Object[]> findDailyCostForAccounts(@Param("accountIds") List<String> accountIds,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    @Query("SELECT d.region, SUM(d.unblendedCost) FROM DailyCost d " +
            "WHERE d.costDate BETWEEN :startDate AND :endDate " +
            "GROUP BY d.region ORDER BY SUM(d.unblendedCost) DESC")
    List<Object[]> findCostGroupedByRegion(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    List<DailyCost> findByCostDateBetween(LocalDate startDate, LocalDate endDate);
}
