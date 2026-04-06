package com.stockox.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class LoginResponse {

    private String accessToken;
    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";

    @Builder.Default
    private long accessExpiresIn = 900;

    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String role;

    // Tenant info — frontend uses this to show company name
    private UUID tenantId;
    private String companyName;
}
