package com.stockox.mapper;

import com.stockox.dto.response.UserResponse;
import com.stockox.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    @Mapping(target = "role", expression = "java(user.getRole().getName().name())")
    @Mapping(target = "status", expression = "java(user.getStatus().name())")
    @Mapping(target = "tenantId", source = "tenant.id")
    @Mapping(target = "companyName", source = "tenant.companyName")
    UserResponse toUserResponse(User user);
}