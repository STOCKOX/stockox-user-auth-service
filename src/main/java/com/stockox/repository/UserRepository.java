package com.stockox.repository;

import com.stockox.entity.Tenant;
import com.stockox.entity.User;
import com.stockox.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailAndDeletedFalse(String email);

    boolean existsByEmail(String email);

    List<User> findAllByTenantAndDeletedFalseOrderByCreatedAtDesc(Tenant tenant);

    List<User> findAllByTenantAndStatusAndDeletedFalse(Tenant tenant, UserStatus status);

    long countByTenantAndDeletedFalse(Tenant tenant);

    @Query("SELECT u FROM User u WHERE u.id = :userId AND u.tenant = :tenant AND u.deleted = false")
    Optional<User> findByIdAndTenant(UUID userId, Tenant tenant);


}
