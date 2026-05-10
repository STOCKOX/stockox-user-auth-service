package com.stockox.service.impl;

import com.stockox.enums.OtpType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Separate bean for async email sending.
 *
 * WHY THIS EXISTS:
 * Spring @Async works via AOP proxy. If sendOtpEmailAsync() lived inside
 * OtpServiceImpl and was called from generateAndSend() in the SAME class,
 * Spring's proxy would be bypassed and the method would run SYNCHRONOUSLY
 * (a well-known Spring self-invocation limitation).
 *
 * By moving the async method to its own bean (EmailService), OtpServiceImpl
 * calls it through the proxy → @Async works correctly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.app-name}")
    private String appName;

    @Value("${app.otp.expiry-minutes}")
    private int expiryMinutes;

    @Async("emailTaskExecutor")
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
            log.error("Unexpected error sending OTP email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    private String getEmailSubject(OtpType type) {
        return switch (type) {
            case EMAIL_VERIFY    -> appName + " — Verify your email address";
            case PASSWORD_RESET  -> appName + " — Reset your password";
            case TWO_FACTOR_AUTH -> appName + " — Your login verification code";
        };
    }

    private String buildEmailHtml(String firstName, String code, OtpType type) {
        String actionText = switch (type) {
            case EMAIL_VERIFY    -> "verify your email address and activate your account";
            case PASSWORD_RESET  -> "reset your password";
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