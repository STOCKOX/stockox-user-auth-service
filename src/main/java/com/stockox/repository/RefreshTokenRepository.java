package com.stockox.repository;

import com.stockox.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<com.stockox.entity.RefreshToken> findByTokenAndRevokedFalse(String token);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user.id = :userId")
    void revokeAllByUserId(@Param("userId") UUID userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :cutoff OR r.revoked = true")
    void deleteExpiredTokens(@Param("cutoff") LocalDateTime cutoff);
}