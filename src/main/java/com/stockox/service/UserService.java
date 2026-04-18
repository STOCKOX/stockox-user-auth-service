package com.stockox.service;

import com.stockox.dto.request.ChangePasswordRequest;
import com.stockox.dto.request.InviteUserRequest;
import com.stockox.dto.request.UpdateProfileRequest;
import com.stockox.dto.response.UserResponse;
import com.stockox.enums.UserRole;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserResponse getMyProfile(String userId);
    UserResponse updateMyProfile(String userId, UpdateProfileRequest request);
    void changePassword(String userId, ChangePasswordRequest request);
    List<UserResponse> getAllUsersInCompany(String adminUserId);
    UserResponse inviteUser(String adminUserId, InviteUserRequest request);
    UserResponse changeUserRole(String adminUserId, UUID targetUserId, UserRole newRole);
    void suspendUser(String adminUserId, UUID targetUserId);
    void activateUser(String adminUserId, UUID targetUserId);
    void deleteUser(String adminUserId, UUID targetUserId);


}
