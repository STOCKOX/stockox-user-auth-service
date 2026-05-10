package com.stockox.service.impl;

import com.stockox.entity.OtpToken;
import com.stockox.entity.User;
import com.stockox.enums.OtpType;
import com.stockox.exception.BadRequestException;
import com.stockox.repository.OtpTokenRepository;
import com.stockox.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final OtpTokenRepository otpRepository;

  private final EmailService emailService;

  @Value("${app.otp.expiry-minutes}")
  private int expiryMinutes;

  @Value("${app.otp.length}")
  private int otpLength;

  @Value("${app.otp.max-attempts}")
  private int maxAttempts;

  @Override
  @Transactional
  public void generateAndSend(User user, OtpType type) {
    // Expire any existing unused OTPs of the same type
    otpRepository.expireAllPreviousOtps(user, type);

    String code = generateCode();

    OtpToken otp = OtpToken.builder()
        .user(user)
        .code(code)
        .type(type)
        .used(false)
        .attempts(0)
        .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
        .build();

    otpRepository.save(otp);
    log.info("OTP generated for user: {} type: {}", user.getEmail(), type);

    // Delegate to separate bean so @Async proxy is honoured
    emailService.sendOtpEmailAsync(user.getEmail(), user.getFirstName(), code, type);
  }

  @Override
  @Transactional
  public void verify(User user, String code, OtpType type) {
    OtpToken otp = otpRepository
        .findLatestValidOtp(user, type, LocalDateTime.now())
        .orElseThrow(() -> new BadRequestException(
            "OTP has expired. Please request a new one."));

    if (otp.isLocked(maxAttempts)) {
      throw new BadRequestException(
          "Too many wrong attempts. Please request a new OTP.");
    }

    if (!otp.getCode().equals(code)) {
      otp.setAttempts(otp.getAttempts() + 1);
      otpRepository.save(otp);

      int remaining = maxAttempts - otp.getAttempts();
      if (remaining <= 0) {
        throw new BadRequestException(
            "Too many wrong attempts. Please request a new OTP.");
      }
      throw new BadRequestException(
          String.format("Invalid OTP. %d attempt(s) remaining.", remaining));
    }

    otp.setUsed(true);
    otpRepository.save(otp);
    log.info("OTP verified for user: {} type: {}", user.getEmail(), type);
  }

  private String generateCode() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < otpLength; i++) {
      sb.append(SECURE_RANDOM.nextInt(10));
    }
    return sb.toString();
  }
}