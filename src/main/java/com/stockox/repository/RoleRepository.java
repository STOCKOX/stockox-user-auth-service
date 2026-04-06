package com.stockox.repository;

import com.stockox.entity.Role;
import com.stockox.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
/** Queries for roles table. Mostly used by DataSeeder and AuthService. */
public interface RoleRepository extends JpaRepository<Role, UUID> {

    // Find role by enum
    Optional<Role> findByName(UserRole name);
}
