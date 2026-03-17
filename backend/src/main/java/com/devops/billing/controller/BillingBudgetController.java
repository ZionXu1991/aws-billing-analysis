package com.devops.billing.controller;

import com.devops.billing.dto.BillingDTO.*;
import com.devops.billing.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/billing/budgets")
@RequiredArgsConstructor
@Tag(name = "Billing Budgets", description = "Budget tracking and management")
public class BillingBudgetController {

    private final BudgetService budgetService;

    @PostMapping
    @Operation(summary = "Create a new budget")
    public ResponseEntity<BudgetResponse> create(@RequestBody BudgetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.create(request));
    }

    @GetMapping
    @Operation(summary = "List all budgets")
    public ResponseEntity<List<BudgetResponse>> getAll() {
        return ResponseEntity.ok(budgetService.getAll());
    }

    @GetMapping("/month/{yearMonth}")
    @Operation(summary = "Get budgets for a specific month")
    public ResponseEntity<List<BudgetResponse>> getByMonth(@PathVariable String yearMonth) {
        return ResponseEntity.ok(budgetService.getByMonth(yearMonth));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a budget")
    public ResponseEntity<BudgetResponse> update(@PathVariable Long id, @RequestBody BudgetRequest request) {
        return ResponseEntity.ok(budgetService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a budget")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        budgetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
