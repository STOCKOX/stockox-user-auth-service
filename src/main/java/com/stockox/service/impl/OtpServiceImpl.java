package com.stockox.service.impl;

import com.stockox.entity.OtpToken;
import com.stockox.entity.User;
import com.stockox.enums.OtpType;
import com.stockox.exception.BadRequestException;
import com.stockox.repository.OtpTokenRepository;
import com.stockox.service.OtpService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final OtpTokenRepository otpRepository;
    private final JavaMailSender mailSender;

    @Value("${app.otp.expiry-minutes}")
    private int expiryMinutes;

    @Value("${app.otp.length}")
    private int otpLength;

    @Value("${app.otp.max-attempts}")
    private int maxAttempts;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.app-name}")
    private String appName;

    @Override
    @Transactional
    public void generateAndSend(User user, OtpType type) {
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

        sendOtpEmailAsync(user.getEmail(), user.getFirstName(), code, type);
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

        log.info("OTP verified successfully for user: {} type: {}", user.getEmail(), type);
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    @Async
    public void sendOtpEmailAsync(String toEmail, String firstName,
                                  String code, OtpType type) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, appName);
            helper.setTo(toEmail);
            helper.setSubject(getEmailSubject(type));
            helper.setText(buildEmailHtml(firstName, code, type), true);

            mailSender.send(message);
            log.info("OTP email sent to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending OTP email: {}", e.getMessage(), e);
        }
    }

    private String getEmailSubject(OtpType type) {
        return switch (type) {
            case EMAIL_VERIFY -> appName + " — Verify your email address";
            case PASSWORD_RESET -> appName + " — Reset your password";
            case TWO_FACTOR_AUTH -> appName + " — Your login verification code";
        };
    }

    private String buildEmailHtml(String firstName, String code, OtpType type) {
        String actionText = switch (type) {
            case EMAIL_VERIFY -> "verify your email address and activate your account";
            case PASSWORD_RESET -> "reset your password";
            case TWO_FACTOR_AUTH -> "complete your login";
        };

        String digitBoxes = buildDigitBoxes(code);

        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin:0;padding:0;background:#f3f4f6;font-family:Arial,sans-serif">
              <table width="100%%" cellpadding="0" cellspacing="0">
                <tr>
                  <td align="center" style="padding:40px 20px">
                    <table width="480" cellpadding="0" cellspacing="0"
                           style="background:#ffffff;border-radius:12px;overflow:hidden">

                      <tr>
                        <td style="background:#4F46E5;padding:28px 32px">
                          <h1 style="color:#ffffff;margin:0;font-size:22px;font-weight:600">
                            %s
                          </h1>
                          <p style="color:#c7d2fe;margin:4px 0 0;font-size:14px">
                            Inventory Management System
                          </p>
                        </td>
                      </tr>

                      <tr>
                        <td style="padding:32px">
                          <p style="color:#374151;font-size:16px;margin:0 0 8px">
                            Hi <strong>%s</strong>,
                          </p>
                          <p style="color:#6b7280;font-size:14px;line-height:1.6;margin:0 0 28px">
                            Use the code below to %s. This code expires in
                            <strong>%d minutes</strong>.
                          </p>

                          <div style="text-align:center;margin:0 0 28px">
                            %s
                          </div>

                          <div style="background:#fef3c7;border-radius:8px;
                                      padding:14px 16px;margin:0 0 24px">
                            <p style="color:#92400e;font-size:13px;margin:0;line-height:1.5">
                              <strong>Security notice:</strong> Never share this code
                              with anyone. %s team will never ask for your OTP.
                            </p>
                          </div>

                          <p style="color:#9ca3af;font-size:12px;margin:0">
                            If you did not request this, please ignore this email.
                            Your account is safe.
                          </p>
                        </td>
                      </tr>

                      <tr>
                        <td style="background:#f9fafb;padding:20px 32px;
                                   border-top:1px solid #e5e7eb">
                          <p style="color:#9ca3af;font-size:12px;margin:0;text-align:center">
                            © 2026 %s. All rights reserved.
                          </p>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(
                appName,
                firstName,
                actionText,
                expiryMinutes,
                digitBoxes,
                appName,
                appName
        );
    }

    private String buildDigitBoxes(String code) {
        StringBuilder boxes = new StringBuilder();
        for (char digit : code.toCharArray()) {
            boxes.append(String.format(
                    "<span style=\"display:inline-block;width:44px;height:52px;" +
                            "background:#f3f4f6;border:2px solid #e5e7eb;border-radius:8px;" +
                            "text-align:center;line-height:52px;font-size:28px;font-weight:700;" +
                            "color:#4F46E5;margin:0 4px\">%c</span>", digit));
        }
        return boxes.toString();
    }
}