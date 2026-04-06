package com.stockox.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TenantRegisterRequest {

    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 100, message = "Company name must be 2-100 characters")
    private String companyName;

    @Email(message = "Enter a valid company email")
    private String companyEmail;

    @Size(max = 20, message = "Phone number too long")
    private String companyPhone;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50)
    private String adminFirstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50)
    private String adminLastName;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Enter a valid admin email")
    private String adminEmail;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String adminPassword;

    @Size(max = 20)
    private String adminPhone;

}