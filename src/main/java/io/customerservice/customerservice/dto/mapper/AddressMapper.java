package io.customerservice.customerservice.dto.mapper;

import io.customerservice.customerservice.dto.response.AddressResponse;
import io.customerservice.customerservice.entity.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AddressMapper {
    @Mapping(source = "audit.createdAt", target = "createdAt")
    @Mapping(source = "audit.updatedAt", target = "updatedAt")
    AddressResponse toResponse(Address address);
}