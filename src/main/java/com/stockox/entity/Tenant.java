package com.stockox.entity;


import com.stockox.enums.TenantStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "email", unique = true, nullable = true)
    private String email;

    @Column(name = "phone", nullable = true, length = 20)
    private String phone;

    @Column(name = "address", nullable = true)
    private String address;

    @Column(name = "logo_url", nullable = true)
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TenantStatus status = TenantStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
