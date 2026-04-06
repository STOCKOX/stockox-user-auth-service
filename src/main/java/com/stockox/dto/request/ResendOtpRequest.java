package com.stockox.dto.request;


import com.stockox.enums.OtpType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResendOtpRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email a valid email")
    private String email;

    @NotNull(message = "OTP type is required")
    private OtpType type;

}
