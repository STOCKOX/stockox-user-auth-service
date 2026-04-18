package com.stockox.controller;

import com.stockox.dto.request.ChangePasswordRequest;
import com.stockox.dto.request.InviteUserRequest;
import com.stockox.dto.request.UpdateProfileRequest;
import com.stockox.dto.response.ApiResponse;
import com.stockox.dto.response.UserResponse;
import com.stockox.enums.UserRole;
import com.stockox.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Slf4j
@RequestMapping("api/v1/users")
@RequiredArgsConstructor

public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        UserResponse response = userService.getMyProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Profile fetched.", response));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {

        UserResponse response = userService.updateMyProfile(
                userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated.", response));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {

        userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(
                "Password changed. Please login again on other devices."));
    }

    @GetMapping("/admin/all")

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<UserResponse> users = userService.getAllUsersInCompany(
                userDetails.getUsername());
        return ResponseEntity.ok(
                ApiResponse.success("Users fetched. Total: " + users.size(), users));
    }


    @PostMapping("/admin/invite")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> inviteUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody InviteUserRequest request) {

        UserResponse response = userService.inviteUser(
                userDetails.getUsername(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Invite sent to " + request.getEmail() + ". They will receive an OTP email.",
                        response));
    }

    @PutMapping("/admin/{userId}/role")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> changeRole(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID userId,
            @RequestParam UserRole role) {

        UserResponse response = userService.changeUserRole(
                userDetails.getUsername(), userId, role);
        return ResponseEntity.ok(ApiResponse.success(
                "Role updated to " + role.name() + ".", response));
    }

    @PutMapping("/admin/{userId}/suspend")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> suspendUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID userId) {

        userService.suspendUser(userDetails.getUsername(), userId);
        return ResponseEntity.ok(ApiResponse.success(
                "User has been suspended and logged out from all devices."));
    }

    @PutMapping("/admin/{userId}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activateUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID userId) {

        userService.activateUser(userDetails.getUsername(), userId);
        return ResponseEntity.ok(ApiResponse.success("User has been reactivated."));
    }

    @DeleteMapping("/admin/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID userId) {

        userService.deleteUser(userDetails.getUsername(), userId);
        return ResponseEntity.ok(ApiResponse.success("User has been removed."));
    }

}
