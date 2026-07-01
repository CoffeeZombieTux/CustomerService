package io.customerservice.customerservice.service;

import io.customerservice.customerservice.config.AppProperties;
import io.customerservice.customerservice.dto.request.AgreementRequest;
import io.customerservice.customerservice.dto.response.AgreementResponse;
import io.customerservice.customerservice.dto.mapper.AgreementMapper;
import io.customerservice.customerservice.entity.Customer;
import io.customerservice.customerservice.entity.CustomerAgreement;
import io.customerservice.customerservice.exception.DuplicateAgreementException;
import io.customerservice.customerservice.exception.MandatoryAgreementException;
import io.customerservice.customerservice.exception.ResourceNotFoundException;
import io.customerservice.customerservice.repository.CustomerAgreementRepository;
import io.customerservice.customerservice.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgreementService {
    private final CustomerAgreementRepository customerAgreementRepository;
    private final CustomerRepository customerRepository;
    private final AgreementMapper agreementMapper;
    private final AppProperties.Agreements agreementProps;

    public List<AgreementResponse> getAgreements(Long customerId) {
        return customerAgreementRepository.findByCustomerId(customerId).stream().map(agreementMapper::toResponse).toList();
    }

    public List<AgreementResponse> getActiveAgreements(Long customerId) {
        return customerAgreementRepository.findActiveByCustomerId(customerId)
                .stream()
                .map(agreementMapper::toResponse)
                .toList();
    }

    @Transactional
    public List<AgreementResponse> updateAgreements(Long customerId, AgreementRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(Customer.RESOURCE));

        if (!request.accepted() && agreementProps.mandatory().contains(request.type())) {
            throw new MandatoryAgreementException();
        }

        customerAgreementRepository
                .findTopByCustomerIdAndAgreementTypeOrderByAudit_CreatedAtDesc(customerId, request.type())
                .ifPresent(latest -> {
                    if (request.version().equals(latest.getVersion()) && latest.isAccepted() == request.accepted()) {
                        throw new DuplicateAgreementException();
                    }
                });
        CustomerAgreement customerAgreement = new CustomerAgreement();
        customerAgreement.setCustomer(customer);
        customerAgreement.setAgreementType(request.type());
        customerAgreement.setVersion(request.version());
        customerAgreement.setAccepted(request.accepted());
        customerAgreementRepository.save(customerAgreement);

        return customerAgreementRepository.findByCustomerId(customerId)
                .stream()
                .map(agreementMapper::toResponse)
                .toList();
    }
}
