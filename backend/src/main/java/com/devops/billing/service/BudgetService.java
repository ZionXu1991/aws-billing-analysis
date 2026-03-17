package com.devops.billing.service;

import com.devops.billing.dto.BillingDTO.BudgetRequest;
import com.devops.billing.dto.BillingDTO.BudgetResponse;
import com.devops.billing.entity.Budget;
import com.devops.billing.repository.BudgetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;

    @Transactional
    public BudgetResponse create(BudgetRequest request) {
        Budget budget = Budget.builder()
                .accountId(request.accountId())
                .serviceName(request.serviceName())
                .market(request.market())
                .yearMonth(request.yearMonth())
                .budgetAmount(request.budgetAmount())
                .alertThresholdPercent(request.alertThresholdPercent() != null
                        ? request.alertThresholdPercent()
                        : new BigDecimal("80"))
                .build();

        budget = budgetRepository.save(budget);
        return toResponse(budget);
    }

    public List<BudgetResponse> getAll() {
        return budgetRepository.findByActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<BudgetResponse> getByMonth(String yearMonth) {
        return budgetRepository.findByYearMonthAndActiveTrue(yearMonth)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BudgetResponse update(Long id, BudgetRequest request) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget not found with id: " + id));

        budget.setBudgetAmount(request.budgetAmount());
        if (request.alertThresholdPercent() != null) {
            budget.setAlertThresholdPercent(request.alertThresholdPercent());
        }
        if (request.accountId() != null) {
            budget.setAccountId(request.accountId());
        }
        if (request.serviceName() != null) {
            budget.setServiceName(request.serviceName());
        }
        if (request.market() != null) {
            budget.setMarket(request.market());
        }
        if (request.yearMonth() != null) {
            budget.setYearMonth(request.yearMonth());
        }

        budget = budgetRepository.save(budget);
        return toResponse(budget);
    }

    @Transactional
    public void delete(Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget not found with id: " + id));
        budget.setActive(false);
        budgetRepository.save(budget);
    }

    private BudgetResponse toResponse(Budget budget) {
        BigDecimal utilizationPercent = BigDecimal.ZERO;
        if (budget.getActualAmount() != null && budget.getBudgetAmount().compareTo(BigDecimal.ZERO) != 0) {
            utilizationPercent = budget.getActualAmount()
                    .divide(budget.getBudgetAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return new BudgetResponse(
                budget.getId(),
                budget.getAccountId(),
                budget.getServiceName(),
                budget.getMarket(),
                budget.getYearMonth(),
                budget.getBudgetAmount().setScale(2, RoundingMode.HALF_UP),
                budget.getActualAmount() != null
                        ? budget.getActualAmount().setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                budget.getAlertThresholdPercent().setScale(2, RoundingMode.HALF_UP),
                utilizationPercent,
                budget.getActive()
        );
    }
}
