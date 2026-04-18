package com.stockox.service.impl;


import com.stockox.dto.request.ChangePasswordRequest;
import com.stockox.dto.request.InviteUserRequest;
import com.stockox.dto.request.UpdateProfileRequest;
import com.stockox.dto.response.UserResponse;
import com.stockox.entity.Role;
import com.stockox.entity.Tenant;
import com.stockox.entity.User;
import com.stockox.enums.OtpType;
import com.stockox.enums.UserRole;
import com.stockox.enums.UserStatus;
import com.stockox.exception.BadRequestException;
import com.stockox.exception.ResourceNotFoundException;
import com.stockox.repository.RefreshTokenRepository;
import com.stockox.repository.RoleRepository;
import com.stockox.repository.UserRepository;
import com.stockox.service.OtpService;
import com.stockox.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor

public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    @Override
    @Transactional

    public UserResponse getMyProfile(String userId) {
        User user = findById(userId);
        return toUserResponse(user);
    }


    @Override
    @Transactional

    public UserResponse updateMyProfile(String userId, UpdateProfileRequest req) {
        User user = findById(userId);

        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setPhone(req.getPhone());

        User savedUser = userRepository.save(user);
        log.info("Profile updated for user: {}", user.getEmail());
        return toUserResponse(savedUser);

    }

    @Override
    public void changePassword(String userId, ChangePasswordRequest req) {
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new BadRequestException("New passwords do not match.");
        }
        User user = findById(userId);
        if(!passwordEncoder.matches(req.getConfirmPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect.");
        }

        if (passwordEncoder.matches(req.getNewPassword(), user.getPassword())) {
            throw new BadRequestException(
                    "New password must be different from your current password.");
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(user.getId());

        log.info("Password changed for user: {}", user.getEmail());
    }

    // ADMIN TEAM MANAGEMENT

    @Override
    @Transactional

    public List<UserResponse> getAllUsersInCompany(String adminUserId) {
        User admin = findById(adminUserId);
        Tenant tenant = admin.getTenant();

        return userRepository.findAllByTenantAndDeletedFalseOrderByCreatedAtDesc(tenant)
                .stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }
    @Override
    @Transactional
    public UserResponse inviteUser(String adminUserId, InviteUserRequest req) {
        User admin = findById(adminUserId);


        if (req.getRole() == UserRole.ADMIN || req.getRole() == UserRole.SUPER_ADMIN) {
            throw new BadRequestException(
                    "You cannot invite a user with ADMIN role. Contact platform support.");
        }


        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestException(
                    "A user with email '" + req.getEmail() + "' is already registered.");
        }

        Role role = roleRepository.findByName(req.getRole())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Role not found: " + req.getRole()));


        String tempPassword = generateTempPassword();

        User invitedUser = User.builder()
                .tenant(admin.getTenant())
                .role(role)
                .email(req.getEmail())
                .password(passwordEncoder.encode(tempPassword))
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .phone(req.getPhone())
                .emailVerified(false)
                .status(UserStatus.INACTIVE)
                .build();

        invitedUser = userRepository.save(invitedUser);

        otpService.generateAndSend(invitedUser, OtpType.EMAIL_VERIFY);

        log.info("User invited: {} by admin: {}", req.getEmail(), admin.getEmail());

        return toUserResponse(invitedUser);
    }


    @Override
    @Transactional
    public UserResponse changeUserRole(String adminUserId, UUID targetUserId, UserRole newRole) {
        User admin   = findById(adminUserId);
        User target  = findUserInSameTenant(targetUserId, admin.getTenant());

        if (admin.getId().equals(targetUserId)) {
            throw new BadRequestException("You cannot change your own role.");
        }

        if (newRole == UserRole.ADMIN || newRole == UserRole.SUPER_ADMIN) {
            throw new BadRequestException("Cannot assign ADMIN role here.");
        }

        Role role = roleRepository.findByName(newRole)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + newRole));

        target.setRole(role);
        User saved = userRepository.save(target);

        log.info("Role changed for user: {} to: {} by admin: {}",
                target.getEmail(), newRole, admin.getEmail());

        return toUserResponse(saved);
    }


    @Override
    @Transactional
    public void suspendUser(String adminUserId, UUID targetUserId) {
        User admin  = findById(adminUserId);
        User target = findUserInSameTenant(targetUserId, admin.getTenant());

        if (admin.getId().equals(targetUserId)) {
            throw new BadRequestException("You cannot suspend your own account.");
        }

        target.setStatus(UserStatus.SUSPENDED);
        userRepository.save(target);

        // Revoke all their tokens of the user and forces immediate logout
        refreshTokenRepository.revokeAllByUserId(targetUserId);
        log.info("User suspended: {} by admin: {}", target.getEmail(), admin.getEmail());
    }

    @Override
    public void activateUser(String adminUserId, UUID targetUserId) {
        User admin  = findById(adminUserId);
        User target = findUserInSameTenant(targetUserId, admin.getTenant());

        if (target.getStatus() != UserStatus.SUSPENDED) {
            throw new BadRequestException("User is not suspended.");
        }

        target.setStatus(UserStatus.ACTIVE);
        userRepository.save(target);

        log.info("User activated: {} by admin: {}", target.getEmail(), admin.getEmail());

    }

    @Override
    public void deleteUser(String adminUserId, UUID targetUserId) {
        User admin  = findById(adminUserId);
        User target = findUserInSameTenant(targetUserId, admin.getTenant());

        if (admin.getId().equals(targetUserId)) {
            throw new BadRequestException("You cannot delete your own account.");
        }

        target.setDeleted(true);
        target.setStatus(UserStatus.DELETED);
        userRepository.save(target);

        refreshTokenRepository.revokeAllByUserId(targetUserId);

        log.info("User soft-deleted: {} by admin: {}", target.getEmail(), admin.getEmail());
    }

    // -------------Private Helpers--------------------

    private User findUserInSameTenant(UUID targetUserId, Tenant adminTenant) {
        return userRepository.findByIdAndTenant(targetUserId, adminTenant)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found in your company."));
    }


    private String generateTempPassword() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }


    private User findById(String userId) {
        return userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No user found"
                ));
    }


    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .role(user.getRole().getName().name())
                .status(user.getStatus().name())
                .emailVerified(user.isEmailVerified())
                .tenantId(user.getTenant().getId())
                .companyName(user.getTenant().getCompanyName())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
