package com.stockox.entity;

import com.stockox.enums.OtpType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor

@Table(
        name = "otp_tokens",
        indexes = {
                @Index(name = "idx_otp_user_type", columnList = "user_id, type, used")
        }
)
public class OtpToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private OtpType type;

    @Column(name = "used", nullable = false)
    @Builder.Default
    private boolean used = false;

    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;


    @Transient
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /** Checks if too many wrong attempts */
    @Transient
    public boolean isLocked(int maxAttempts) {
        return attempts >= maxAttempts;
    }

}
