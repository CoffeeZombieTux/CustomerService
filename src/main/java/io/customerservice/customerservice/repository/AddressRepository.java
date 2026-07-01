package io.customerservice.customerservice.repository;

import io.customerservice.customerservice.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findAllByCustomer_Id(Long customerId);
    Optional<Address> findByIdAndCustomer_Id(Long id, Long customerId);
}