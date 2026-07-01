package io.customerservice.customerservice.dto.mapper;

import io.customerservice.customerservice.dto.response.SessionResponse;
import io.customerservice.customerservice.entity.RefreshToken;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SessionMapper {
    @Mapping(source = "expiredAt", target = "expiresAt")
    @Mapping(source = "audit.createdAt", target = "createdAt")
    @Mapping(source = "audit.updatedAt", target = "updatedAt")
    SessionResponse toResponse(RefreshToken token);
}