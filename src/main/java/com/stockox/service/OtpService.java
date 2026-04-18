package com.stockox.service;

import com.stockox.entity.User;
import com.stockox.enums.OtpType;

public interface OtpService {
    void generateAndSend(User user, OtpType type);
    void verify(User user, String code, OtpType type);
}
