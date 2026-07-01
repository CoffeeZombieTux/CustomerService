package io.customerservice.customerservice.service;

import io.customerservice.customerservice.dto.mapper.AddressMapper;
import io.customerservice.customerservice.dto.request.AddressRequest;
import io.customerservice.customerservice.dto.response.AddressResponse;
import io.customerservice.customerservice.entity.Address;
import io.customerservice.customerservice.entity.Customer;
import io.customerservice.customerservice.exception.ResourceNotFoundException;
import io.customerservice.customerservice.repository.AddressRepository;
import io.customerservice.customerservice.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {
    private final AddressRepository addressRepository;
    private final CustomerRepository customerRepository;
    private final AddressMapper addressMapper;

    public List<AddressResponse> getAll(Long customerId) {
        return addressRepository.findAllByCustomer_Id(customerId)
                .stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    @Transactional
    public AddressResponse create(AddressRequest request, Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(Customer.RESOURCE));

        Address address = new Address();
        address.setCustomer(customer);
        address.setType(request.type());
        address.setStreet(request.street());
        address.setCity(request.city());
        address.setPostalCode(request.postalCode());
        address.setCountry(request.country());
        addressRepository.save(address);
        return addressMapper.toResponse(address);
    }

    @Transactional
    public AddressResponse update(Long addressId, Long customerId, AddressRequest request) {
        Address address = addressRepository.findByIdAndCustomer_Id(addressId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException(Address.RESOURCE));

        address.setType(request.type());
        address.setStreet(request.street());
        address.setCity(request.city());
        address.setPostalCode(request.postalCode());
        address.setCountry(request.country());
        addressRepository.save(address);
        return addressMapper.toResponse(address);
    }

    @Transactional
    public void delete(Long addressId, Long customerId) {
        Address address = addressRepository.findByIdAndCustomer_Id(addressId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException(Address.RESOURCE));
        addressRepository.delete(address);
    }
}
