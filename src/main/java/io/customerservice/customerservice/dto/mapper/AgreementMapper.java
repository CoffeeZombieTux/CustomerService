package io.customerservice.customerservice.dto.mapper;

import io.customerservice.customerservice.dto.response.AgreementResponse;
import io.customerservice.customerservice.entity.CustomerAgreement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AgreementMapper {
    @Mapping(source = "agreementType", target = "type")
    @Mapping(source = "audit.createdAt", target = "createdAt")
    @Mapping(source = "audit.updatedAt", target = "updatedAt")
    AgreementResponse toResponse(CustomerAgreement agreement);
}