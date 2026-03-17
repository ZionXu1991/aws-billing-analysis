package com.devops.billing.repository;

import com.devops.billing.entity.AccountMetadata;
import com.devops.billing.enums.Environment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountMetadataRepository extends JpaRepository<AccountMetadata, Long> {

    Optional<AccountMetadata> findByAccountId(String accountId);

    List<AccountMetadata> findByTeam(String team);

    List<AccountMetadata> findByEnvironment(Environment environment);

    List<AccountMetadata> findByActiveTrue();

    boolean existsByAccountId(String accountId);

    List<AccountMetadata> findByMarket(String market);

    List<AccountMetadata> findByMarketAndEnvironment(String market, Environment environment);

    @Query("SELECT DISTINCT a.market FROM AccountMetadata a WHERE a.active = true ORDER BY a.market")
    List<String> findDistinctMarkets();
}
