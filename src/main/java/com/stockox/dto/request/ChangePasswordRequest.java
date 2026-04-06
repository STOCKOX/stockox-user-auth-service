package com.stockox.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for PUT /api/v1/users/me/change-password
 *
 * Logged-in user changes their password while already authenticated.
 * They must know their current password to set a new one.
 */

@Data

public class ChangePasswordRequest {
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;

    @NotBlank(message = "Please confirm your new password")
    private String confirmPassword;
}
