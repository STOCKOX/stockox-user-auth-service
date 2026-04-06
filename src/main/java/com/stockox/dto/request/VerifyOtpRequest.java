package com.stockox.dto.request;


import com.stockox.enums.OtpType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyOtpRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email")
    private String email;

    @NotBlank(message = "OTP code is required")
    @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits")
    private String code;

    @NotNull(message = "OTP type is required (EMAIL_VERIFY or PASSWORD_RESET)")
    private OtpType type;
}
