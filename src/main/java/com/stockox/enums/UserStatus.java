package com.stockox.enums;

public enum UserStatus {
    INACTIVE,        // just registered, email not verified
    ACTIVE,          // email verified, can login
    SUSPENDED,       // blocked by admin
    DELETED          // Soft delete
}
