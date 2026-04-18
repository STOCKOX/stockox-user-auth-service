package com.stockox.config;

import com.stockox.repository.OtpTokenRepository;
import com.stockox.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor

public class TokenCleanupScheduler {

    private final OtpTokenRepository otpTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(fixedRate = 3600000) // 1hr
    @Transactional
    public void cleanUpOtpTokens() {

        // find OTPs which are older than 24hrs
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        otpTokenRepository.deleteExpiredOtps(cutoff);
        log.debug("OTP token cleanup completed.");
    }


    // Delete expired and revoked refresh tokens every day at 2AM
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanUpRefreshTokens() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        refreshTokenRepository.deleteExpiredTokens(cutoff);
        log.info("Refresh token cleanup completed at 2 AM.");




    }
}
