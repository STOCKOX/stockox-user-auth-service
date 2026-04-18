package com.stockox.service;

import com.stockox.dto.request.*;
import com.stockox.dto.response.LoginResponse;
import com.stockox.dto.response.TenantResponse;

public interface AuthService {

    TenantResponse registerTenant(TenantRegisterRequest request);

    void verifyOtp(VerifyOtpRequest request);

    void resendOtp(ResendOtpRequest request);

    LoginResponse login(LoginRequest request);

    LoginResponse refreshToken(RefreshTokenRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void logout(String authorizationHeader);

}
