package com.stockox.repository;

import com.stockox.entity.OtpToken;
import com.stockox.entity.User;
import com.stockox.enums.OtpType;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository


public interface OtpTokenRepository extends JpaRepository<OtpToken, UUID> {
    // To find the valid otp

    @Query("""
        SELECT o FROM OtpToken o
        WHERE o.user = :user
          AND o.type = :type
          AND o.code = :code
          AND o.used = false
          AND o.expiresAt > :now
        """)
    Optional<OtpToken> findValidOtp(
            @Param("user") User user,
            @Param("type") OtpType type,
            @Param("code") String code,
            @Param("now") LocalDateTime now
    );

    // To find the latest valid otp

    @Query("""
        SELECT o FROM OtpToken o
        WHERE o.user = :user
          AND o.type = :type
          AND o.used = false
          AND o.expiresAt > :now
        ORDER BY o.createdAt DESC
        LIMIT 1
        """)
    Optional<OtpToken> findLatestValidOtp(
            @Param("user") User user,
            @Param("type") OtpType type,
            @Param("now") LocalDateTime now
    );

    // Mark all previous otp's to true --  used before to send new otp
    @Modifying
    @Query("""
        UPDATE OtpToken o SET o.used = true
        WHERE o.user = :user
          AND o.type = :type
          AND o.used = false
        """)
    void expireAllPreviousOtps(
            @Param("user") User user,
            @Param("type") OtpType type
    );

    // for cleanUp Job---delete old used otp's which are older than 24 hrs.
    @Modifying
    @Query("""
        DELETE FROM OtpToken o
        WHERE (o.used = true OR o.expiresAt < :cutoff)
        """)
    void deleteExpiredOtps(@Param("cutoff") LocalDateTime cutoff);

}
