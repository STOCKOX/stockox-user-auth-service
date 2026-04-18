package com.stockox.service.impl;


import com.stockox.dto.request.*;
import com.stockox.dto.response.LoginResponse;
import com.stockox.dto.response.TenantResponse;
import com.stockox.entity.RefreshToken;
import com.stockox.entity.Role;
import com.stockox.entity.Tenant;
import com.stockox.entity.User;
import com.stockox.enums.OtpType;
import com.stockox.enums.TenantStatus;
import com.stockox.enums.UserRole;
import com.stockox.enums.UserStatus;
import com.stockox.exception.BadRequestException;
import com.stockox.exception.ResourceNotFoundException;
import com.stockox.exception.UnauthorizedException;
import com.stockox.repository.RefreshTokenRepository;
import com.stockox.repository.RoleRepository;
import com.stockox.repository.TenantRepository;
import com.stockox.repository.UserRepository;
import com.stockox.security.JwtUtil;
import com.stockox.service.AuthService;
import com.stockox.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor

public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;



    @Value("${app.jwt.refresh-expiry-ms}")
    private long refreshExpiryMs;


    @Override
    @Transactional

    public TenantResponse registerTenant(TenantRegisterRequest req) {
       if(req.getCompanyEmail() == null && req.getCompanyPhone() == null) {
           throw new BadRequestException(
                   "Please provide on company email or phone number. "
           );
       }

       if(req.getCompanyEmail() != null && tenantRepository.existsByEmail(req.getCompanyEmail())) {
           throw  new BadRequestException("A company with this email is already registered.");
       }

        if (userRepository.existsByEmail(req.getAdminEmail())) {
            throw new BadRequestException(
                    "This email is already registered. Please use a different email.");
        }

        // Get ADMIN role(will seed by DataSeeder on startup
        Role adminRole = roleRepository.findByName(UserRole.ADMIN)
                .orElseThrow(() -> new RuntimeException(
                        "ADMIN role not found. Please ensure DataSeeder ran on startup"
                ));

        Tenant tenant = Tenant.builder()
                .companyName(req.getCompanyName())
                .email(req.getCompanyEmail())
                .phone(req.getCompanyPhone())
                .status(TenantStatus.ACTIVE)
                .build();

        tenant = tenantRepository.save(tenant);
        log.info("Tenant created: {} with id: {}", tenant.getCompanyName(), tenant.getId());

        User admin = User.builder()
                .tenant(tenant)
                .role(adminRole)
                .email(req.getAdminEmail())
                .password(passwordEncoder.encode(req.getAdminPassword()))
                .firstName(req.getAdminFirstName())
                .lastName(req.getAdminLastName())
                .phone(req.getAdminPhone())
                .emailVerified(false)         // must verify email before login
                .status(UserStatus.INACTIVE)  // becomes ACTIVE after OTP verify
                .build();


        admin = userRepository.save(admin);
        log.info("Admin user created: {}", admin.getEmail());

        // Send OTP to admin's email for verification
        otpService.generateAndSend(admin, OtpType.EMAIL_VERIFY);

        return TenantResponse.builder()
                .id(tenant.getId())
                .companyName(tenant.getCompanyName())
                .email(tenant.getEmail())
                .phone(tenant.getPhone())
                .status(tenant.getStatus().name())
                .createdAt(tenant.getCreatedAt())
                .build();

    }

    // OTP verification
    @Override
    @Transactional

    public void verifyOtp(VerifyOtpRequest req) {
        User user = findActiveOrInactiveUser(req.getEmail());

        otpService.verify(user, req.getCode(), req.getType());

        if(req.getType() == OtpType.EMAIL_VERIFY) {
            if(user.isEmailVerified()) {
                throw new BadRequestException("Email is already verified. Please login.");
            }

            user.setEmailVerified(true);
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
            log.info("Email verified and account activated for: {}", user.getEmail());
        }
    }

    @Override
    @Transactional

    public void resendOtp(ResendOtpRequest req) {
        User user = findActiveOrInactiveUser(req.getEmail());

        if(req.getType() == OtpType.EMAIL_VERIFY && user.isEmailVerified()) {
            throw new BadRequestException("Email is already verified. Please login.");
        }

        otpService.generateAndSend(user, req.getType());
        log.info("OTP resent to: {} for type: {}", user.getEmail(), req.getType());
    }

    @Override
    @Transactional

    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByEmailAndDeletedFalse(req.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // check password
        if(!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");

        }
        if(!user.isEmailVerified()) {
            throw new UnauthorizedException(
                    "Please verify your email first. Check your inbox for the OTP."
            );
        }

        // check account status
        switch (user.getStatus()) {
            case SUSPENDED -> throw new UnauthorizedException(
                    "Your account has been suspended. Contact your admin."
            );
            case DELETED ->
                    throw new UnauthorizedException("Account not found.");
            case INACTIVE ->
                    throw new UnauthorizedException(
                            "Account is not active. Please verify your email.");
            default -> { /* ACTIVE — proceed */ }
        }

        if (user.getTenant().getStatus() != TenantStatus.ACTIVE) {
            throw new UnauthorizedException(
                    "Your company workspace is suspended. Contact support.");
        }


        refreshTokenRepository.revokeAllByUserId(user.getId());

        // Generate new tokens
        String accessToken  = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // Save refresh token in DB
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiryMs / 1000))
                .build());

        log.info("Login successful for: {}", user.getEmail());

        return buildLoginResponse(user, accessToken, refreshToken);
    }

    @Override
    @Transactional

    public LoginResponse refreshToken(RefreshTokenRequest req) {
        RefreshToken stored = refreshTokenRepository
                .findByTokenAndRevokedFalse(req.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException(
                        "Invalid or expired refresh token. Please login again."
                ));

        if (!stored.isValid()) {
            throw new UnauthorizedException(
                    "Refresh token has expired. Please login again.");
        }

        if (!jwtUtil.isValid(req.getRefreshToken())) {
            throw new UnauthorizedException("Invalid refresh token.");
        }

        User user = stored.getUser();

        refreshTokenRepository.revokeAllByUserId(user.getId());

        // Issue new tokens
        String newAccessToken  = jwtUtil.generateAccessToken(user);
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(newRefreshToken)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiryMs / 1000))
                .build());

        log.info("Token refreshed for user: {}", user.getEmail());

        return buildLoginResponse(user, newAccessToken, newRefreshToken);
    }

    @Override
    @Transactional

    public void forgotPassword(ForgotPasswordRequest req) {
        userRepository.findByEmailAndDeletedFalse(req.getEmail())
                .ifPresent(user -> {
                    otpService.generateAndSend(user, OtpType.PASSWORD_RESET);
                    log.info("Password reset OTP sent to: {}", req.getEmail());
                });
    }

    @Override
    public void resetPassword(ResetPasswordRequest req) {
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match.");
        }
        User user = findActiveOrInactiveUser(req.getEmail());
        otpService.verify(user, req.getCode(), OtpType.PASSWORD_RESET);


        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);

        // Revoke ALL refresh tokens — forces re-login on all devices
        refreshTokenRepository.revokeAllByUserId(user.getId());

        log.info("Password reset successfully for: {}", user.getEmail());

    }

    @Override
    @Transactional

    public void logout(String authorizationHeader) {
        String token = jwtUtil.extractFromHeader(authorizationHeader);
        if (token != null && jwtUtil.isValid(token)) {
            String userId = jwtUtil.extractUserId(token);
            refreshTokenRepository.revokeAllByUserId(UUID.fromString(userId));
            log.info("User logged out: {}", userId);
        }
        // If token is invalid, just silently succeed
    }


    // PRIVATE HELPERS
    private User findActiveOrInactiveUser(String email) {
        return userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No account found with email: " + email
                ));
    }

    private LoginResponse buildLoginResponse(User user, String accessToken,
                                             String refreshToken) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .accessExpiresIn(900) // 15 minutes in seconds
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().getName().name())
                .tenantId(user.getTenant().getId())
                .companyName(user.getTenant().getCompanyName())
                .build();
    }

}
