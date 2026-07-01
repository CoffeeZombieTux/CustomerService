package io.customerservice.customerservice.dto.mapper;

import io.customerservice.customerservice.dto.request.UpdateProfileRequest;
import io.customerservice.customerservice.dto.response.CustomerResponse;
import io.customerservice.customerservice.entity.Customer;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CustomerMapper {
    @Mapping(source = "audit.createdAt", target = "createdAt")
    @Mapping(source = "audit.updatedAt", target = "updatedAt")
    CustomerResponse toResponse(Customer customer);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, ignoreByDefault = true)
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "phone", source = "phone")
    void updateCustomer(UpdateProfileRequest request, @MappingTarget Customer customer);
}