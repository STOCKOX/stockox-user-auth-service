package com.stockox.dto.request;

import com.stockox.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data

public class InviteUserRequest {
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    // Role to assign: MANAGER or STAFF (Admin cannot invite another ADMIN)
    @NotNull(message = "Role is required")
    private UserRole role;

    @Size(max = 20)
    private String phone;
}
