package com.stockox.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class UserResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String role;
    private String status;
    private boolean emailVerified;

    // Company info
    private UUID tenantId;
    private String companyName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
