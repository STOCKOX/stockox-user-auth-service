package com.stockox.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class TenantResponse {
    private UUID id;
    private String companyName;
    private String email;
    private String phone;
    private String status;
    private LocalDateTime createdAt;
}
