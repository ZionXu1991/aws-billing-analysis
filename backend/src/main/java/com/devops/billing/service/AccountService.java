package com.devops.billing.service;

import com.devops.billing.dto.BillingDTO.AccountRequest;
import com.devops.billing.dto.BillingDTO.AccountResponse;
import com.devops.billing.entity.AccountMetadata;
import com.devops.billing.enums.Environment;
import com.devops.billing.repository.AccountMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountMetadataRepository accountMetadataRepository;

    @Transactional
    public AccountResponse create(AccountRequest request) {
        if (accountMetadataRepository.existsByAccountId(request.accountId())) {
            throw new RuntimeException("Account already exists with accountId: " + request.accountId());
        }

        AccountMetadata account = AccountMetadata.builder()
                .accountId(request.accountId())
                .accountName(request.accountName())
                .team(request.team())
                .environment(Environment.valueOf(request.environment()))
                .market(request.market() != null ? request.market() : "UNKNOWN")
                .region(request.region() != null ? request.region() : "ap-southeast-1")
                .build();

        account = accountMetadataRepository.save(account);
        return toResponse(account);
    }

    public AccountResponse getById(Long id) {
        AccountMetadata account = accountMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + id));
        return toResponse(account);
    }

    public List<AccountResponse> getAll() {
        return accountMetadataRepository.findByActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AccountResponse update(Long id, AccountRequest request) {
        AccountMetadata account = accountMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + id));

        account.setAccountName(request.accountName());
        account.setTeam(request.team());
        account.setEnvironment(Environment.valueOf(request.environment()));
        if (request.market() != null) {
            account.setMarket(request.market());
        }
        if (request.region() != null) {
            account.setRegion(request.region());
        }

        account = accountMetadataRepository.save(account);
        return toResponse(account);
    }

    @Transactional
    public void delete(Long id) {
        AccountMetadata account = accountMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + id));
        account.setActive(false);
        accountMetadataRepository.save(account);
    }

    private AccountResponse toResponse(AccountMetadata account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountId(),
                account.getAccountName(),
                account.getTeam(),
                account.getEnvironment().name(),
                account.getMarket(),
                account.getRegion(),
                account.getActive(),
                account.getCreatedAt()
        );
    }
}
