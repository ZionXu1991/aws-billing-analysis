package com.devops.billing.controller;

import com.devops.billing.dto.BillingDTO.*;
import com.devops.billing.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/billing/accounts")
@RequiredArgsConstructor
@Tag(name = "Billing Accounts", description = "AWS account management for billing")
public class BillingAccountController {

    private final AccountService accountService;

    @PostMapping
    @Operation(summary = "Register a new AWS account")
    public ResponseEntity<AccountResponse> create(@RequestBody AccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.create(request));
    }

    @GetMapping
    @Operation(summary = "List all accounts")
    public ResponseEntity<List<AccountResponse>> getAll() {
        return ResponseEntity.ok(accountService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account by ID")
    public ResponseEntity<AccountResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update account details")
    public ResponseEntity<AccountResponse> update(@PathVariable Long id, @RequestBody AccountRequest request) {
        return ResponseEntity.ok(accountService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate an account")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        accountService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
