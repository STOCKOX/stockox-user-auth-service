package com.stockox.repository;


import com.stockox.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    // Used during registration to prevent duplicates
    Optional<Tenant> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

}
