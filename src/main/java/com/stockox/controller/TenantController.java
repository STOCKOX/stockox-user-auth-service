package com.stockox.controller;

import com.stockox.dto.request.TenantRegisterRequest;
import com.stockox.dto.response.ApiResponse;
import com.stockox.dto.response.TenantResponse;
import com.stockox.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/tenants")
public class TenantController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<TenantResponse>> registerTenant(
            @Valid
            @RequestBody TenantRegisterRequest request
            ) {
        TenantResponse response  =authService.registerTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        "Company Workspace created! Check your email (" + request.getAdminEmail() + ") for the OTP to verify your account.",
                        response));

    }
}
